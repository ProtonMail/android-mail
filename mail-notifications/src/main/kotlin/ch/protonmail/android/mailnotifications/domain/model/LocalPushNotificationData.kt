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

package ch.protonmail.android.mailnotifications.domain.model

internal sealed class LocalPushNotificationData {

    data class MessageRead(
        val userData: UserPushData,
        val pushData: MessageReadPushData
    )

    data class NewMessage(
        val userData: UserPushData,
        val pushData: NewMessagePushData
    ) : LocalPushNotificationData()

    data class Login(
        val userData: UserPushData,
        val pushData: NewLoginPushData
    ) : LocalPushNotificationData()
}

internal data class UserPushData(
    val userId: String,
    val userEmail: String
)

internal data class NewMessagePushData(
    val sender: String,
    val messageId: String,
    val content: String
)

internal data class NewLoginPushData(
    val sender: String,
    val content: String,
    val url: String
)

internal data class MessageReadPushData(
    val messageId: String
)
