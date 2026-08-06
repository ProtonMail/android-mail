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

package ch.protonmail.android.initializer.background

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import kotlin.test.Test

internal class ContentIndexingLifecycleObserverTest {

    private val dispatcher = TestDispatcherProvider().Main

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val isContentSearchFeatureEnabled = mockk<IsContentSearchFeatureEnabled> {
        coEvery { this@mockk.invoke() } returns true
    }
    private val startContentIndexing = mockk<StartContentIndexing>()
    private val activities = MutableSharedFlow<ContentIndexingActivity>(extraBufferCapacity = 8)
    private val observeContentIndexingActivity = mockk<ObserveContentIndexingActivity> {
        every { this@mockk.invoke() } returns activities
    }
    private val workScheduler = mockk<ContentIndexingWorkScheduler>(relaxUnitFun = true)

    @Test
    fun `starts the orchestrator and enqueues the worker when there is work pending`() = runTest {
        // Given
        givenStartSummary(summary(pending = 3))

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { startContentIndexing() }
        coVerify(exactly = 1) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `enqueues the worker when an account is already being indexed`() = runTest {
        // Given - a run still going from before the app was last closed also needs the service.
        givenStartSummary(summary(ongoing = 1))

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `enqueues a worker even when every account looks settled`() = runTest {
        // Given - the summary is taken while Rust is still bringing accounts up, and a worker
        // started after the app is backgrounded could no longer take a foreground service.
        givenStartSummary(summary(completed = 2, disabled = 1))

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then - the worker exits on its own if nothing turns up.
        coVerify(exactly = 1) { startContentIndexing() }
        coVerify(exactly = 1) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `does not enqueue a worker when the orchestrator could not be started`() = runTest {
        // Given
        coEvery { startContentIndexing() } returns DataError.Local.Unknown.left()

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `does nothing at all when the feature is off`() = runTest {
        // Given
        coEvery { isContentSearchFeatureEnabled() } returns false

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { startContentIndexing() }
        coVerify(exactly = 0) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `restarts the orchestrator on every foreground transition`() = runTest {
        // Given
        givenStartSummary(summary(pending = 1))
        val observer = observer()

        // When
        observer.onStart(lifecycleOwner())
        observer.onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { startContentIndexing() }
    }

    @Test
    fun `asks again when indexing starts after the summary was taken`() = runTest {
        // Given - nothing pending when the app came up, then an account signs in and the
        // orchestrator picks it up on its own. The worker from onStart may well have exited by now.
        givenStartSummary(summary(completed = 1))
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // When
        activities.emit(progress())
        advanceUntilIdle()

        // Then - one at start, one when progress turned up.
        coVerify(exactly = 2) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `asks once per burst of progress rather than once per event`() = runTest {
        // Given - progress arrives every batch; only the transition into it is interesting.
        givenStartSummary(summary(pending = 1))
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // When
        activities.emit(progress())
        activities.emit(progress())
        advanceUntilIdle()

        // Then - one at start, one for the burst.
        coVerify(exactly = 2) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `does not enqueue a worker once the app has left the screen`() = runTest {
        // Given - a worker enqueued from the background cannot be promoted to a service anyway.
        givenStartSummary(summary(completed = 1))
        val observer = observer()
        observer.onStart(lifecycleOwner())
        advanceUntilIdle()

        // When
        observer.onStop(lifecycleOwner())
        activities.emit(progress())
        advanceUntilIdle()

        // Then - only the one from onStart; the progress is ignored.
        coVerify(exactly = 1) { workScheduler.ensureWorkerRunning() }
    }

    @Test
    fun `ignores activity that means the orchestrator has nothing to drive`() = runTest {
        // Given
        givenStartSummary(summary(completed = 1))
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // When
        activities.emit(ContentIndexingActivity.WaitingOnUsers)
        activities.emit(ContentIndexingActivity.Stopped)
        advanceUntilIdle()

        // Then - only the one from onStart.
        coVerify(exactly = 1) { workScheduler.ensureWorkerRunning() }
    }

    private fun progress() = ContentIndexingActivity.Progress(
        activeUserId = UserId("user-id"),
        percentage = 12.0,
        processedMessages = 12,
        totalMessages = 100,
        completedUsers = 0,
        userCount = 1
    )

    private fun TestScope.observer() = ContentIndexingLifecycleObserver(
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
        startContentIndexing = startContentIndexing,
        observeContentIndexingActivity = observeContentIndexingActivity,
        workScheduler = workScheduler,
        appScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
    )

    private fun lifecycleOwner() = TestLifecycleOwner(Lifecycle.State.CREATED, dispatcher)

    private fun givenStartSummary(summary: ContentIndexingStartSummary) {
        coEvery { startContentIndexing() } returns summary.right()
    }

    private fun summary(
        pending: Long = 0,
        ongoing: Long = 0,
        completed: Long = 0,
        failed: Long = 0,
        disabled: Long = 0
    ) = ContentIndexingStartSummary(
        pending = pending,
        ongoing = ongoing,
        completed = completed,
        failed = failed,
        disabled = disabled,
        total = pending + ongoing + completed + failed + disabled
    )
}
