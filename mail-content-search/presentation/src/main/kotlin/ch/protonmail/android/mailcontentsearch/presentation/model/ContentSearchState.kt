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

package ch.protonmail.android.mailcontentsearch.presentation.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter
import kotlinx.collections.immutable.persistentListOf

@Stable
data class ContentSearchState(
    val query: String,
    val phase: Phase,
    val includeFilter: IncludeFilter,
    val isConversationGrouping: Boolean,
    val openedFromLocation: LabelId,
    /**
     * All Mail, the one location that spans every folder. Used to open a previously found item that has
     * since been moved to Trash or Spam, which [openedFromLocation] may exclude — the history is not
     * scoped by the include filter, so it can offer items the current search would not return.
     */
    val allMailLocation: LabelId,
    val selectionState: SelectionState,
    val bottomBarState: BottomBarState,
    val bottomSheetState: BottomSheetState?,
    val showDeleteDialog: Boolean,
    val reloadResults: Effect<Unit>,
    val actionMessage: Effect<ActionResult>,
    val avatarImages: AvatarImagesUiModel,
    val downloadingAttachmentId: AttachmentIdUiModel?,
    val openAttachment: Effect<OpenAttachmentIntentValues>,
    val errorMessage: Effect<TextUiModel>
) {
    val inSelectionMode: Boolean get() = selectionState.inSelectionMode

    sealed interface Phase {
        data object Idle : Phase
        data object Loading : Phase
        data class Results(val count: Int) : Phase
    }

    companion object {

        val Initial = ContentSearchState(
            query = "",
            phase = Phase.Idle,
            includeFilter = IncludeFilter.None,
            isConversationGrouping = false,
            openedFromLocation = SystemLabelId.AlmostAllMail.labelId,
            allMailLocation = SystemLabelId.AllMail.labelId,
            selectionState = SelectionState.None,
            bottomBarState = BottomBarState.Data.Hidden(BottomBarTarget.Mailbox, persistentListOf()),
            bottomSheetState = null,
            showDeleteDialog = false,
            reloadResults = Effect.empty(),
            actionMessage = Effect.empty(),
            avatarImages = AvatarImagesUiModel.Empty,
            downloadingAttachmentId = null,
            openAttachment = Effect.empty(),
            errorMessage = Effect.empty()
        )
    }
}
