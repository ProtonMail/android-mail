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

package ch.protonmail.android.maillabel.data.repository

import ch.protonmail.android.mailcommon.data.mapper.LocalViewMode
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.repository.ViewModeRepository
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ViewMode.CONVERSATIONS
import uniffi.mail_uniffi.ViewMode.MESSAGES
import javax.inject.Inject

class ViewModeRepositoryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory
) : ViewModeRepository {

    override suspend fun getViewModeForLabel(userId: UserId, labelId: LabelId): ViewMode? =
        rustMailboxFactory.create(userId, labelId.toLocalLabelId())
            .getOrNull()
            ?.viewMode()
            ?.toViewMode()


    private fun LocalViewMode.toViewMode() = when (this) {
        CONVERSATIONS -> ViewMode.ConversationGrouping
        MESSAGES -> ViewMode.NoConversationGrouping
    }
}
