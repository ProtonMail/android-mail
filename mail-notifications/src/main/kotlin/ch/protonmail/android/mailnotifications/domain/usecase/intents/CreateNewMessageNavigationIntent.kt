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

package ch.protonmail.android.mailnotifications.domain.usecase.intents

import android.app.PendingIntent
import android.content.Context
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class CreateNewMessageNavigationIntent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationsDeepLinkHelper: NotificationsDeepLinkHelper
) {

    /**
     * Creates a [PendingIntent] that navigates to the given messageId.
     */
    operator fun invoke(
        notificationId: Int,
        messageId: String,
        userId: String
    ): PendingIntent {

        return PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            notificationsDeepLinkHelper.buildMessageDeepLinkIntent(
                notificationId.toString(),
                messageId,
                userId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Creates a [PendingIntent] that navigates to the given userId's Inbox.
     */
    operator fun invoke(notificationId: Int, userId: String): PendingIntent {

        return PendingIntent.getActivity(
            context,
            userId.hashCode(),
            notificationsDeepLinkHelper.buildMessageGroupDeepLinkIntent(
                notificationId.toString(),
                userId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
