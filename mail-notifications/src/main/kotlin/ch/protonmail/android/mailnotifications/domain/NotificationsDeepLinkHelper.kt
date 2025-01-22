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

package ch.protonmail.android.mailnotifications.domain

import android.content.Intent
import android.net.Uri

interface NotificationsDeepLinkHelper {

    fun buildMessageDeepLinkIntent(
        notificationId: String,
        messageId: String,
        userId: String
    ): Intent

    fun buildMessageGroupDeepLinkIntent(notificationId: String, userId: String): Intent

    fun NotificationsDeepLinkHelper.buildMessageDeepLinkUri(
        notificationId: String,
        messageId: String,
        userId: String
    ): Uri = Uri.parse(
        DeepLinkMessageTemplate
            .replace("{messageId}", messageId)
            .replace("{userId}", userId)
            .replace("{notificationId}", notificationId)
    )

    fun NotificationsDeepLinkHelper.buildMessageGroupDeepLinkUri(notificationId: String, userId: String): Uri =
        Uri.parse(
            DeepLinkMessageGroupTemplate
                .replace("{notificationId}", notificationId)
                .replace("{userId}", userId)
        )

    companion object {

        const val NotificationHost = "notification"
        private const val DeepLinkBaseUri = "proton://$NotificationHost/"
        private const val DeepLinkMessageBase = "${DeepLinkBaseUri}mailbox/message/"
        const val DeepLinkMessageTemplate = "$DeepLinkMessageBase{messageId}/{userId}/{notificationId}"
        private const val DeepLinkMessageGroupBase = "${DeepLinkBaseUri}mailbox/"
        const val DeepLinkMessageGroupTemplate = "$DeepLinkMessageGroupBase{notificationId}/{userId}"
    }
}
