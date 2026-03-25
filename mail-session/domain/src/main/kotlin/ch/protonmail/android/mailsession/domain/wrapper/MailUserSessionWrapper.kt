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

package ch.protonmail.android.mailsession.domain.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.mapper.toEventLoopError
import ch.protonmail.android.mailsession.domain.model.EventLoopError
import timber.log.Timber
import uniffi.mail_uniffi.AsyncLiveQueryCallback
import uniffi.mail_uniffi.EventLoopErrorObserver
import uniffi.mail_uniffi.ExecuteWhenOnlineCallbackAsync
import uniffi.mail_uniffi.Fork
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionForkResult
import uniffi.mail_uniffi.MailUserSessionOverrideUserFeatureFlagResult
import uniffi.mail_uniffi.MailUserSessionUserResult
import uniffi.mail_uniffi.MeasurementEventType
import uniffi.mail_uniffi.MeasurementValue
import uniffi.mail_uniffi.User
import uniffi.mail_uniffi.VoidEventResult

class MailUserSessionWrapper(private val userSession: MailUserSession) {

    fun getRustUserSession() = userSession

    suspend fun fork(): Either<DataError, Fork> = when (val result = userSession.fork("android", "mail")) {
        is MailUserSessionForkResult.Error -> result.v1.toDataError().left()
        is MailUserSessionForkResult.Ok -> result.v1.right()
    }

    suspend fun pollEvents(): Either<EventLoopError, Unit> = when (val result = userSession.forceEventLoopPoll()) {
        is VoidEventResult.Error -> result.v1.toEventLoopError().left()
        VoidEventResult.Ok -> Unit.right()
    }

    suspend fun pollEventsAndWait(): Either<EventLoopError, Unit> =
        when (val result = userSession.forceEventLoopPollAndWait()) {
            is VoidEventResult.Error -> result.v1.toEventLoopError().left()
            VoidEventResult.Ok -> Unit.right()
        }

    suspend fun imageForSender(address: String, bimi: String?) = userSession.imageForSender(
        address,
        bimi,
        true,
        128u,
        null,
        "png"
    )

    suspend fun getAttachment(attachmentId: LocalAttachmentId) = userSession.getAttachment(attachmentId)

    fun watchUser(callback: AsyncLiveQueryCallback) = userSession.watchUser(callback)

    fun watchUserStream() = userSession.watchUserStream()

    suspend fun getUser(): Either<DataError, User> = when (val result = userSession.user()) {
        is MailUserSessionUserResult.Error -> result.v1.toDataError().left()
        is MailUserSessionUserResult.Ok -> result.v1.right()
    }

    fun executeWhenOnline(block: () -> Unit) {
        val callback = object : ExecuteWhenOnlineCallbackAsync {
            override suspend fun onOnline() {
                block()
            }
        }
        userSession.executeWhenOnlineAsync(callback)
    }

    fun observeEventLoopErrors(callback: EventLoopErrorObserver) = userSession.observeEventLoopErrors(callback)

    suspend fun isFeatureEnabled(featureId: String) = userSession.isFeatureEnabled(featureId = featureId)

    suspend fun overrideFeatureFlag(flagName: String, newValue: Boolean) =
        when (val result = userSession.overrideUserFeatureFlag(flagName = flagName, newValue = newValue)) {
            is MailUserSessionOverrideUserFeatureFlagResult.Ok -> Unit.right()
            is MailUserSessionOverrideUserFeatureFlagResult.Error -> {
                Timber.e("MailUserSession FeatureFlag Override:: Unable set set feature flag ${result.v1}")
                result.v1.toDataError().left()
            }
        }

    suspend fun sendMeasurementEvent(
        eventType: MeasurementEventType,
        asid: String,
        appPackageName: String,
        fields: Map<String, MeasurementValue?>
    ) = userSession.recordMeasurement(eventType, asid, appPackageName, fields)
}
