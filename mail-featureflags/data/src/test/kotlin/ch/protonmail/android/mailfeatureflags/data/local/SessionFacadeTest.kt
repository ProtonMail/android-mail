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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import uniffi.mail_uniffi.MailUserSessionIsFeatureEnabledResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionFacadeTest {

    private val userSessionRepository: UserSessionRepository = mockk()
    private val mailSessionRepository: MailSessionRepository = mockk()
    private val mockMailSession: MailSessionWrapper = mockk(relaxed = true)
    private val mockUserSessionWrapper: MailUserSessionWrapper = mockk()

    private lateinit var facade: SessionFacade

    private val featureKey = "test_feature"
    private val userId = UserId("user-123")

    @Before
    fun setup() {
        // Given
        facade = SessionFacade(userSessionRepository, mailSessionRepository)

        every { mailSessionRepository.getMailSession() } returns mockMailSession
    }

    @Test
    fun `should return user id when observer emits a value`() = runTest {
        // Given
        coEvery { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)

        // When
        val result = facade.getUserId()

        // Then
        assertEquals(userId, result)
    }

    @Test
    fun `should return null when user id observer emits nothing`() = runTest {
        // Given
        coEvery { userSessionRepository.observePrimaryUserId() } returns flowOf()

        // When
        val result = facade.getUserId()

        // Then
        assertNull(result)
    }

    @Test
    fun `should return user session wrapper when repository finds session`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSessionWrapper

        // When
        val result = facade.getUserSession(userId)

        // Then
        assertEquals(mockUserSessionWrapper, result)
    }

    @Test
    fun `should return null when repository does not find session`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = facade.getUserSession(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return initialization status from mail repository`() = runTest {
        // Given
        every { mailSessionRepository.isMailSessionInitialised() } returns true

        // When
        val result = facade.isMailSessionInitialised()

        // Then
        assertTrue(result)
        coVerify(exactly = 1) { mailSessionRepository.isMailSessionInitialised() }
    }

    @Test
    fun `should return true when user session feature check is ok true`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSessionWrapper
        coEvery {
            mockUserSessionWrapper
                .isFeatureEnabled(featureKey)
        } returns MailUserSessionIsFeatureEnabledResult.Ok(
            v1 = true
        )

        // When
        val result = facade.getIsUserSessionFeatureEnabled(featureKey, userId)

        // Then
        assert(result is MailUserSessionIsFeatureEnabledResult.Ok)
        assert((result as MailUserSessionIsFeatureEnabledResult.Ok).v1 == true)
        coVerify(exactly = 1) { userSessionRepository.getUserSession(userId) }
    }

    @Test
    fun `should return null when user session is not found for feature check`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = facade.getIsUserSessionFeatureEnabled(featureKey, userId)

        // Then
        assert(result == null)
        coVerify(exactly = 1) { userSessionRepository.getUserSession(userId) }
        coVerify(exactly = 0) { mockUserSessionWrapper.isFeatureEnabled(any()) }
    }

    @Test
    fun `should return error when user session feature check returns error`() = runTest {
        // Given
        val errorResult = MailUserSessionIsFeatureEnabledResult.Error(ProtonError.Network)
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSessionWrapper
        coEvery { mockUserSessionWrapper.isFeatureEnabled(featureKey) } returns errorResult

        // When
        val result = facade.getIsUserSessionFeatureEnabled(featureKey, userId)

        // Then
        assert(result == errorResult)
        coVerify(exactly = 1) { userSessionRepository.getUserSession(userId) }
    }
}
