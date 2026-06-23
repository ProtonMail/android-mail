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
import uniffi.mail_uniffi.ResolverRequest
import uniffi.mail_uniffi.ResolverRequestStream
import uniffi.mail_uniffi.ResolverRequestStreamNextAsyncResult

/**
 * Resolves the hostnames Rust requests through [AndroidDnsResolver].
 *
 * Only created when `ApiConfig.useCustomResolver` is set.
 */
internal class ResolverRequestStreamDriver(
    private val stream: ResolverRequestStream,
    private val dnsResolver: AndroidDnsResolver
) : RustRequestStreamDriver<ResolverRequest>(name = "resolver", concurrent = true) {

    override suspend fun awaitRequest(): Poll<ResolverRequest> = when (val result = stream.nextAsync()) {
        is ResolverRequestStreamNextAsyncResult.Ok -> Poll.Request(result.v1)
        is ResolverRequestStreamNextAsyncResult.Error -> Poll.Closed(result.v1.toString())
    }

    override suspend fun answer(request: ResolverRequest) {
        request.respond(dnsResolver.resolve(request.host()))
    }
}
