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

package ch.protonmail.android.testdata.notifications

import ch.protonmail.android.mailnotifications.domain.model.NotificationAction
import ch.protonmail.android.mailnotifications.domain.model.PushNotification
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationSender

object PushNotificationSample {

    private val SampleEmailPushNotificationData = PushNotificationData(
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

    fun getSampleNotification(
        type: String,
        version: Int = 2,
        data: PushNotificationData = SampleEmailPushNotificationData
    ) = PushNotification(type, version, data)
}
