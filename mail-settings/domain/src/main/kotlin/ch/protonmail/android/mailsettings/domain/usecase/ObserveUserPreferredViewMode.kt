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

package ch.protonmail.android.mailsettings.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ch.protonmail.android.maillabel.domain.model.ViewMode
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode as CoreViewMode

class ObserveUserPreferredViewMode @Inject constructor(
    private val observeMailSettings: ObserveMailSettings
) {

    operator fun invoke(userId: UserId): Flow<ViewMode> = observeMailSettings(userId)
        .filterNotNull()
        .map { settings ->
            when (settings.viewMode?.enum) {
                CoreViewMode.ConversationGrouping -> ViewMode.ConversationGrouping
                CoreViewMode.NoConversationGrouping -> ViewMode.NoConversationGrouping
                null -> DefaultViewMode
            }
        }
        .distinctUntilChanged()

    companion object {

        val DefaultViewMode = ViewMode.NoConversationGrouping
    }
}
