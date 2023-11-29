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

import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailnotifications.domain.handler.SessionAwareNotificationHandler
import ch.protonmail.android.mailnotifications.domain.usecase.DismissEmailNotificationsForUser
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class SessionAwareNotificationHandlerNoDismissalTests(
    @Suppress("UNUSED_PARAMETER") testName: String,
    private val testInput: TestInput
) {

    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val dismissEmailNotificationsForUser: DismissEmailNotificationsForUser = mockk()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val scope = TestScope()
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
    fun `should not call notification dismissal`() = with(testInput) {
        every { observePrimaryUserId() } returns flowOf(userId)
        every { appInBackgroundState.observe() } returns flowOf(isInBackground)

        // when
        notificationHandler.handle()
        scope.advanceUntilIdle()

        // then
        verify { dismissEmailNotificationsForUser wasNot called }
    }

    companion object {

        private val noNotificationDismissal = listOf(
            TestInput(userId = null, isInBackground = true),
            TestInput(userId = null, isInBackground = false),
            TestInput(userId = UserId("id"), isInBackground = true)
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return noNotificationDismissal
                .map { testInput ->
                    val testName = """
                        UserId: ${testInput.userId}
                        IsInBackground: ${testInput.isInBackground}
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }

        data class TestInput(val userId: UserId?, val isInBackground: Boolean)
    }
}
