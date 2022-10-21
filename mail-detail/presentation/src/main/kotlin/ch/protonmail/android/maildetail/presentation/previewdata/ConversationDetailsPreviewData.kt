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
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.contentDescription
import ch.protonmail.android.mailcommon.presentation.model.iconDrawable
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationState

object ConversationDetailsPreviewData {

    val Conversation = ConversationDetailState(
        ConversationState.Data(
            conversationUiModel = ConversationDetailsUiModelPreviewData.WeatherForecast
        ),
        bottomBarState = BottomBarState.Data(
            listOf(
                ActionUiModel(Action.Reply, Action.Reply.iconDrawable(), Action.Reply.contentDescription()),
                ActionUiModel(Action.Archive, Action.Archive.iconDrawable(), Action.Archive.contentDescription())
            )
        )
    )

    val FailedLoadingData = ConversationDetailState(
        conversationState = ConversationState.Error.FailedLoadingData,
        bottomBarState = BottomBarState.Error.FailedLoadingActions
    )

    val Loading = ConversationDetailState(
        conversationState = ConversationState.Loading,
        bottomBarState = BottomBarState.Loading
    )

    val NotLoggedIn = ConversationDetailState(
        conversationState = ConversationState.Error.NotLoggedIn,
        bottomBarState = BottomBarState.Loading
    )
}

class ConversationDetailsPreviewProvider : PreviewParameterProvider<ConversationDetailState> {

    override val values = sequenceOf(
        ConversationDetailsPreviewData.Conversation,
        ConversationDetailsPreviewData.FailedLoadingData,
        ConversationDetailsPreviewData.Loading,
        ConversationDetailsPreviewData.NotLoggedIn
    )
}
