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

package ch.protonmail.android.maildetail.presentation.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.contentDescription
import ch.protonmail.android.mailcommon.presentation.model.iconDrawable
import ch.protonmail.android.mailcommon.presentation.sample.TextMessageSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample

object ConversationDetailsPreviewData {

    val Success = ConversationDetailState(
        ConversationDetailMetadataState.Data(
            conversationUiModel = ConversationDetailsUiModelPreviewData.WeatherForecast
        ),
        messagesState = ConversationDetailsMessagesState.Data(
            messages = listOf(
                ConversationDetailMessageUiModelSample.AugWeatherForecast,
                ConversationDetailMessageUiModelSample.InvoiceRepliedAll,
                ConversationDetailMessageUiModelSample.InvoiceForwarded,
                ConversationDetailMessageUiModelSample.ExpiringInvitation
            )
        ),
        bottomBarState = BottomBarState.Data(
            actions = listOf(
                ActionUiModel(Action.Reply, Action.Reply.iconDrawable(), Action.Reply.contentDescription()),
                ActionUiModel(Action.Archive, Action.Archive.iconDrawable(), Action.Archive.contentDescription())
            )
        ),
        bottomSheetContentState = MoveToBottomSheetState.Data(
            moveToDestinations = emptyList(),
            selected = null
        ),
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetEffect = Effect.empty()
    )

    val FailedLoadingConversation = ConversationDetailState(
        conversationState = ConversationDetailMetadataState.Error(TextMessageSample.UnknownError),
        messagesState = ConversationDetailsMessagesState.Loading,
        bottomBarState = BottomBarState.Loading,
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetContentState = MoveToBottomSheetState.Loading,
        bottomSheetEffect = Effect.empty()
    )

    val FailedLoadingMessages = ConversationDetailState(
        conversationState = ConversationDetailMetadataState.Loading,
        messagesState = ConversationDetailsMessagesState.Error(TextMessageSample.NoNetwork),
        bottomBarState = BottomBarState.Loading,
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetContentState = MoveToBottomSheetState.Loading,
        bottomSheetEffect = Effect.empty()
    )

    val FailedLoadingBottomBar = ConversationDetailState(
        conversationState = ConversationDetailMetadataState.Loading,
        messagesState = ConversationDetailsMessagesState.Loading,
        bottomBarState = BottomBarState.Error.FailedLoadingActions,
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetContentState = MoveToBottomSheetState.Loading,
        bottomSheetEffect = Effect.empty()
    )

    val Loading = ConversationDetailState(
        conversationState = ConversationDetailMetadataState.Loading,
        messagesState = ConversationDetailsMessagesState.Loading,
        bottomBarState = BottomBarState.Loading,
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetContentState = MoveToBottomSheetState.Loading,
        bottomSheetEffect = Effect.empty()
    )

    val NotLoggedIn = ConversationDetailState(
        conversationState = ConversationDetailMetadataState.Error(TextMessageSample.NotLoggedIn),
        messagesState = ConversationDetailsMessagesState.Error(TextMessageSample.NotLoggedIn),
        bottomBarState = BottomBarState.Loading,
        exitScreenEffect = Effect.empty(),
        exitScreenWithMessageEffect = Effect.empty(),
        error = Effect.empty(),
        bottomSheetContentState = MoveToBottomSheetState.Loading,
        bottomSheetEffect = Effect.empty()
    )
}

class ConversationDetailsPreviewProvider : PreviewParameterProvider<ConversationDetailState> {

    override val values = sequenceOf(
        ConversationDetailsPreviewData.Success,
        ConversationDetailsPreviewData.FailedLoadingConversation,
        ConversationDetailsPreviewData.FailedLoadingMessages,
        ConversationDetailsPreviewData.FailedLoadingBottomBar,
        ConversationDetailsPreviewData.Loading,
        ConversationDetailsPreviewData.NotLoggedIn
    )
}
