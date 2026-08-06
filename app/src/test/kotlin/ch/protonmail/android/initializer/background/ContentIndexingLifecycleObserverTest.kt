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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        verify(exactly = 1) { workScheduler.enqueueIfWorkPending() }
    }

    @Test
    fun `enqueues the worker when an account is already being indexed`() = runTest {
        // Given - a run still going from before the app was last closed also needs the service.
        givenStartSummary(summary(ongoing = 1))

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        verify(exactly = 1) { workScheduler.enqueueIfWorkPending() }
    }

    @Test
    fun `does not enqueue a worker when every account is already settled`() = runTest {
        // Given - nothing to drive, so there is no reason to hold a foreground service.
        givenStartSummary(summary(completed = 2, disabled = 1))

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { startContentIndexing() }
        verify(exactly = 0) { workScheduler.enqueueIfWorkPending() }
    }

    @Test
    fun `does not enqueue a worker when the orchestrator could not be started`() = runTest {
        // Given
        coEvery { startContentIndexing() } returns DataError.Local.Unknown.left()

        // When
        observer().onStart(lifecycleOwner())
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { workScheduler.enqueueIfWorkPending() }
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
        verify(exactly = 0) { workScheduler.enqueueIfWorkPending() }
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

    private fun TestScope.observer() = ContentIndexingLifecycleObserver(
        isContentSearchFeatureEnabled = isContentSearchFeatureEnabled,
        startContentIndexing = startContentIndexing,
        workScheduler = workScheduler,
        appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
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
