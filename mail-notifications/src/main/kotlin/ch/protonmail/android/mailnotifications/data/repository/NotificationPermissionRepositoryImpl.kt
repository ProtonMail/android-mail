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

package ch.protonmail.android.mailnotifications.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.local.NotificationPermissionLocalDataSource
import javax.inject.Inject

class NotificationPermissionRepositoryImpl @Inject constructor(
    private val notificationPermissionLocalDataSource: NotificationPermissionLocalDataSource
) : NotificationPermissionRepository {

    override suspend fun getNotificationPermissionTimestamp(): Either<DataError.Local, Long> =
        notificationPermissionLocalDataSource.getNotificationPermissionTimestamp()

    override suspend fun saveNotificationPermissionTimestamp(timestamp: Long) =
        notificationPermissionLocalDataSource.saveNotificationPermissionTimestamp(timestamp)

    override suspend fun getShouldStopShowingPermissionDialog(): Either<DataError.Local, Boolean> =
        notificationPermissionLocalDataSource.getShouldStopShowingPermissionDialog()

    override suspend fun saveShouldStopShowingPermissionDialog(shouldStopShowingPermissionDialog: Boolean) =
        notificationPermissionLocalDataSource.saveShouldStopShowingPermissionDialog(
            shouldStopShowingPermissionDialog
        )
}
