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

package ch.protonmail.android.maildetail.presentation.ui

import ch.protonmail.android.maildetail.presentation.ui.footer.MessageDetailFooter
import ch.protonmail.android.mailmessage.domain.model.MessageId
import org.junit.Test
import kotlin.test.assertEquals

internal class MessageDetailFooterActionsTest {

    private val replyLambda: (MessageId) -> Unit = { println("Reply $it") }
    private val replyAllLambda: (MessageId) -> Unit = { println("Reply All $it") }
    private val forwardLambda: (MessageId) -> Unit = { println("Forward $it") }
    private val expected = MessageDetailFooter.Actions(
        onReply = replyLambda,
        onReplyAll = replyAllLambda,
        onForward = forwardLambda
    )

    @Test
    fun `should map the message details content actions correctly`() {
        val actions = MessageDetailContent.Actions(
            onExpandCollapseButtonClicked = {},
            onMessageBodyLinkClicked = {},
            onShowAllAttachmentsClicked = {},
            onAttachmentClicked = {},
            loadEmbeddedImage = { _, _ -> null },
            onReply = replyLambda,
            onReplyAll = replyAllLambda,
            onForward = forwardLambda,
            onMoreActionsClick = {},
            onReload = {},
            onLoadRemoteContent = {},
            onLoadEmbeddedImages = {},
            onLoadRemoteAndEmbeddedContent = {},
            onOpenInProtonCalendar = {},
            showFeatureMissingSnackbar = {},
            onPrint = {},
            onAvatarClicked = { _, _ -> },
            onParticipantClicked = { _, _ -> },
            onViewEntireMessageClicked = { _, _, _, _ -> }
        )

        // When
        val actual = MessageDetailFooter.Actions.fromMessageDetailContentActions(actions)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should map the conversation detail item actions correctly`() {
        val actions = ConversationDetailItem.Actions(
            onMessageBodyLinkClicked = { _, _ -> },
            onAttachmentClicked = { _, _ -> },
            loadEmbeddedImage = { _, _ -> null },
            onReply = replyLambda,
            onReplyAll = replyAllLambda,
            onForward = forwardLambda,
            onMoreActionsClick = {},
            onLoadRemoteContent = {},
            onLoadEmbeddedImages = {},
            onLoadRemoteAndEmbeddedContent = {},
            onOpenInProtonCalendar = {},
            onCollapse = {},
            onExpand = {},
            onShowAllAttachmentsForMessage = {},
            onOpenMessageBodyLink = {},
            showFeatureMissingSnackbar = {},
            onBodyExpandCollapseButtonClicked = {},
            onScrollRequestCompleted = {},
            onPrint = {},
            onAvatarClicked = { _, _ -> },
            onParticipantClicked = { _, _ -> },
            onOpenComposer = {},
            onViewEntireMessageClicked = { _, _, _, _ -> },
            onEffectConsumed = { _, _ -> }
        )

        // When
        val actual = MessageDetailFooter.Actions.fromConversationDetailItemActions(actions)

        // Then
        assertEquals(expected, actual)
    }
}
