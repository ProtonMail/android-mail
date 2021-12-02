/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailconversation.domain.Conversation
import ch.protonmail.android.mailconversation.domain.ConversationId

@Composable
fun MailboxScreen(
    navigateToConversation: (ConversationId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        val viewModel = hiltViewModel<MailboxViewModel>()
        val viewState = remember { viewModel.viewState }
            .collectAsState(initial = MailboxViewState.initialState)

        LazyColumn {
            itemsIndexed(viewState.value.mailboxItems) { _, item ->
                MailboxItem(item) { conversationId ->
                    navigateToConversation(conversationId)
                }
            }
        }
    }
}

@Composable
private fun MailboxItem(
    item: Conversation,
    onMessageClicked: (ConversationId) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { onMessageClicked(item.conversationId) }
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                Text("Message ID: ${item.conversationId.id}")
                Text(item.subject)
            }
        }
    }
}
