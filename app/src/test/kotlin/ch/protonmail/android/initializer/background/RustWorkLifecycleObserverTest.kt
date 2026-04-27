/*
 * Copyright (c) 2022 Proton Technologies AG
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
import ch.protonmail.android.mailsession.data.background.BackgroundExecutionWorkScheduler
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import kotlin.test.Test

internal class RustWorkLifecycleObserverTest {

    val dispatcher = TestDispatcherProvider().Main

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val scheduler = mockk<BackgroundExecutionWorkScheduler>()
    private val mailSessionRepository = mockk<MailSessionRepository>()
    private val observer = RustWorkLifecycleObserver(mailSessionRepository, scheduler)

    @Test
    fun `should cancel background execution and resume work when onStart is triggered`() = runTest {
        // Given
        coEvery { scheduler.cancelPendingWork() } just runs
        every { mailSessionRepository.getMailSession().onEnterForeground() } just runs
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED, dispatcher)

        // When
        observer.onStart(lifecycleOwner)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { scheduler.cancelPendingWork() }
        coVerify(exactly = 1) { mailSessionRepository.getMailSession().onEnterForeground() }
        confirmVerified(mailSessionRepository, scheduler)
    }

    @Test
    fun `should schedule background execution and pause work when onStop is triggered`() = runTest {
        // Given
        every { scheduler.scheduleWork() } just runs
        every { mailSessionRepository.getMailSession().onExitForeground() } just runs
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED, dispatcher)

        // When
        observer.onStop(lifecycleOwner)
        advanceUntilIdle()

        // Then
        verify(exactly = 1) { scheduler.scheduleWork() }
        coVerify(exactly = 1) { mailSessionRepository.getMailSession().onExitForeground() }
        confirmVerified(mailSessionRepository, scheduler)
    }
}
