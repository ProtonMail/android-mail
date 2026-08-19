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

package ch.protonmail.android.payment.di

import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.repository.getPrimarySession
import dagger.Lazy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import me.proton.android.payment.capability.HttpCapability
import me.proton.android.payment.model.HttpResponse
import uniffi.mail_uniffi.MailUserSessionPaymentsHttpGetResult
import uniffi.mail_uniffi.MailUserSessionPaymentsHttpPostResult
import uniffi.mail_uniffi.PaymentsHttpResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCapabilityImpl @Inject constructor(
    private val sessionRepository: Lazy<UserSessionRepository>
) : HttpCapability {

    override suspend fun get(endpoint: String): HttpResponse {
        val session = sessionRepository.get().getPrimarySession()
            ?: throw IllegalStateException("Payments HTTP GET $endpoint: no primary session")
        return when (val result = session.paymentsHttpGet(endpoint)) {
            is MailUserSessionPaymentsHttpGetResult.Ok -> result.v1.toHttpResponse()
            is MailUserSessionPaymentsHttpGetResult.Error ->
                throw IOException("Payments HTTP GET $endpoint failed: ${result.v1}")
        }
    }

    override suspend fun post(endpoint: String, body: ByteArray?): HttpResponse {
        val session = sessionRepository.get().getPrimarySession()
            ?: throw IllegalStateException("Payments HTTP POST $endpoint: no primary session")
        return when (val result = session.paymentsHttpPost(endpoint, body ?: ByteArray(0))) {
            is MailUserSessionPaymentsHttpPostResult.Ok -> result.v1.toHttpResponse()
            is MailUserSessionPaymentsHttpPostResult.Error ->
                throw IOException("Payments HTTP POST $endpoint failed: ${result.v1}")
        }
    }
}

private fun PaymentsHttpResponse.toHttpResponse(): HttpResponse = HttpResponse(
    status = status.toInt(),
    body = if (body.isEmpty()) {
        null
    } else {
        Json.parseToJsonElement(body.toString(Charsets.UTF_8)).jsonObject
    }
)
