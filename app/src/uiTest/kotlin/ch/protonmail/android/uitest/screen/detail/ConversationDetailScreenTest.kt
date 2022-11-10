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

package ch.protonmail.android.uitest.screen.detail

import androidx.compose.ui.test.junit4.createComposeRule
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import org.junit.Rule
import kotlin.test.Test

class ConversationDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenConversationIsLoadedThenSubjectIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify {
            val conversationState = state.conversationState as ConversationDetailMetadataState.Data
            subjectIsDisplayed(conversationState.conversationUiModel.subject)
        }
    }

    @Test
    fun whenMessagesAreLoadedThenSenderIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify {
            val messagesState = state.messagesState as ConversationDetailsMessagesState.Data
            val firstMessage = messagesState.messages.first()
            senderIsDisplayed(firstMessage.sender)
        }
    }

    @Test
    fun whenMessageIsLoadedSenderInitialIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify {
            val messagesState = state.messagesState as ConversationDetailsMessagesState.Data
            val firstMessage = messagesState.messages.first()
            val initial = firstMessage.avatar as AvatarUiModel.ParticipantInitial
            senderInitialIsDisplayed(initial = initial.value)
        }
    }

    @Test
    fun whenDraftMessageIsLoadedSenderInitialIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.EmptyDraft
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify { draftIconAvatarIsDisplayed() }
    }

    private fun setupScreen(
        state: ConversationDetailState,
        actions: ConversationDetailScreen.Actions = ConversationDetailScreen.Actions.Empty
    ): ConversationDetailRobot = composeTestRule.ConversationDetailRobot {
        ConversationDetailScreen(state = state, actions = actions)
    }
}
