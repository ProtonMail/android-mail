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

package ch.protonmail.android.maildetail.presentation.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues

@Stable
data class ConversationDetailState(
    val conversationState: ConversationDetailMetadataState,
    val messagesState: ConversationDetailsMessagesState,
    val bottomBarState: BottomBarState,
    val bottomSheetState: BottomSheetState?,
    val exitScreenEffect: Effect<Unit>,
    val exitScreenActionResult: Effect<ActionResult>,
    val error: Effect<TextUiModel>,
    val actionResult: Effect<ActionResult>,
    val loadingErrorEffect: Effect<TextUiModel>,
    val openMessageBodyLinkEffect: Effect<MessageBodyLink>,
    val openAttachmentEffect: Effect<OpenAttachmentIntentValues>,
    val downloadingAttachmentId: AttachmentId? = null,
    val openProtonCalendarIntent: Effect<OpenProtonCalendarIntentValues>,
    val onExitWithNavigateToComposer: Effect<MessageIdUiModel>,
    val scrollToMessageState: ScrollToMessageState,
    val requestLinkConfirmation: Boolean,
    val conversationDeleteState: ConversationDeleteState,
    val reportPhishingDialogState: ReportPhishingDialogState,
    val hiddenMessagesBannerState: HiddenMessagesBannerState,
    val markAsLegitimateDialogState: MarkAsLegitimateDialogState,
    val editScheduledMessageDialogState: EditScheduledMessageDialogState,
    val blockSenderDialogState: BlockSenderDialogState
) {

    companion object {

        val Loading = ConversationDetailState(
            conversationState = ConversationDetailMetadataState.Loading,
            messagesState = ConversationDetailsMessagesState.Loading,
            bottomBarState = BottomBarState.Loading,
            bottomSheetState = null,
            exitScreenEffect = Effect.empty(),
            exitScreenActionResult = Effect.empty(),
            error = Effect.empty(),
            actionResult = Effect.empty(),
            loadingErrorEffect = Effect.empty(),
            openMessageBodyLinkEffect = Effect.empty(),
            openAttachmentEffect = Effect.empty(),
            downloadingAttachmentId = null,
            openProtonCalendarIntent = Effect.empty(),
            onExitWithNavigateToComposer = Effect.empty(),
            scrollToMessageState = ScrollToMessageState.NoScrollTarget,
            requestLinkConfirmation = false,
            conversationDeleteState = ConversationDeleteState.Hidden,
            reportPhishingDialogState = ReportPhishingDialogState.Hidden,
            hiddenMessagesBannerState = HiddenMessagesBannerState.Hidden,
            markAsLegitimateDialogState = MarkAsLegitimateDialogState.Hidden,
            editScheduledMessageDialogState = EditScheduledMessageDialogState.Hidden,
            blockSenderDialogState = BlockSenderDialogState.Hidden
        )
    }
}
