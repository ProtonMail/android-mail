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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

class ObserveSwipeActionsPreference @Inject constructor(
    private val observeMailSettings: ObserveMailSettings
) {

    operator fun invoke(userId: UserId): Flow<SwipeActionsPreference> =
        observeMailSettings(userId).transform { mailSettings ->
            if (mailSettings != null) {
                val preference = SwipeActionsPreference(
                    swipeLeft = mailSettings.swipeLeft?.enum ?: SwipeAction.None,
                    swipeRight = mailSettings.swipeRight?.enum ?: SwipeAction.None
                )
                emit(preference)
            }
        }
}
