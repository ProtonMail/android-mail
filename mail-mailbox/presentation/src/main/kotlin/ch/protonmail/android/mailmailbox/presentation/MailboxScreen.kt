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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailconversation.domain.Conversation
import ch.protonmail.android.mailconversation.domain.ConversationId
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme

const val TEST_TAG_MAILBOX_SCREEN = "MailboxScreenTestTag"

@Composable
fun MailboxScreen(
    navigateToConversation: (ConversationId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MailboxViewModel = hiltViewModel(),
) {
    val mailboxState by rememberAsState(viewModel.state, MailboxState())

    MailboxScreen(
        navigateToConversation = navigateToConversation,
        modifier = modifier,
        mailboxState = mailboxState
    )
}

@Composable
private fun MailboxScreen(
    navigateToConversation: (ConversationId) -> Unit,
    modifier: Modifier = Modifier,
    mailboxState: MailboxState = MailboxState(),
) {
    LazyColumn(
        modifier = modifier
            .background(ProtonTheme.colors.backgroundNorm)
            .fillMaxSize()
            .testTag(TEST_TAG_MAILBOX_SCREEN)
    ) {
        item {
            Text("Header: Location: ${mailboxState.filteredLocations}")
        }
        items(
            items = mailboxState.mailboxItems,
            key = { it.conversationId.id }
        ) { item ->
            MailboxItem(item) { navigateToConversation(it) }
        }
        item {
            Text("Footer")
        }
    }
}

@Composable
private fun MailboxItem(
    item: Conversation,
    onItemClicked: (ConversationId) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { onItemClicked(item.conversationId) }
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

@Preview(
    name = "Sidebar in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Sidebar in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun PreviewMailbox() {
    ProtonTheme {
        MailboxScreen(
            navigateToConversation = {},
            mailboxState = MailboxState(
                mailboxItems = listOf(
                    Conversation(ConversationId("1"), "First message"),
                    Conversation(ConversationId("2"), "Second message"),
                ),
                filteredLocations = setOf(MailLocation.Inbox)
            )
        )
    }
}
