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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailmessage.domain.model.MessageId

data class ConversationDetailState(
    val conversationState: ConversationDetailMetadataState,
    val messagesState: ConversationDetailsMessagesState,
    val bottomBarState: BottomBarState,
    val bottomSheetState: BottomSheetState?,
    val exitScreenEffect: Effect<Unit>,
    val exitScreenWithMessageEffect: Effect<TextUiModel>,
    val error: Effect<TextUiModel>,
    val openMessageBodyLinkEffect: Effect<String>,
    val openAttachmentEffect: Effect<OpenAttachmentIntentValues>,
    val scrollToMessage: MessageId?,
    val showReplyActionsFeatureFlag: Boolean
) {

    companion object {

        val Loading = ConversationDetailState(
            conversationState = ConversationDetailMetadataState.Loading,
            messagesState = ConversationDetailsMessagesState.Loading,
            bottomBarState = BottomBarState.Loading,
            bottomSheetState = null,
            exitScreenEffect = Effect.empty(),
            exitScreenWithMessageEffect = Effect.empty(),
            error = Effect.empty(),
            openMessageBodyLinkEffect = Effect.empty(),
            openAttachmentEffect = Effect.empty(),
            scrollToMessage = null,
            showReplyActionsFeatureFlag = false
        )
    }
}
