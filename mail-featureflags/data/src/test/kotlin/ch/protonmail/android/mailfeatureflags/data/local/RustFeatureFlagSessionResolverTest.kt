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

package ch.protonmail.android.mailfeatureflags.data.local

import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailUserSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RustFeatureFlagSessionResolverTest {

    private val rustMailSession: MailSession = mockk()
    private val mailSessionWrapper: MailSessionWrapper = mockk {
        every { getRustMailSession() } returns rustMailSession
    }
    private val rustUserSession: MailUserSession = mockk()
    private val userSessionWrapper: MailUserSessionWrapper = mockk {
        every { getRustUserSession() } returns rustUserSession
    }

    private val mailSessionRepository: MailSessionRepository = mockk {
        every { getMailSession() } returns mailSessionWrapper
    }
    private val userSessionRepository: UserSessionRepository = mockk()

    private val resolver = RustFeatureFlagSessionResolver(
        userSessionRepository = userSessionRepository,
        mailSessionRepository = mailSessionRepository
    )

    private val userId = UserId("user-123")

    @Test
    fun `resolves the user session when a primary user is logged in`() = runTest {
        // Given
        every { mailSessionRepository.isMailSessionInitialised() } returns true
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery { userSessionRepository.getUserSession(userId) } returns userSessionWrapper

        // When
        val result = resolver.activeSession()

        // Then
        assertEquals(ActiveSession.User(rustUserSession), result)
    }

    @Test
    fun `resolves the app session when the mail session is not initialised`() = runTest {
        // Given
        every { mailSessionRepository.isMailSessionInitialised() } returns false

        // When
        val result = resolver.activeSession()

        // Then
        assertEquals(ActiveSession.App(rustMailSession), result)
    }

    @Test
    fun `resolves the app session when there is no primary user`() = runTest {
        // Given
        every { mailSessionRepository.isMailSessionInitialised() } returns true
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(null)

        // When
        val result = resolver.activeSession()

        // Then
        assertTrue(result is ActiveSession.App)
    }
}
