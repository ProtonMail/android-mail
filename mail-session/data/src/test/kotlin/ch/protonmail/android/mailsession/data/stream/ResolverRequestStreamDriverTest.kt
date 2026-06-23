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

import ch.protonmail.android.mailsession.data.network.AndroidDnsResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.ResolverOutcome
import uniffi.mail_uniffi.ResolverRequest
import uniffi.mail_uniffi.ResolverRequestStream
import uniffi.mail_uniffi.ResolverRequestStreamNextAsyncResult
import kotlin.test.Test

internal class ResolverRequestStreamDriverTest {

    private val dnsResolver = mockk<AndroidDnsResolver>()
    private val stream = mockk<ResolverRequestStream>()

    @Test
    fun `resolves the requested host and responds with the outcome then disposes the request`() = runTest {
        // Given
        val request = mockk<ResolverRequest>(relaxed = true)
        val outcome = ResolverOutcome.Resolved(emptyList())
        every { request.host() } returns "proton.me"
        coEvery { dnsResolver.resolve("proton.me") } returns outcome
        coEvery { stream.nextAsync() } returnsMany listOf(
            ResolverRequestStreamNextAsyncResult.Ok(request),
            ResolverRequestStreamNextAsyncResult.Error(mockk<ProtonError>())
        )

        // When
        ResolverRequestStreamDriver(stream, dnsResolver).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { dnsResolver.resolve("proton.me") }
        coVerify(exactly = 1) { request.respond(outcome) }
        verify(exactly = 1) { request.close() }
    }

    @Test
    fun `does not resolve when the stream is closed`() = runTest {
        // Given
        coEvery { stream.nextAsync() } returns
            ResolverRequestStreamNextAsyncResult.Error(mockk<ProtonError>())

        // When
        ResolverRequestStreamDriver(stream, dnsResolver).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { dnsResolver.resolve(any()) }
    }
}
