/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.presentation.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers "Not now" on the storage banner for each account until the app is restarted. When the banner should
 * show again is not decided yet, so nothing is stored.
 */
@Singleton
class ByoeStorageBannerDismissal @Inject constructor() {

    private val dismissedUserIds = MutableStateFlow<Set<UserId>>(emptySet())

    fun isDismissed(userId: UserId): Flow<Boolean> = dismissedUserIds.map { userId in it }.distinctUntilChanged()

    fun dismiss(userId: UserId) {
        dismissedUserIds.update { it + userId }
    }
}
