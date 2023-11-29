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

package ch.protonmail.android.mailnotifications.domain

import java.util.UUID
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailnotifications.domain.handler.SessionAwareNotificationHandler
import ch.protonmail.android.mailnotifications.domain.handler.SessionAwareNotificationHandler.Companion.DISMISSAL_DELAY
import ch.protonmail.android.mailnotifications.domain.usecase.DismissEmailNotificationsForUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test

internal class SessionAwareNotificationHandlerDismissalTests {

    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val dismissEmailNotificationsForUser: DismissEmailNotificationsForUser = mockk(relaxUnitFun = true)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val testDispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private val notificationHandler = SessionAwareNotificationHandler(
        appInBackgroundState,
        observePrimaryUserId,
        dismissEmailNotificationsForUser,
        scope
    )

    @After
    fun resetMocks() {
        unmockkAll()
    }

    @Test
    fun `should call dismissal on not null userId and app in foreground`() = runTest(testDispatcher) {
        // given
        val userId = UserId(UUID.randomUUID().toString())
        every { observePrimaryUserId() } returns flowOf(userId)
        every { appInBackgroundState.observe() } returns flowOf(false)

        // when
        notificationHandler.handle()
        advanceUntilIdle()

        // then
        verify(exactly = 1) { dismissEmailNotificationsForUser(userId) }
    }

    @Test
    fun `should not call dismissal multiple times within a given period`() = runTest(testDispatcher) {
        // given
        val userId = UserId(UUID.randomUUID().toString())
        val userIdTwo = UserId(UUID.randomUUID().toString())
        val userIdFlow = flow {
            emit(userId)
            emit(userIdTwo)
        }

        every { observePrimaryUserId() } returns userIdFlow
        every { appInBackgroundState.observe() } returns flowOf(false)

        // when
        notificationHandler.handle()
        advanceTimeBy(DISMISSAL_DELAY * 2)
        advanceUntilIdle()

        // then
        verify(exactly = 0) { dismissEmailNotificationsForUser(userId) }
        verify(exactly = 1) { dismissEmailNotificationsForUser(userIdTwo) }
    }

    @Test
    fun `should call dismissal once if the app is brought to the foreground`() = runTest(testDispatcher) {
        // given
        val userId = UserId(UUID.randomUUID().toString())
        val userIdFlow = flow { emit(userId) }
        val backgroundFlow = flow {
            emit(false)
            delay(DISMISSAL_DELAY + 1)
            emit(true)
        }

        every { observePrimaryUserId() } returns userIdFlow
        every { appInBackgroundState.observe() } returns backgroundFlow

        // when
        notificationHandler.handle()
        advanceTimeBy(DISMISSAL_DELAY * 2)
        advanceUntilIdle()

        // then
        verify(exactly = 1) { dismissEmailNotificationsForUser(userId) }
    }

    @Test
    fun `should call dismissal again if another event is delivered after the threshold`() = runTest(testDispatcher) {
        // given
        val userId = UserId(UUID.randomUUID().toString())
        val userIdTwo = UserId(UUID.randomUUID().toString())
        val userIdFlow = flow {
            emit(userId)
            delay(DISMISSAL_DELAY + 1)
            emit(userIdTwo)
        }

        every { observePrimaryUserId() } returns userIdFlow
        every { appInBackgroundState.observe() } returns flowOf(false)

        // when
        notificationHandler.handle()
        advanceTimeBy(DISMISSAL_DELAY * 2)
        advanceUntilIdle()

        // then
        verify(exactly = 1) { dismissEmailNotificationsForUser(userId) }
        verify(exactly = 1) { dismissEmailNotificationsForUser(userIdTwo) }
    }
}
