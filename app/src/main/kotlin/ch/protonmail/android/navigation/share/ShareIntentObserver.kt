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

package ch.protonmail.android.navigation.share

import android.content.Intent
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareIntentObserver @Inject constructor() {

    private val _intentFlow = MutableStateFlow<Intent?>(null)
    private val intentFlow: StateFlow<Intent?> = _intentFlow

    operator fun invoke(): Flow<Intent> {
        return intentFlow
            .filterNotNull()
            .filter { intent ->
                !intent.isNotificationIntent() && intent.action in setOf(
                    Intent.ACTION_SEND,
                    Intent.ACTION_SEND_MULTIPLE,
                    Intent.ACTION_VIEW,
                    Intent.ACTION_SENDTO,
                    Intent.ACTION_MAIN
                )
            }
            .distinctUntilChanged()
    }

    fun onNewIntent(intent: Intent?) {
        _intentFlow.value = intent
    }

    private fun Intent.isNotificationIntent(): Boolean = data?.host == NotificationsDeepLinkHelper.NotificationHost
}
