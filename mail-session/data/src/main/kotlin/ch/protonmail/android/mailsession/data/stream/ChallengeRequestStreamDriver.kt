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

import kotlinx.coroutines.CancellationException
import me.proton.android.core.humanverification.domain.ChallengeNotifierCallback
import timber.log.Timber
import uniffi.mail_uniffi.ChallengeRequest
import uniffi.mail_uniffi.ChallengeRequestStream
import uniffi.mail_uniffi.ChallengeRequestStreamNextAsyncResult
import uniffi.mail_uniffi.ChallengeResponse

/**
 * Forwards human-verification challenges from Rust to [ChallengeNotifierCallback].
 *
 * Handled sequentially: a single challenge is presented at a time.
 */
internal class ChallengeRequestStreamDriver(
    private val stream: ChallengeRequestStream,
    private val challengeNotifier: ChallengeNotifierCallback
) : RustRequestStreamDriver<ChallengeRequest>(name = "challenge") {

    override suspend fun awaitRequest(): Poll<ChallengeRequest> = when (val result = stream.nextAsync()) {
        is ChallengeRequestStreamNextAsyncResult.Ok -> Poll.Request(result.v1)
        is ChallengeRequestStreamNextAsyncResult.Error -> Poll.Closed(result.v1.toString())
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun answer(request: ChallengeRequest) {
        val response = try {
            request.server().use { server ->
                request.payload().use { payload ->
                    challengeNotifier.onChallenge(server, payload)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.e(error, "rust-stream: 'challenge' failed to resolve a challenge; responding with Failure")
            ChallengeResponse.Failure
        }
        request.respond(response)
    }
}
