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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushNotificationData(
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String,
    @SerialName("body") val body: String,
    @SerialName("vibrate") val vibrate: Int,
    @SerialName("sound") val sound: Int,
    @SerialName("largeIcon") val largeIcon: String,
    @SerialName("smallIcon") val smallIcon: String,
    @SerialName("badge") val badge: Int,
    @SerialName("messageId") val messageId: String,
    @SerialName("customId") val customId: String,
    @SerialName("sender") val sender: PushNotificationSender?,
    @SerialName("url") val url: String = "",
    @SerialName("action") val action: NotificationAction
)
