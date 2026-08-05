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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomBarActions
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Resolves the bottom action toolbar state for a given selection by computing the context-appropriate
 * actions ([GetBottomBarActions]) and mapping them to UI models. Shared so any selection-capable
 * screen (mailbox, content search) builds the toolbar identically.
 *
 * Returns [BottomBarState.Data.Hidden] rather than [BottomBarState.Data.Shown] for an empty action list,
 * matching [ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer]'s own empty-collapse —
 * otherwise a caller that doesn't separately guard against it would show an empty floating toolbar.
 */
class BottomBarStateFactory @Inject constructor(
    private val getBottomBarActions: GetBottomBarActions,
    private val actionUiModelMapper: ActionUiModelMapper
) {

    suspend fun create(
        userId: UserId,
        labelId: LabelId,
        itemIds: List<MailboxItemId>,
        viewMode: ViewMode
    ): BottomBarState = getBottomBarActions(userId, labelId, itemIds, viewMode).fold(
        ifLeft = { BottomBarState.Error.FailedLoadingActions },
        ifRight = { actions ->
            val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }.toImmutableList()
            if (actionUiModels.isEmpty()) {
                BottomBarState.Data.Hidden(target = BottomBarTarget.Mailbox, actions = actionUiModels)
            } else {
                BottomBarState.Data.Shown(target = BottomBarTarget.Mailbox, actions = actionUiModels)
            }
        }
    )
}
