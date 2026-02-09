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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserPreferredViewMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId

class ObserveIsSingleMessageViewModePreferred @Inject constructor(
    private val observeUserPreferredViewMode: ObserveUserPreferredViewMode
) {

    operator fun invoke(userId: UserId): Flow<Boolean> = observeUserPreferredViewMode(userId)
        .map { preferredViewMode -> preferredViewMode == ViewMode.NoConversationGrouping }
        .distinctUntilChanged()
}
