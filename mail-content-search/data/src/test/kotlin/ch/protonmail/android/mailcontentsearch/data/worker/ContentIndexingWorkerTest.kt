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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.system.DeviceArchitectureProvider
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailBackgroundExecScope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

internal class ContentIndexingWorkerTest {

    private val execScope = mockk<MailBackgroundExecScope> { justRun { finsihed() } }
    private val mailSession = mockk<MailSessionWrapper> {
        every { newBackgroundExecutionScope() } returns execScope
    }
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { getMailSession() } returns mailSession
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getAccount(any()) } answers { account(firstArg()) }
    }

    // Each account's own backfill, which is what the notification reports - as opposed to the
    // orchestrator's session-wide progress, which only decides how long the worker lives.
    private val indexingStates = mapOf(
        FirstUser to MutableStateFlow<ContentIndexingState>(ContentIndexingState.Initializing),
        SecondUser to MutableStateFlow<ContentIndexingState>(ContentIndexingState.Initializing)
    )
    private val observeContentSearchIndexingStatus = mockk<ObserveContentSearchIndexingStatus> {
        every { this@mockk.invoke(any()) } answers { indexingStates.getValue(firstArg()) }
    }
    private val observeContentIndexingActivity = mockk<ObserveContentIndexingActivity>()
    private val startContentIndexing = mockk<StartContentIndexing> {
        coEvery { this@mockk.invoke() } returns startSummary(pending = 1).right()
    }
    private val appInBackground = MutableStateFlow(false)
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns appInBackground
        every { isAppInBackground() } answers { appInBackground.value }
    }
    private val workScheduler = mockk<ContentIndexingWorkScheduler>(relaxUnitFun = true)
    private val deviceArchitectureProvider = mockk<DeviceArchitectureProvider> {
        every { is64Bit() } returns true
    }

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

    @Test
    fun `exits straight away on a 32-bit process`() = runTest {
        // Given
        every { deviceArchitectureProvider.is64Bit() } returns false

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { startContentIndexing() }
        verify(exactly = 0) { observeContentIndexingActivity() }
    }

    private fun worker() = ContentIndexingWorker(
        context = mockk(relaxed = true),
        workerParameters = workerParameters(),
        mailSessionRepository = mailSessionRepository,
        userSessionRepository = userSessionRepository,
        observeContentIndexingActivity = observeContentIndexingActivity,
        observeContentSearchIndexingStatus = observeContentSearchIndexingStatus,
        startContentIndexing = startContentIndexing,
        appInBackgroundState = appInBackgroundState,
        workScheduler = workScheduler,
        deviceArchitectureProvider = deviceArchitectureProvider
    )

    @Test
    fun `finishes once the orchestrator enters forward mode with every account done`() = runTest {
        // Given
        givenActivity(
            flowOf(
                progress(completedUsers = 2),
                ContentIndexingActivity.ForwardMode(FirstUser),
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
    fun `stays alive while the orchestrator hands over to the next account`() = runTest {
        // Given
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(completedUsers = 1, userCount = 2))
        runCurrent()

        // When
        activity.emit(ContentIndexingActivity.WaitingOnUsers)
        runCurrent()
        advanceTimeBy((ContentIndexingWorker.HandoverTimeout.inWholeMilliseconds - 1).milliseconds)

        // Then
        assertTrue(work.isActive)
        activity.emit(progress(activeUserId = SecondUser, completedUsers = 1, userCount = 2))
        runCurrent()
        // Back on the long timeout, rather than still counting down the handover.
        advanceTimeBy((ContentIndexingWorker.HandoverTimeout.inWholeMilliseconds + 1).milliseconds)
        assertTrue(work.isActive)
        work.cancel()
    }

    @Test
    fun `finishes when the orchestrator settles with no accounts left`() = runTest {
        // Given
        coEvery { startContentIndexing() } returnsMany listOf(
            startSummary(pending = 1).right(),
            startSummary(completed = 2).right()
        )
        givenActivity(
            flow {
                emit(progress(completedUsers = 2, userCount = 2))
                emit(ContentIndexingActivity.WaitingOnUsers)
                awaitCancellation()
            }
        )

        // When
        val result = worker().doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `gives up on a handover the orchestrator never resolves`() = runTest {
        // Given
        givenActivity(
            flow {
                emit(progress(completedUsers = 1, userCount = 2))
                emit(ContentIndexingActivity.WaitingOnUsers)
                awaitCancellation()
            }
        )

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.HandoverTimeout.inWholeMilliseconds + 1).milliseconds)

        // Then
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `releases without a handover wait when the orchestrator has nothing left`() = runTest {
        // Given
        coEvery { startContentIndexing() } returnsMany listOf(
            startSummary(pending = 1).right(),
            startSummary(completed = 2).right()
        )
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(completedUsers = 1, userCount = 2))
        runCurrent()

        // When
        activity.emit(ContentIndexingActivity.ForwardMode(FirstUser))
        runCurrent()

        // Then
        assertFalse(work.isActive)
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `asks the orchestrator for another account when one settles into forward mode`() = runTest {
        // Given
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(completedUsers = 1, userCount = 2))
        runCurrent()

        // When
        activity.emit(ContentIndexingActivity.ForwardMode(FirstUser))
        runCurrent()

        // Then
        coVerify(exactly = 2) { startContentIndexing() }
        assertTrue(work.isActive)
        work.cancel()
    }

    @Test
    fun `assumes an account is still coming when the orchestrator cannot be asked at a handover`() = runTest {
        // Given
        coEvery { startContentIndexing() } returnsMany listOf(
            startSummary(pending = 1).right(),
            DataError.Local.Unknown.left()
        )
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(completedUsers = 1, userCount = 2))
        runCurrent()

        // When
        activity.emit(ContentIndexingActivity.ForwardMode(FirstUser))
        runCurrent()

        // Then
        assertTrue(work.isActive)
        advanceTimeBy((ContentIndexingWorker.HandoverTimeout.inWholeMilliseconds + 1).milliseconds)
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `keeps the notification it is showing through a handover`() = runTest {
        // Given
        appInBackground.value = true
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser, completedUsers = 1, userCount = 2))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 41.6)
        runCurrent()

        // When
        activity.emit(ContentIndexingActivity.WaitingOnUsers)
        runCurrent()

        // Then
        val percentages = mutableListOf<Double?>()
        verify { ContentIndexingNotification.build(any(), any(), captureNullable(percentages)) }
        assertEquals(41.6, percentages.last())
        work.cancel()
    }

    @Test
    fun `holds the last percentage when the outgoing account reports itself caught up`() = runTest {
        // Given
        appInBackground.value = true
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser, completedUsers = 1, userCount = 2))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 98.0)
        runCurrent()

        // When
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Completed
        activity.emit(ContentIndexingActivity.WaitingOnUsers)
        runCurrent()

        // Then
        val percentages = mutableListOf<Double?>()
        verify { ContentIndexingNotification.build(any(), any(), captureNullable(percentages)) }
        assertEquals(98.0, percentages.last())
        work.cancel()
    }

    @Test
    fun `reports the incoming account once it publishes progress of its own`() = runTest {
        // Given
        appInBackground.value = true
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser, completedUsers = 1, userCount = 2))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Completed
        activity.emit(ContentIndexingActivity.WaitingOnUsers)
        runCurrent()

        // When
        activity.emit(progress(activeUserId = SecondUser, completedUsers = 1, userCount = 2))
        indexingStates.getValue(SecondUser).value = ContentIndexingState.Running(percentage = 3.0)
        runCurrent()

        // Then
        verify { ContentIndexingNotification.build(any(), "user-2@proton.me", 3.0) }
        work.cancel()
    }

    @Test
    fun `takes the service while the orchestrator is between accounts`() = runTest {
        // Given - an account is still to come, and standby quota is charged from the moment the app
        // leaves the screen, so the promotion cannot wait for that account's first progress.
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser, completedUsers = 1, userCount = 2))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 62.5)
        activity.emit(ContentIndexingActivity.ForwardMode(FirstUser))
        runCurrent()

        // When - the app leaves the screen mid-handover.
        appInBackground.value = true
        runCurrent()

        // Then - showing the outgoing account, rather than nothing at all.
        verify { ContentIndexingNotification.build(any(), "user-1@proton.me", 62.5) }
        work.cancel()
    }

    @Test
    fun `gives up when the orchestrator never answers whether it has work`() = runTest {
        // Given
        coEvery { startContentIndexing() } coAnswers { awaitCancellation() }
        givenActivity(flow { awaitCancellation() })

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.OrchestratorAskTimeout.inWholeMilliseconds + 1).milliseconds)

        // Then
        assertTrue(work.isActive)
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds + 1).milliseconds)
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `takes the service once the next account actually starts indexing`() = runTest {
        // Given
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser, completedUsers = 1, userCount = 2))
        activity.emit(ContentIndexingActivity.WaitingOnUsers)
        runCurrent()
        appInBackground.value = true
        runCurrent()

        // When
        activity.emit(progress(activeUserId = SecondUser, completedUsers = 1, userCount = 2))
        runCurrent()

        // Then
        verify(atLeast = 1) { ContentIndexingNotification.build(any(), any(), any()) }
        work.cancel()
    }

    @Test
    fun `tells the scheduler it is leaving before it unwinds`() = runTest {
        // Given.
        givenActivity(flowOf(progress(completedUsers = 2), ContentIndexingActivity.Stopped))

        // When
        worker().doWork()

        // Then
        verify { workScheduler.markWorkerExiting() }
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
    fun `gives up quickly when the orchestrator says there is nothing to index`() = runTest {
        // Given - a worker is enqueued on every foreground transition whether or not there is work,
        // and one there was never anything for should not hold a job for the full idle timeout.
        coEvery { startContentIndexing() } returns startSummary(completed = 1).right()
        givenActivity(flow { awaitCancellation() })

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds + 1).milliseconds)

        // Then
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `waits for an orchestrator that has work but has not reported it yet`() = runTest {
        // Given - a large first backfill can stay quiet for a while before the first event.
        givenActivity(
            flow {
                delay((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds * 3).milliseconds)
                emit(progress())
                emit(ContentIndexingActivity.Stopped)
            }
        )

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds * 3 + 1).milliseconds)

        // Then
        assertEquals(ListenableWorker.Result.success(), work.await())
        coVerify(atLeast = 1) { observeContentIndexingActivity() }
    }

    @Test
    fun `keeps waiting once progress has arrived, whatever the stats said`() = runTest {
        // Given - the stats are read while Rust is still bringing accounts up, so a fresh login can
        // be reported as nothing to index for a moment.
        coEvery { startContentIndexing() } returns startSummary(completed = 1).right()
        givenActivity(
            flow {
                emit(progress())
                delay((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds * 3).milliseconds)
                emit(ContentIndexingActivity.Stopped)
            }
        )

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds * 3 + 1).milliseconds)

        // Then
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `assumes there is work when the orchestrator cannot be asked`() = runTest {
        // Given - being wrong here costs an idle job; the other way costs an unprotected backfill.
        coEvery { startContentIndexing() } returns DataError.Local.Unknown.left()
        givenActivity(flow { awaitCancellation() })

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds + 1).milliseconds)

        // Then - still waiting, past the short timeout but not for ever.
        assertTrue(work.isActive)
        advanceTimeBy((ContentIndexingWorker.StartupTimeout.inWholeMilliseconds + 1).milliseconds)
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    @Test
    fun `gives up when the orchestrator promises work and then publishes none`() = runTest {
        // Given
        coEvery { startContentIndexing() } returns startSummary(pending = 1).right()
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.StartupTimeout.inWholeMilliseconds - 1).milliseconds)

        // Then
        assertTrue(work.isActive)
        advanceTimeBy(2.milliseconds)
        assertEquals(ListenableWorker.Result.success(), work.await())
    }

    private fun startSummary(pending: Long = 0, completed: Long = 0) = ContentIndexingStartSummary(
        pending = pending,
        ongoing = 0,
        completed = completed,
        failed = 0,
        disabled = 0,
        total = pending + completed
    )

    @Test
    fun `the watchdog measures the gap between emissions, not the total run`() = runTest {
        // Given - steady progress well past the idle timeout in total.
        givenActivity(
            flow {
                emit(progress())
                repeat(4) {
                    delay((ContentIndexingWorker.IdleTimeout.inWholeMilliseconds / 2).milliseconds)
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

        // Then
        verify(exactly = 0) { ContentIndexingNotification.build(any(), any(), any()) }
    }

    @Test
    fun `takes the service the moment the app is backgrounded`() = runTest {
        // Given - not debounced: a job only escapes JobScheduler's standby quota while the app holds
        // a foreground service, and quota is charged as soon as the app stops being on top.
        appInBackground.value = true
        givenActivity(
            flow {
                emit(progress())
                awaitCancellation()
            }
        )

        // When
        val work = async { worker().doWork() }
        runCurrent()

        // Then
        verify(atLeast = 1) { ContentIndexingNotification.build(any(), any(), any()) }
        work.cancel()
    }

    @Test
    fun `reports the account's own progress rather than the orchestrator's`() = runTest {
        // Given - the orchestrator's percentage is summed across every account, so it does not
        // describe the one account the notification names. The settings screen reads the per-account
        // stream this asserts on, and the two must agree.
        appInBackground.value = true
        givenActivity(
            flow {
                emit(progress(activeUserId = FirstUser, percentage = 12.0))
                awaitCancellation()
            }
        )
        val work = async { worker().doWork() }
        runCurrent()

        // When
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 41.6)
        runCurrent()

        // Then
        verify { ContentIndexingNotification.build(any(), "user-1@proton.me", 41.6) }
        verify(exactly = 0) { ContentIndexingNotification.build(any(), any(), 12.0) }
        work.cancel()
    }

    @Test
    fun `follows the orchestrator on to the next account`() = runTest {
        // Given - the first account is done and Rust has moved on, so a notification still naming it
        // would sit on a percentage that never moves again.
        appInBackground.value = true
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 100.0)
        runCurrent()

        // When
        activity.emit(progress(activeUserId = SecondUser))
        indexingStates.getValue(SecondUser).value = ContentIndexingState.Running(percentage = 3.0)
        runCurrent()

        // Then
        verify { ContentIndexingNotification.build(any(), "user-2@proton.me", 3.0) }
        work.cancel()
    }

    @Test
    fun `keeps the account it was given while Rust hands over to the next one`() = runTest {
        // Given - the orchestrator reports no active account between accounts, which would otherwise
        // leave the notification without a subject for as long as the handover takes.
        appInBackground.value = true
        val activity = MutableSharedFlow<ContentIndexingActivity>()
        givenActivity(activity)
        val work = async { worker().doWork() }
        runCurrent()
        activity.emit(progress(activeUserId = FirstUser))
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 41.6)
        runCurrent()

        // When
        activity.emit(progress(activeUserId = null))
        runCurrent()

        // Then - still the account it was last told about, rather than an unnamed notification.
        val addresses = mutableListOf<String?>()
        verify { ContentIndexingNotification.build(any(), captureNullable(addresses), any()) }
        assertEquals("user-1@proton.me", addresses.last())
        work.cancel()
    }

    @Test
    fun `waits rather than reporting a percentage for an account that has finished`() = runTest {
        // Given - Rust leaves a caught-up account in Running, which would otherwise sit at 100%.
        appInBackground.value = true
        givenActivity(
            flow {
                emit(progress(activeUserId = FirstUser))
                awaitCancellation()
            }
        )
        val work = async { worker().doWork() }
        runCurrent()

        // When
        indexingStates.getValue(FirstUser).value = ContentIndexingState.Running(percentage = 100.0)
        runCurrent()

        // Then
        verify(exactly = 0) { ContentIndexingNotification.build(any(), any(), 100.0) }
        work.cancel()
    }

    @Test
    fun `takes the service before any progress when there is indexing to protect`() = runTest {
        // Given - a large first backfill can stay quiet for a while, and is unprotected until the
        // service is held, so promotion does not wait for the first event.
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        runCurrent()

        // When
        appInBackground.value = false
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then - it has a service to hand back, so it ends itself to do it.
        coVerify(exactly = 1) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `does not take the service when there is nothing to index`() = runTest {
        // Given - a worker is enqueued on every foreground transition whether or not there is work,
        // and one that promoted anyway would flash "Preparing" for every trip to the background.
        appInBackground.value = true
        coEvery { startContentIndexing() } returns startSummary(completed = 1).right()
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        runCurrent()

        // When
        appInBackground.value = false
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then - nothing was ever acquired, so there is nothing to give back either.
        coVerify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `takes the service when progress turns up after all`() = runTest {
        // Given - a worker told there was nothing to index, because the stats were read while Rust
        // was still bringing accounts up.
        appInBackground.value = true
        coEvery { startContentIndexing() } returns startSummary(completed = 1).right()
        givenActivity(
            flow {
                delay((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds / 2).milliseconds)
                emit(progress())
                awaitCancellation()
            }
        )

        // When
        val work = async { worker().doWork() }
        advanceTimeBy((ContentIndexingWorker.NoWorkTimeout.inWholeMilliseconds / 2 + 1).milliseconds)

        // Then - the account address is read, which only happens to build a notification.
        coVerify(atLeast = 1) { userSessionRepository.getAccount(FirstUser) }
        work.cancel()
    }

    @Test
    fun `replaces itself when the app comes back on screen`() = runTest {
        // Given - the service was acquired while the app was away.
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        runCurrent()

        // When
        appInBackground.value = false
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then - the only way to give the foreground service back is to end this worker.
        coVerify(exactly = 1) { workScheduler.restart() }
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
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // When
        appInBackground.value = false
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then
        coVerify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `does not replace itself when the app was never off screen`() = runTest {
        // Given
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }

        // When - a foreground app that stays foregrounded holds no service to give back.
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then
        coVerify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    @Test
    fun `keeps the service through a brief return to the screen`() = runTest {
        // Given - a permission dialog or a share sheet, not the user coming back to stay.
        appInBackground.value = true
        givenActivity(flow { awaitCancellation() })
        val work = async { worker().doWork() }
        runCurrent()

        // When
        appInBackground.value = false
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds / 2).milliseconds)
        appInBackground.value = true
        advanceTimeBy((ContentIndexingWorker.ForegroundReturnDebounce.inWholeMilliseconds + 1).milliseconds)

        // Then - replacing the worker here would drop and re-acquire the service for nothing.
        coVerify(exactly = 0) { workScheduler.restart() }
        work.cancel()
    }

    private fun account(userId: UserId) = mockk<Account> {
        every { primaryAddress } returns "${userId.id}@proton.me"
    }

    private fun givenActivity(flow: Flow<ContentIndexingActivity>) {
        every { observeContentIndexingActivity() } returns flow
    }

    private fun progress(
        activeUserId: UserId? = FirstUser,
        completedUsers: Long = 0,
        userCount: Long = 2,
        percentage: Double = 10.0
    ) = ContentIndexingActivity.Progress(
        activeUserId = activeUserId,
        percentage = percentage,
        processedMessages = 10,
        totalMessages = 100,
        completedUsers = completedUsers,
        userCount = userCount
    )

    private companion object {

        val FirstUser = UserId("user-1")
        val SecondUser = UserId("user-2")
    }
}
