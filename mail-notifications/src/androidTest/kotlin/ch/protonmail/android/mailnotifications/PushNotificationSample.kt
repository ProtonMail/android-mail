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

package ch.protonmail.android.mailnotifications

import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationAction
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotification
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationData
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationSender

internal object PushNotificationSample {

    private val SampleNewMessagePushNotificationData = PushNotificationData(
        title = "ProtonMail",
        subtitle = "",
        body = "Notification",
        sender = PushNotificationSender("SenderEmail", "Sender", ""),
        vibrate = 1,
        sound = 1,
        largeIcon = "large_icon",
        smallIcon = "small_icon",
        badge = 1,
        messageId = "aMessageId",
        customId = "aCustomId",
        action = NotificationAction.CREATED
    )

    private val SampleMessageReadPushNotificationData = PushNotificationData(
        title = "ProtonMail",
        subtitle = "",
        body = "",
        sender = PushNotificationSender("SenderEmail", "Sender", ""),
        vibrate = 1,
        sound = 1,
        largeIcon = "large_icon",
        smallIcon = "small_icon",
        badge = 1,
        messageId = "messageId",
        customId = "",
        action = NotificationAction.TOUCHED
    )

    private val SampleLoginPushNotificationData = PushNotificationData(
        title = "ProtonMail",
        subtitle = "",
        body = "New login attempt",
        sender = PushNotificationSender("abuse@proton.me", "Proton Mail", ""),
        vibrate = 1,
        sound = 1,
        largeIcon = "large_icon",
        smallIcon = "small_icon",
        badge = 1,
        messageId = "",
        customId = "",
        action = NotificationAction.CREATED
    )

    fun getSampleLoginAlertNotification() =
        PushNotification(type = "open_url", version = 2, SampleLoginPushNotificationData)

    fun getSampleMessageReadNotification() =
        PushNotification(type = "email", version = 2, SampleMessageReadPushNotificationData)

    fun getSampleNewMessageNotification() =
        PushNotification(type = "email", version = 2, SampleNewMessagePushNotificationData)

    fun getSampleNotification(
        type: String,
        version: Int = 2,
        data: PushNotificationData = SampleNewMessagePushNotificationData
    ) = PushNotification(type, version, data)
}
