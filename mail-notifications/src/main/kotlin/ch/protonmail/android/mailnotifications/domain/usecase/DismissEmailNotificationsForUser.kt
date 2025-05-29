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

package ch.protonmail.android.mailnotifications.domain.usecase

import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class DismissEmailNotificationsForUser @Inject constructor(
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy
) {

    operator fun invoke(
        userId: UserId,
        notificationId: Int,
        isSilentNotification: Boolean = false
    ) {
        val shouldDismissGroup = shouldDismissGroupNotification(
            userId = userId,
            notificationId = notificationId,
            isSilentNotification = isSilentNotification
        )

        if (shouldDismissGroup) {
            notificationManagerCompatProxy.dismissNotification(userId.id.hashCode())
        } else {
            notificationManagerCompatProxy.dismissNotification(notificationId)
        }
    }

    operator fun invoke(userId: UserId) {
        notificationManagerCompatProxy.dismissNotification(userId.id.hashCode())
    }

    private fun shouldDismissGroupNotification(userId: UserId) =
        notificationManagerCompatProxy.activeNotifications.count { userId.id in it.groupKey } <= DISMISSAL_THRESHOLD

    private fun shouldDismissGroupNotification(
        userId: UserId,
        notificationId: Int,
        isSilentNotification: Boolean
    ): Boolean {
        val isSingleRegularNotification = !isSilentNotification &&
            shouldDismissGroupNotification(userId)

        val isSingleSilentNotification = isSilentNotification &&
            notificationManagerCompatProxy.activeNotifications.any { it.id == notificationId } &&
            shouldDismissGroupNotification(userId)

        return isSingleRegularNotification || isSingleSilentNotification
    }

    private companion object {

        const val DISMISSAL_THRESHOLD = 2 // It's 2 since we always show group + child notification.
    }
}
