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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailBackgroundExecScope
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

    private fun worker() = ContentIndexingWorker(
        context = mockk(relaxed = true),
        workerParameters = mockk<WorkerParameters>(relaxed = true),
        mailSessionRepository = mailSessionRepository,
        userSessionRepository = userSessionRepository,
        observeContentIndexingActivity = observeContentIndexingActivity
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

    private fun givenActivity(flow: Flow<ContentIndexingActivity>) {
        every { observeContentIndexingActivity() } returns flow
    }

    private fun progress(
        activeUserId: UserId? = UserId("user-1"),
        completedUsers: Long = 0,
        userCount: Long = 2
    ) = ContentIndexingActivity.Progress(activeUserId, completedUsers, userCount)
}
