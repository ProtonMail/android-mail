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

package ch.protonmail.android.mailevents.data.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.data.local.MailEventsDataSource
import ch.protonmail.android.mailevents.data.remote.EventsDataSource
import ch.protonmail.android.mailevents.data.remote.model.EventMetadata
import ch.protonmail.android.mailevents.data.remote.model.toPayload
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.AppInfoProvider
import ch.protonmail.android.mailevents.domain.repository.DeviceInfoProvider
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.IsNewAppInstall
import timber.log.Timber
import javax.inject.Inject

class EventsRepositoryImpl @Inject constructor(
    private val mailEventsDataSource: MailEventsDataSource,
    private val eventsDataSource: EventsDataSource,
    private val appInfoProvider: AppInfoProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val isNewAppInstall: IsNewAppInstall
) : EventsRepository {

    override suspend fun sendEvent(event: AppEvent): Either<DataError, Unit> {
        // Only send events if this is a new install or install event has been sent
        val shouldSendEvent = isNewAppInstall() || mailEventsDataSource.hasInstallEventBeenSent()

        if (!shouldSendEvent) {
            Timber.d("Skipping event submission: not eligible")
            return DataError.Local.Other("Not eligible for sending events").left()
        }

        return mailEventsDataSource.getOrCreateAsid().flatMap { asid ->
            val appInfo = appInfoProvider.getAppInfo()
            val metadata = EventMetadata(
                asid = asid,
                appPackageName = appInfo.packageName,
                appIdentifier = appInfo.packageName,
                appVersion = appInfo.version,
                deviceInfo = deviceInfoProvider.getDeviceInfo()
            )
            val payload = event.toPayload(metadata)
            eventsDataSource.sendEvent(payload)
        }
    }

    override suspend fun hasInstallEventBeenSent(): Boolean = mailEventsDataSource.hasInstallEventBeenSent()

    override suspend fun markInstallEventSent() {
        mailEventsDataSource.markInstallEventSent()
    }

    override suspend fun hasSentMessageEventBeenSent(): Boolean =
        mailEventsDataSource.hasFirstMessageSentEventBeenSent()

    override suspend fun markSentMessageEventSent() {
        mailEventsDataSource.markSentMessageEventSent()
    }

    override suspend fun hasSignupEventBeenSent(): Boolean = mailEventsDataSource.hasSignupEventBeenSent()

    override suspend fun markSignupEventSent() {
        mailEventsDataSource.markSignupEventSent()
    }

    override suspend fun getLastAppOpenTimestamp(): Long? = mailEventsDataSource.getLastAppOpenTimestamp()

    override suspend fun saveLastAppOpenTimestamp(timestampMs: Long): Either<DataError, Unit> =
        mailEventsDataSource.saveLastAppOpenTimestamp(timestampMs)
}
