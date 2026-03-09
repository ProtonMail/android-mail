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

import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationActionType
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData.MessagePushData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationSenderData
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import uniffi.mail_uniffi.DecryptedEmailPushNotification
import uniffi.mail_uniffi.DecryptedEmailPushNotificationAction
import uniffi.mail_uniffi.DecryptedOpenUrlPushNotification
import uniffi.mail_uniffi.NotificationSender
import uniffi.mail_uniffi.RemoteId

internal object NotificationTestData {

    val userId = UserId("user-id")
    val sessionId = SessionId("session-id")
    const val email = "test@proton.me"
    const val url = "https://proton.me"

    const val messageId = "messageId"
    const val content = "content"
    private val unknownAction = NotificationActionType.Unknown("some_action")

    val defaultUserPushData = LocalPushNotificationData.UserPushData(userId, email)
    val defaultMessagePushData = MessagePushData.NewMessagePushData(
        PushNotificationSenderData(SenderData.name, SenderData.address, SenderData.group),
        messageId,
        content
    )
    val defaultMessageReadPushData = MessagePushData.MessageReadPushData(messageId)
    val defaultMessageUnexpectedData = MessagePushData.UnsupportedActionPushData(unknownAction)
    val defaultOpenUrlPushData = LocalPushNotificationData.NewLoginPushData(
        PushNotificationSenderData(SenderData.name, SenderData.address, SenderData.group),
        content,
        url
    )

    val decryptedEmailPushNotification = DecryptedEmailPushNotification(
        subject = content,
        sender = NotificationSender(name = SenderData.name, address = SenderData.address, group = SenderData.group),
        messageId = RemoteId(messageId),
        action = DecryptedEmailPushNotificationAction.MessageCreated
    )
    val decryptedMessageReadPushNotification = DecryptedEmailPushNotification(
        subject = "",
        sender = NotificationSender(name = "", address = "", group = ""),
        messageId = RemoteId(messageId),
        action = DecryptedEmailPushNotificationAction.MessageTouched
    )
    val decryptedMessageUnknownPushNotification = decryptedEmailPushNotification.copy(
        action = DecryptedEmailPushNotificationAction.Unexpected(unknownAction.action)
    )

    val decryptedOpenUrlPushNotification = DecryptedOpenUrlPushNotification(
        sender = NotificationSender(name = SenderData.name, address = SenderData.address, group = SenderData.group),
        url = url,
        content = content
    )

    val defaultNewMessageNotification = LocalPushNotification.Message.NewMessage(
        userData = defaultUserPushData,
        pushData = defaultMessagePushData
    )

    private object SenderData {

        const val name = "name"
        const val address = "address"
        const val group = "group"
    }
}
