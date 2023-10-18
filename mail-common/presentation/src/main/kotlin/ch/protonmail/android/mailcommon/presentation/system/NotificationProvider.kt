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
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import ch.protonmail.android.mailcommon.presentation.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

typealias ChannelId = String

@Singleton
class NotificationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    fun initNotificationChannels() {
        // Attachments
        createNotificationChannel(
            context = context,
            channelId = ATTACHMENT_CHANNEL_ID,
            channelName = R.string.attachment_download_notification_channel_name,
            channelDescription = R.string.attachment_download_notification_channel_description
        )

        // Email
        createNotificationChannel(
            context = context,
            channelId = EMAIL_CHANNEL_ID,
            channelName = R.string.email_notification_channel_name,
            channelDescription = R.string.email_notification_channel_description
        )

        // New logins
        createNotificationChannel(
            context = context,
            channelId = LOGIN_CHANNEL_ID,
            channelName = R.string.login_notification_channel_name,
            channelDescription = R.string.login_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    fun provideNotificationChannel(channelId: ChannelId): NotificationChannel =
        notificationManager.getNotificationChannel(channelId)

    fun provideNotification(
        context: Context,
        channel: NotificationChannel,
        @StringRes title: Int
    ): Notification {
        return NotificationCompat.Builder(context, channel.id).apply {
            setContentTitle(context.getString(title))
            setSmallIcon(R.drawable.ic_proton_brand_proton_mail)
            setOngoing(true)
        }.build()
    }

    fun provideEmailNotificationBuilder(
        context: Context,
        contentTitle: String,
        subText: String,
        contentText: String,
        group: String,
        isGroupSummary: Boolean = false,
        autoCancel: Boolean = false
    ): NotificationCompat.Builder {
        val channel = provideNotificationChannel(EMAIL_CHANNEL_ID)
        return NotificationCompat.Builder(context, channel.id).apply {
            setContentTitle(contentTitle)
            setSmallIcon(R.drawable.ic_proton_brand_proton_mail)
            setSubText(subText)
            setContentText(contentText)
            setGroup(group)
            setGroupSummary(isGroupSummary)
            setAutoCancel(autoCancel)
        }
    }

    fun provideLoginNotificationBuilder(
        context: Context,
        userAddress: String,
        contentTitle: String,
        contentText: String,
        group: String,
        isGroupSummary: Boolean = false,
        autoCancel: Boolean = false
    ): NotificationCompat.Builder {
        val channel = provideNotificationChannel(LOGIN_CHANNEL_ID)
        val style = NotificationCompat.BigTextStyle().run {
            setSummaryText(userAddress)
            bigText(contentText)
        }
        return NotificationCompat.Builder(context, channel.id).apply {
            setStyle(style)
            setContentTitle(contentTitle)
            setSmallIcon(R.drawable.ic_proton_brand_proton_mail)
            setContentText(contentText)
            setGroup(group)
            setGroupSummary(isGroupSummary)
            setAutoCancel(autoCancel)
        }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: ChannelId,
        @StringRes channelName: Int,
        @StringRes channelDescription: Int,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        val channelNameString = context.getString(channelName)
        val channelDescriptionString = context.getString(channelDescription)
        val notificationChannel = NotificationChannel(channelId, channelNameString, importance).apply {
            description = channelDescriptionString
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    companion object {

        const val ATTACHMENT_CHANNEL_ID: ChannelId = "attachment_channel_id"
        const val EMAIL_CHANNEL_ID: ChannelId = "email_channel_id"
        const val LOGIN_CHANNEL_ID: ChannelId = "login_channel_id"
    }
}
