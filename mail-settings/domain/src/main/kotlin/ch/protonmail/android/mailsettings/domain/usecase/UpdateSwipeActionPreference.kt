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

import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.shouldSyncWithRemote
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class UpdateSwipeActionPreference @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        swipeActionDirection: SwipeActionDirection,
        swipeAction: SwipeAction
    ) {
        when (swipeActionDirection) {
            SwipeActionDirection.RIGHT ->
                mailSettingsRepository.updateSwipeRight(userId, swipeAction, swipeAction.shouldSyncWithRemote())
            SwipeActionDirection.LEFT ->
                mailSettingsRepository.updateSwipeLeft(userId, swipeAction, swipeAction.shouldSyncWithRemote())
        }.exhaustive
    }
}
