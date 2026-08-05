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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.SnackbarType
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId

/**
 * A request to open a message the user picked on the search screen.
 *
 * @param searchQuery forwarded so the detail view highlights the terms the message was found by. Empty
 * when no query is known for it.
 * @param shouldOpenInComposer true for drafts, which open in the composer rather than in the detail view.
 */
data class OpenSearchResultRequest(
    val messageId: MessageId,
    val conversationId: ConversationId,
    val searchQuery: String,
    val isConversationGrouping: Boolean,
    val openedFromLocation: LabelId,
    val shouldOpenInComposer: Boolean
)

data class ContentSearchScreenActions(
    val onClose: () -> Unit,
    val onOpenItem: (request: OpenSearchResultRequest) -> Unit,
    val showSnackbar: (SnackbarType) -> Unit,
    val snackbarHeight: () -> Dp = { 0.dp }
)
