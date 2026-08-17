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

import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel

/**
 * Pure state transitions for content-search selection mode and the bottom action toolbar,
 * applied by [ch.protonmail.android.mailcontentsearch.presentation.reducer.ContentSearchReducer].
 * Search-driving transitions (query/phase) remain in the view model as they are paging-coupled.
 */
sealed interface ContentSearchOperation {

    data class EnterSelectionMode(val item: MailboxItemUiModel) : ContentSearchOperation

    data class ToggleSelection(val item: MailboxItemUiModel) : ContentSearchOperation

    data object ExitSelectionMode : ContentSearchOperation

    data class ItemsRemovedFromSelection(val itemIds: List<String>) : ContentSearchOperation

    data object MarkSelectionAsRead : ContentSearchOperation

    data object MarkSelectionAsUnread : ContentSearchOperation

    data object StarSelection : ContentSearchOperation

    data object UnStarSelection : ContentSearchOperation

    data class BottomBarUpdated(val bottomBarState: BottomBarState) : ContentSearchOperation

    data object ShowDeleteDialog : ContentSearchOperation

    data object DismissDeleteDialog : ContentSearchOperation

    data class ShowActionMessage(val actionResult: ActionResult) : ContentSearchOperation

    data class ShowError(val message: TextUiModel) : ContentSearchOperation

    data object ReloadResults : ContentSearchOperation

    data class AvatarImagesUpdated(val avatarImages: AvatarImagesUiModel) : ContentSearchOperation

    data class AttachmentDownloadStarted(val attachmentId: AttachmentIdUiModel) : ContentSearchOperation

    data object AttachmentDownloadInProgress : ContentSearchOperation

    data class AttachmentReady(val openAttachmentIntentValues: OpenAttachmentIntentValues) : ContentSearchOperation

    data object AttachmentDownloadFailed : ContentSearchOperation

    data object AttachmentDownloadCancelled : ContentSearchOperation
}
