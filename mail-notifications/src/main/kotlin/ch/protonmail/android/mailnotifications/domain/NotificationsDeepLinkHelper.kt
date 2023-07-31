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

    fun buildMessageGroupDeepLinkIntent(notificationId: String): Intent

    fun cancelNotification(notificationId: Int)

    fun NotificationsDeepLinkHelper.buildMessageDeepLinkUri(
        notificationId: String,
        messageId: String,
        userId: String
    ): Uri = Uri.parse(
        DEEP_LINK_MESSAGE_TEMPLATE
            .replace("{messageId}", messageId)
            .replace("{userId}", userId)
            .replace("{notificationId}", notificationId)
    )

    fun NotificationsDeepLinkHelper.buildMessageGroupDeepLinkUri(notificationId: String): Uri = Uri.parse(
        DEEP_LINK_MESSAGE_GROUP_TEMPLATE
            .replace("{notificationId}", notificationId)
    )

    companion object {

        private const val DEEP_LINK_BASE_URI = "proton://notification/"
        private const val DEEP_LINK_MESSAGE_BASE = "${DEEP_LINK_BASE_URI}mailbox/message/"
        const val DEEP_LINK_MESSAGE_TEMPLATE = "$DEEP_LINK_MESSAGE_BASE{messageId}/{userId}/{notificationId}"
        private const val DEEP_LINK_MESSAGE_GROUP_BASE = "${DEEP_LINK_BASE_URI}mailbox/"
        const val DEEP_LINK_MESSAGE_GROUP_TEMPLATE = "$DEEP_LINK_MESSAGE_GROUP_BASE{notificationId}"
    }
}
