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

package ch.protonmail.android.mailcommon.presentation.system

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.protonmail.android.mailcommon.presentation.R

typealias ChannelId = String

interface NotificationProvider {

    fun provideNotificationChannel(
        context: Context,
        channelId: ChannelId,
        @StringRes channelName: Int,
        @StringRes channelDescription: Int,
        importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT
    ): NotificationChannel

    fun provideNotification(
        context: Context,
        channel: NotificationChannel,
        @StringRes title: Int
    ): Notification

    companion object {

        const val ATTACHMENT_CHANNEL_ID: ChannelId = "attachment_channel_id"
    }

}

class NotificationProviderImpl : NotificationProvider {

    override fun provideNotificationChannel(
        context: Context,
        channelId: ChannelId,
        @StringRes channelName: Int,
        @StringRes channelDescription: Int,
        importance: Int
    ): NotificationChannel {
        val channelNameString = context.getString(channelName)
        val channelDescriptionString = context.getString(channelDescription)
        return NotificationChannel(channelId, channelNameString, importance).apply {
            description = channelDescriptionString
        }
    }

    override fun provideNotification(
        context: Context,
        channel: NotificationChannel,
        @StringRes title: Int
    ): Notification {
        return NotificationCompat.Builder(context, channel.id).apply {
            setContentTitle(context.getString(title))
            setSmallIcon(R.drawable.ic_logo_mail_no_bg)
            setOngoing(true)
        }.build()
    }
}
