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
package ch.protonmail.android.navigation.mapper

import android.content.Intent
import ch.protonmail.android.mailcommon.data.file.getShareInfo
import ch.protonmail.android.mailcommon.data.file.isMailToIntent
import ch.protonmail.android.mailcommon.domain.model.isNotEmpty
import ch.protonmail.android.navigation.model.HomeNavigationEvent
import javax.inject.Inject

class IntentMapper @Inject constructor() {

    fun map(intent: Intent): HomeNavigationEvent {
        return when {
            intent.isLauncherIntent() -> {
                HomeNavigationEvent.LauncherIntentReceived(intent)
            }

            intent.isShareIntent() -> {
                if (intent.isMailToIntent()) {
                    return HomeNavigationEvent.MailToIntentReceived(intent)
                }

                val shareInfo = intent.getShareInfo()
                    .takeIf { it.isNotEmpty() }
                    ?: return HomeNavigationEvent.InvalidShareIntentReceived(intent)

                if (shareInfo.isExternal) {
                    HomeNavigationEvent.ExternalShareIntentReceived(
                        intent = intent,
                        shareInfo = shareInfo
                    )
                } else {
                    HomeNavigationEvent.InternalShareIntentReceived(
                        intent = intent,
                        shareInfo = shareInfo
                    )
                }
            }

            else -> {
                HomeNavigationEvent.UnknownIntentReceived(intent)
            }
        }
    }

    private fun Intent.isLauncherIntent(): Boolean = action == Intent.ACTION_MAIN &&
        categories?.contains(Intent.CATEGORY_LAUNCHER) == true ||
        categories?.contains(Intent.CATEGORY_DEFAULT) == true

    private fun Intent.isShareIntent(): Boolean = action == Intent.ACTION_SEND ||
        action == Intent.ACTION_SEND_MULTIPLE ||
        action == Intent.ACTION_VIEW ||
        action == Intent.ACTION_SENDTO
}
