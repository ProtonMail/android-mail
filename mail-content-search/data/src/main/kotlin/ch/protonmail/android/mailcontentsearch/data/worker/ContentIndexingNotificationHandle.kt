/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.worker

import android.app.NotificationManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Removes the indexing notification from outside the worker that posted it.
 *
 * WorkManager removes the notification when the worker holding the service ends. That fails when the
 * process dies first, or when Android has already taken the service away. The notification then stays
 * on screen with nothing left to remove it.
 *
 * Android ignores the cancel while a live foreground service still owns the notification. So any caller
 * can use this safely, and a held service is still only ever dropped by its worker.
 */
class ContentIndexingNotificationHandle @Inject constructor(
    private val notificationManager: NotificationManager
) {

    /** Cancels the notification unless a live service owns it. */
    fun remove(origin: DismissalOrigin) {
        Timber.d("content-search: cancelling the indexing notification on $origin")
        notificationManager.cancel(ContentIndexingNotification.NotificationId)
    }

    /** Who asked for the removal. Only used in the logs. */
    enum class DismissalOrigin {
        PauseTap,
        WorkerExit,
        AppForeground
    }
}
