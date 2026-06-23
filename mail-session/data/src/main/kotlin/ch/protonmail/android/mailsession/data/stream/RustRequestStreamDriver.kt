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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Drives one Rust request stream for the lifetime of the session.
 *
 * Since the 0.167.6 Rust bump the foreign side no longer *implements* the callback traits
 * (device info, human verification, DNS resolution). Instead, Rust drives a channel: it parks a
 * request, we answer it through [answer] and the answer flows back as the `respond` argument.
 *
 * The contract for every stream is identical:
 * - loop on `nextAsync`
 * - answer the request
 * - repeat
 *
 * so the loop, request disposal and shutdown handling live here; subclasses only adapt their
 * generated request/result types in [awaitRequest] and [answer].
 *
 * @param concurrent when `true`, each request is answered in its own child coroutine so a slow
 * answer (e.g. a DNS lookup) does not stall the next request. Keep `false` for streams that
 * must be handled one at a time (e.g. human verification, which drives the UI).
 */
internal abstract class RustRequestStreamDriver<R : AutoCloseable>(
    private val name: String,
    private val concurrent: Boolean = false
) {

    /** Suspend until Rust parks the next request, or the stream is closed/cancelled. */
    protected abstract suspend fun awaitRequest(): Poll<R>

    /** Produce and deliver the answer for [request]. The request is disposed by the caller. */
    protected abstract suspend fun answer(request: R)

    suspend fun loop(scope: CoroutineScope) {
        while (currentCoroutineContext().isActive) {
            when (val poll = awaitRequest()) {
                is Poll.Request -> if (concurrent) {
                    scope.launch { poll.value.use { answerSafely(it) } }
                } else {
                    poll.value.use { answerSafely(it) }
                }

                is Poll.Closed -> {
                    Timber.w("rust-stream: '$name' stream closed (${poll.reason}); stopping driver")
                    return
                }
            }
        }
    }

    /**
     * Answers a single request, isolating its failure from the stream.
     *
     * Some streams like device-info and human-verification have answers that can throw; letting that propagate would
     * kill the driver coroutine and the stream would never be serviced again (there is no reconnect).
     * [CancellationException] is rethrown so cooperative cancellation still tears the loop down.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun answerSafely(request: R) {
        try {
            answer(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.e(error, "rust-stream: '$name' failed to answer a request; keeping the stream alive")
        }
    }

    sealed interface Poll<out R> {
        data class Request<R>(val value: R) : Poll<R>
        data class Closed(val reason: String) : Poll<Nothing>
    }
}
