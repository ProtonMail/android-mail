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

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomSheetActions
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Builds the "more actions" bottom-sheet payload for a selection: resolves the context-appropriate
 * actions ([GetBottomSheetActions]) and maps them to UI models. Shared so any selection-capable screen
 * (mailbox, content search) fills the sheet identically.
 */
class MoreActionsSheetStateFactory @Inject constructor(
    private val getBottomSheetActions: GetBottomSheetActions,
    private val actionUiModelMapper: ActionUiModelMapper
) {

    /**
     * The sheet reports the size of [itemIds] as its selected count, so callers do not pass it
     * separately — the count and the items acted on cannot then disagree.
     */
    suspend fun create(
        userId: UserId,
        labelId: LabelId,
        itemIds: List<MailboxItemId>,
        viewMode: ViewMode
    ): Either<DataError, MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData> =
        getBottomSheetActions(userId, labelId, itemIds, viewMode).map { actions ->
            MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                hiddenActionUiModels = actions.hiddenActions
                    .map { actionUiModelMapper.toUiModel(it) }
                    .toImmutableList(),
                visibleActionUiModels = actions.visibleActions
                    .map { actionUiModelMapper.toUiModel(it) }
                    .toImmutableList(),
                customizeToolbarActionUiModel = actionUiModelMapper.toUiModel(Action.CustomizeToolbar),
                selectedCount = itemIds.size
            )
        }
}
