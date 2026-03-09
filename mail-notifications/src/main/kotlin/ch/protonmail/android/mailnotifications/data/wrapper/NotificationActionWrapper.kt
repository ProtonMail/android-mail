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

package ch.protonmail.android.mailnotifications.data.wrapper

import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationActionType
import uniffi.mail_uniffi.DecryptedEmailPushNotificationAction

internal data class NotificationActionWrapper(private val pushAction: DecryptedEmailPushNotificationAction?) {

    val value = when (pushAction) {
        DecryptedEmailPushNotificationAction.MessageCreated -> NotificationActionType.Created
        DecryptedEmailPushNotificationAction.MessageTouched -> NotificationActionType.Touched
        is DecryptedEmailPushNotificationAction.Unexpected -> NotificationActionType.Unknown(pushAction.v1)
        null -> null // Null on iOS, should never happen on Android side.
    }
}
