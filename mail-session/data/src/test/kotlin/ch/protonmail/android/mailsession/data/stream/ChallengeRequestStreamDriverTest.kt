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

package ch.protonmail.android.mailsession.data.stream

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.android.core.humanverification.domain.ChallengeNotifierCallback
import uniffi.mail_uniffi.ChallengeRequest
import uniffi.mail_uniffi.ChallengeRequestStream
import uniffi.mail_uniffi.ChallengeRequestStreamNextAsyncResult
import uniffi.mail_uniffi.ChallengeResponse
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test

internal class ChallengeRequestStreamDriverTest {

    private val challengeNotifier = mockk<ChallengeNotifierCallback>()
    private val stream = mockk<ChallengeRequestStream>()

    @Test
    fun `forwards the challenge to the notifier and responds with its result`() = runTest {
        // Given
        val request = mockk<ChallengeRequest>(relaxed = true)
        coEvery { challengeNotifier.onChallenge(any(), any()) } returns ChallengeResponse.Cancelled
        coEvery { stream.nextAsync() } returnsMany listOf(
            ChallengeRequestStreamNextAsyncResult.Ok(request),
            ChallengeRequestStreamNextAsyncResult.Error(mockk<ProtonError>())
        )

        // When
        ChallengeRequestStreamDriver(stream, challengeNotifier).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { challengeNotifier.onChallenge(any(), any()) }
        coVerify(exactly = 1) { request.respond(ChallengeResponse.Cancelled) }
        verify(exactly = 1) { request.close() }
    }

    @Test
    fun `responds with Failure and keeps the stream alive when the notifier throws`() = runTest {
        // Given
        val failing = mockk<ChallengeRequest>(relaxed = true)
        val recovered = mockk<ChallengeRequest>(relaxed = true)
        coEvery { challengeNotifier.onChallenge(any(), any()) } throws IllegalStateException("boom") andThen
            ChallengeResponse.Cancelled
        coEvery { stream.nextAsync() } returnsMany listOf(
            ChallengeRequestStreamNextAsyncResult.Ok(failing),
            ChallengeRequestStreamNextAsyncResult.Ok(recovered),
            ChallengeRequestStreamNextAsyncResult.Error(mockk<ProtonError>())
        )

        // When
        ChallengeRequestStreamDriver(stream, challengeNotifier).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { failing.respond(ChallengeResponse.Failure) }
        coVerify(exactly = 1) { recovered.respond(ChallengeResponse.Cancelled) }
        verify(exactly = 1) { failing.close() }
        verify(exactly = 1) { recovered.close() }
    }

    @Test
    fun `does not notify when the stream is closed`() = runTest {
        // Given
        coEvery { stream.nextAsync() } returns
            ChallengeRequestStreamNextAsyncResult.Error(mockk<ProtonError>())

        // When
        ChallengeRequestStreamDriver(stream, challengeNotifier).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { challengeNotifier.onChallenge(any(), any()) }
    }
}
