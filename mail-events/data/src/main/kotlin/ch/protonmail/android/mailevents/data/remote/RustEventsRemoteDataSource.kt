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

package ch.protonmail.android.mailevents.data.remote

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.data.remote.model.EventPayload
import ch.protonmail.android.mailevents.data.remote.model.toMeasurementEventType
import ch.protonmail.android.mailevents.data.remote.model.toMeasurementFields
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import javax.inject.Inject

class RustEventsRemoteDataSource @Inject constructor(
    private val mailSessionRepository: MailSessionRepository,
    private val userSessionRepository: UserSessionRepository
) : EventsRemoteDataSource {

    override suspend fun sendEvent(payload: EventPayload): Either<DataError, Unit> {
        val eventType = payload.toMeasurementEventType()
        val fields = payload.toMeasurementFields()

        @Suppress("TooGenericExceptionCaught")
        return try {
            val primaryUserId = userSessionRepository.observePrimaryUserId().firstOrNull()
            val userSession = primaryUserId?.let { userSessionRepository.getUserSession(it) }

            if (userSession != null) {
                userSession.sendMeasurementEvent(
                    eventType, payload.metadata.asid, payload.metadata.appPackageName, fields
                )
            } else {
                val mailSession = mailSessionRepository.getMailSession()
                mailSession.sendMeasurementEvent(
                    eventType, payload.metadata.asid, payload.metadata.appPackageName, fields
                )
            }
            Unit.right()
        } catch (e: Exception) {
            Timber.e(e, "Failed to send measurement event: ${payload.eventType}")
            DataError.Remote.Unknown.left()
        }
    }
}
