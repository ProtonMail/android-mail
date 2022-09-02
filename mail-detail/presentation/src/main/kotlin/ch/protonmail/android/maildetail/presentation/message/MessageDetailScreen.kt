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

package ch.protonmail.android.maildetail.presentation.message

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailState
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.util.kotlin.exhaustive

@Composable
fun MessageDetailScreen(
    messageId: MessageId,
    modifier: Modifier = Modifier,
    viewModel: MessageDetailViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = MessageDetailState.Loading
        ).value
    ) {
        is MessageDetailState.Data -> {
            Text(
                modifier = modifier,
                text = "Message detail for message ID: ${state.messageUiModel.messageId.id}"
            )
        }
        MessageDetailState.Error.NoMessageIdProvided -> throw IllegalStateException("No message id given")
        MessageDetailState.Error.NotLoggedIn -> Text(modifier = modifier, text = "No user logged in")
        MessageDetailState.Loading -> ProtonCenteredProgress()
    }.exhaustive
}

object MessageDetailScreen {

    const val MESSAGE_ID_KEY = "message id"
}
