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

import uniffi.mail_uniffi.DecryptedEmailPushNotification
import uniffi.mail_uniffi.DecryptedOpenUrlPushNotification
import uniffi.mail_uniffi.DecryptedPushNotification

internal sealed class DecryptedPushNotificationWrapper {
    data class Email(
        private val decryptedNotification: DecryptedEmailPushNotification
    ) : DecryptedPushNotificationWrapper() {

        val sender = NotificationSenderWrapper(decryptedNotification.sender)
        val subject = NotificationContentWrapper(decryptedNotification.subject)
        val messageId = NotificationMessageIdWrapper(decryptedNotification.messageId)
        val action = NotificationActionWrapper(decryptedNotification.action)
    }

    data class OpenUrl(
        private val decryptedNotification: DecryptedOpenUrlPushNotification
    ) : DecryptedPushNotificationWrapper() {

        val sender = NotificationSenderWrapper(decryptedNotification.sender)
        val url = NotificationContentWrapper(decryptedNotification.url)
        val content = NotificationContentWrapper(decryptedNotification.content)
    }

    companion object {

        operator fun invoke(decryptPushNotification: DecryptedPushNotification) = when (decryptPushNotification) {
            is DecryptedPushNotification.Email -> Email(decryptPushNotification.v1)
            is DecryptedPushNotification.OpenUrl -> OpenUrl(decryptPushNotification.v1)
        }
    }
}
