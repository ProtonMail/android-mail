/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnps.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionDismissNpsResult
import uniffi.mail_uniffi.MailUserSessionRecordNpsScoreResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class NpsRepositoryImplTest {

    private val userId = UserId("user")
    private val rustSession = mockk<MailUserSession>()
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns mockk<MailUserSessionWrapper> {
            every { getRustUserSession() } returns rustSession
        }
    }

    private val repository = NpsRepositoryImpl(
        ExecuteWithUserSession(userSessionRepository, UnconfinedTestDispatcher())
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a score and comment, when recording, then they are sent to the SDK`() = runTest {
        // Given
        coEvery { rustSession.recordNpsScore(8L, "great") } returns MailUserSessionRecordNpsScoreResult.Ok

        // When
        val result = repository.recordScore(userId, NpsScore(8), "great")

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { rustSession.recordNpsScore(8L, "great") }
    }

    @Test
    fun `given the SDK fails, when recording a score, then an error is returned`() = runTest {
        // Given
        coEvery { rustSession.recordNpsScore(any(), any()) } returns
            MailUserSessionRecordNpsScoreResult.Error(ProtonError.Network)

        // When
        val result = repository.recordScore(userId, NpsScore(8), comment = null)

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `given a dismissal, when recording it, then it is sent to the SDK`() = runTest {
        // Given
        coEvery { rustSession.dismissNps() } returns MailUserSessionDismissNpsResult.Ok

        // When
        val result = repository.dismiss(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { rustSession.dismissNps() }
    }

    @Test
    fun `given no user session, when recording a dismissal, then an error is returned`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = repository.dismiss(userId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }
}
