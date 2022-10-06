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
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailState

object ConversationDetailsPreviewData {

    val Conversation = ConversationDetailState.Data(
        conversationUiModel = ConversationDetailsUiModelPreviewData.WeatherForecast
    )

    val FailedLoadingData = ConversationDetailState.Error.FailedLoadingData

    val Loading = ConversationDetailState.Loading

    val NotLoggedIn = ConversationDetailState.Error.NotLoggedIn
}

class ConversationDetailsPreviewProvider : PreviewParameterProvider<ConversationDetailState> {

    override val values = sequenceOf(
        ConversationDetailsPreviewData.Conversation,
        ConversationDetailsPreviewData.FailedLoadingData,
        ConversationDetailsPreviewData.Loading,
        ConversationDetailsPreviewData.NotLoggedIn
    )
}
