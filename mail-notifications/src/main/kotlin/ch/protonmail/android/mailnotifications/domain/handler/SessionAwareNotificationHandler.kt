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

package ch.protonmail.android.mailnotifications.domain.handler

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.usecase.DismissEmailNotificationsForUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class SessionAwareNotificationHandler @Inject constructor(
    private val appInBackgroundState: AppInBackgroundState,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val dismissEmailNotificationsForUser: DismissEmailNotificationsForUser,
    @AppScope private val coroutineScope: CoroutineScope
) : NotificationHandler {

    override fun handle() {
        coroutineScope.launch {
            combine(
                observePrimaryUserId(),
                appInBackgroundState.observe()
            ) { userId, isInBackground ->
                UserAppState(userId, isInBackground)
            }.collectLatest {
                if (it.userId == null || it.isInBackground) return@collectLatest

                // Delaying is needed to prevent multiple notification groups from being dismissed when the app
                // is brought to the foreground AND the userId has changed due to a forced account switch.
                // Delaying makes the block cancellable and keeps the previous userId's notifications in place.
                delay(DISMISSAL_DELAY)
                dismissEmailNotificationsForUser(it.userId)
            }
        }
    }

    private data class UserAppState(val userId: UserId?, val isInBackground: Boolean)

    companion object {

        const val DISMISSAL_DELAY = 2000L
    }
}
