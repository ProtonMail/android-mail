/*
 * Copyright (c) 2025 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailcontentsearch.data.worker

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailBackgroundExecScope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentIndexingWorkerTest {

    private val execScope = mockk<MailBackgroundExecScope> { justRun { finsihed() } }
    private val mailSession = mockk<MailSessionWrapper> {
        every { newBackgroundExecutionScope() } returns execScope
    }
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { getMailSession() } returns mailSession
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getAccount(any()) } returns null
    }
    private val observeContentIndexingActivity = mockk<ObserveContentIndexingActivity>()
    private val appInBackground = MutableStateFlow(false)
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns appInBackground
    }
    private val workScheduler = mockk<ContentIndexingWorkScheduler>(relaxUnitFun = true)

    // Whether the platform grants the foreground service. Left to a relaxed mock the promotion always
    // fails - there is no real notification to build outside Robolectric - and the worker would then
    // never consider itself promoted, which is the distinction most of what follows turns on.
    private var isPromotionGranted = true

    @BeforeTest
    fun setUp() {
        mockkObject(ContentIndexingNotification)
        every { ContentIndexingNotification.build(any(), any(), any()) } returns mockk {
            every { build() } returns mockk(relaxed = true)
        }
    }

    @AfterTest
    fun tearDown() = unmockkObject(ContentIndexingNotification)

    private fun workerParameters() = mockk<WorkerParameters>(relaxed = true) {
        every { foregroundUpdater } returns mockk {
            every { setForegroundAsync(any(), any(), any()) } answers {
                mockk {
                    every { isDone } returns true
                    every { get() } answers {
                        if (isPromotionGranted) null else throw IllegalStateException("promotion refused")
                    }
                    every { addListener(any(), any()) } answers { firstArg<Runnable>().run() }
                }
            }
        }
    }

    private fun worker() = ContentIndexingWorker(
        context = mockk(relaxed = true),
        workerParameters = workerParameters(),
        mailSessionRepository = mailSessionRepository,
        userSessionRepository = userSessionRepository,
        observeContentIndexingActivity = observeContentIndexingActivity,
        appInBackgroundState = appInBackgroundState,
        workScheduler = workScheduler
    )

    @Test
    fun `finishes once the orchestrator enters forward mode`() = runTest {
        // Given
        givenActivity(
            flowOf(
                progress(),
                ContentIndexingActivity.ForwardMode(UserId("user-1")),
                progress()
            )
        )

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `finishes when the orchestrator reports it is waiting on accounts`() = runTest {
        // Given - nothing eligible right now, so there is nothing to hold the service open for.
        givenActivity(flowOf(ContentIndexingActivity.WaitingOnUsers))

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `finishes when the orchestrator stops`() = runTest {
        // Given
        givenActivity(flowOf(progress(), ContentIndexingActivity.Stopped))

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `finishes when the orchestrator fails`() = runTest {
        // Given
        givenActivity(flowOf(ContentIndexingActivity.Failed("boom")))

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `the watchdog releases the service when the orchestrator never reports anything`() = runTest {
        // Given - an orchestrator that accepted start() but publishes nothing would otherwise hold
        // a foreground service open indefinitely.
        givenActivity(flow { awaitCancellation() })

        // When
        val work = async { worker().doWork() }
        advanceTimeBy(ContentIndexingWorker.IdleTimeout.inWholeMilliseconds + 1)

        // Then
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `the watchdog measures the gap between emissions, not the total run`() = runTest {
        // Given - steady progress well past the idle timeout in total.
        givenActivity(
            flow {
                repeat(4) {
                    delay(ContentIndexingWorker.IdleTimeout.inWholeMilliseconds / 2)
                    emit(progress())
                }
                emit(ContentIndexingActivity.Stopped)
            }
        )

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `does not show a notification while the app is on screen`() = runTest {
        // Given - the worker is enqueued from the foreground, but the service is only worth
        // anything once the app is off screen.
        givenActivity(flowOf(progress(), progress(), ContentIndexingActivity.Stopped))

        // When
        worker().doWork()

        // Then - the account label is only ever read to build the notification.
        coVerify(exactly = 0) { userSessionRepository.getAccount(any()) }
    }

    @Test
    fun `shows the notification once the app is backgrounded`() = runTest {
        // Given
        appInBackground.value = true
        givenActivity(
            flow {
                emit(progress())
                awaitCancellation()
            }
        )

        // When
        val work = async { worker().doWork() }
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // Then - the account label is read to build the notification.
        coVerify(atLeast = 1) { userSessionRepository.getAccount(UserId("user-1")) }
        work.cancel()
    }

    @Test
    fun `replaces itself when the app comes back on screen`() = runTest {
        // Given - the service was acquired while the app was away.
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // When
        appInBackground.value = false
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // Then - the only way to give the foreground service back is to end this worker.
        verify(exactly = 1) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `does not replace itself when the promotion was refused`() = runTest {
        // Given - the platform turned the foreground service down, so there is none to hand back and
        // replacing this worker would cost a background execution scope for nothing.
        isPromotionGranted = false
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // When
        appInBackground.value = false
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // Then
        verify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `does not replace itself when the app was never off screen`() = runTest {
        // Given
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }

        // When - a foreground app that stays foregrounded holds no service to give back.
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // Then
        verify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `rides out a brief trip to the background without acquiring the service`() = runTest {
        // Given - a permission dialog or a share sheet, not the user leaving.
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }

        // When
        appInBackground.value = true
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds / 2)
        appInBackground.value = false
        advanceTimeBy(ContentIndexingWorker.VisibilityDebounce.inWholeMilliseconds + 1)

        // Then
        coVerify(exactly = 0) { userSessionRepository.getAccount(any()) }
        verify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    private fun givenActivity(flow: Flow<ContentIndexingActivity>) {
        every { observeContentIndexingActivity() } returns flow
    }

    private fun progress(
        activeUserId: UserId? = UserId("user-1"),
        completedUsers: Long = 0,
        userCount: Long = 2
    ) = ContentIndexingActivity.Progress(activeUserId, completedUsers, userCount)
}
