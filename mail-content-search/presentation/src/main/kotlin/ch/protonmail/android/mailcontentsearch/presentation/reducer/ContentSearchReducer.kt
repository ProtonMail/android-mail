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

package ch.protonmail.android.mailcontentsearch.presentation.reducer

import ch.protonmail.android.mailattachments.presentation.model.AttachmentDownloadState
import ch.protonmail.android.mailattachments.presentation.reducer.AttachmentDownloadReducer
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchOperation
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import javax.inject.Inject

class ContentSearchReducer @Inject constructor(
    private val selectionReducer: SelectionStateReducer,
    private val attachmentDownloadReducer: AttachmentDownloadReducer,
    private val bulkActionMessageFactory: BulkActionMessageFactory
) {

    fun newStateFrom(currentState: ContentSearchState, operation: ContentSearchOperation): ContentSearchState =
        when (operation) {
            // Selection-set transitions are shared with the mailbox via SelectionStateReducer.
            is ContentSearchOperation.EnterSelectionMode ->
                currentState.withSelection(selectionReducer.enterSelection(operation.item))

            is ContentSearchOperation.ToggleSelection -> {
                val selection = currentState.selectionState
                when {
                    selection.selectedItems.any { it.id == operation.item.id } ->
                        currentState.withSelection(selectionReducer.removeFromSelection(selection, operation.item.id))

                    // Refusing silently would look like a dropped tap, so report the cap.
                    selection.isAtLimit ->
                        currentState.copy(
                            actionMessage = Effect.of(bulkActionMessageFactory.maxSelectionReachedResult())
                        )

                    else -> currentState.withSelection(selectionReducer.addToSelection(selection, operation.item))
                }
            }

            is ContentSearchOperation.ItemsRemovedFromSelection ->
                currentState.withSelection(
                    selectionReducer.removeFromSelection(currentState.selectionState, operation.itemIds)
                )

            ContentSearchOperation.MarkSelectionAsRead ->
                currentState.withSelection(selectionReducer.markRead(currentState.selectionState, isRead = true))

            ContentSearchOperation.MarkSelectionAsUnread ->
                currentState.withSelection(selectionReducer.markRead(currentState.selectionState, isRead = false))

            ContentSearchOperation.StarSelection ->
                currentState.withSelection(selectionReducer.markStarred(currentState.selectionState, isStarred = true))

            ContentSearchOperation.UnStarSelection ->
                currentState.withSelection(selectionReducer.markStarred(currentState.selectionState, isStarred = false))

            // Content-search-specific layers on top of the shared selection state.
            ContentSearchOperation.ExitSelectionMode ->
                currentState.copy(selectionState = SelectionState.None, showDeleteDialog = false)

            is ContentSearchOperation.BottomBarUpdated -> currentState.copy(bottomBarState = operation.bottomBarState)

            ContentSearchOperation.ShowDeleteDialog -> currentState.copy(showDeleteDialog = true)
            ContentSearchOperation.DismissDeleteDialog -> currentState.copy(showDeleteDialog = false)

            is ContentSearchOperation.ShowActionMessage ->
                currentState.copy(actionMessage = Effect.of(operation.actionResult))

            is ContentSearchOperation.ShowError -> currentState.copy(errorMessage = Effect.of(operation.message))

            ContentSearchOperation.ReloadResults -> currentState.copy(reloadResults = Effect.of(Unit))

            is ContentSearchOperation.AvatarImagesUpdated ->
                currentState.copy(avatarImages = operation.avatarImages)

            // Attachment downloads are driven by the shared AttachmentDownloadReducer, so the
            // one-download-at-a-time behaviour matches every other list that shows attachment pills.
            is ContentSearchOperation.AttachmentDownloadStarted -> currentState.mapAttachmentDownload {
                attachmentDownloadReducer.downloadStarted(it, operation.attachmentId)
            }

            ContentSearchOperation.AttachmentDownloadInProgress -> currentState.mapAttachmentDownload {
                attachmentDownloadReducer.downloadAlreadyInProgress(it)
            }

            is ContentSearchOperation.AttachmentReady -> currentState.mapAttachmentDownload {
                attachmentDownloadReducer.downloadReady(it, operation.openAttachmentIntentValues)
            }

            ContentSearchOperation.AttachmentDownloadFailed -> currentState.mapAttachmentDownload {
                attachmentDownloadReducer.downloadFailed(it)
            }

            ContentSearchOperation.AttachmentDownloadCancelled -> currentState.mapAttachmentDownload {
                attachmentDownloadReducer.downloadCancelled(it)
            }
        }

    private fun ContentSearchState.withSelection(selectionState: SelectionState) = copy(selectionState = selectionState)

}

// Adapters between this screen's flat attachment fields and the shared [AttachmentDownloadState].
private fun ContentSearchState.mapAttachmentDownload(
    transition: (AttachmentDownloadState) -> AttachmentDownloadState
): ContentSearchState = withAttachmentDownload(transition(attachmentDownload))

private val ContentSearchState.attachmentDownload: AttachmentDownloadState
    get() = AttachmentDownloadState(
        downloadingAttachmentId = downloadingAttachmentId,
        openAttachment = openAttachment,
        error = errorMessage
    )

private fun ContentSearchState.withAttachmentDownload(state: AttachmentDownloadState) = copy(
    downloadingAttachmentId = state.downloadingAttachmentId,
    openAttachment = state.openAttachment,
    errorMessage = state.error
)
