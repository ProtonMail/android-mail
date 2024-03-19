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

import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.DetailScreenTopBar
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.detail.conversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.messageDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.detailTopBarSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@RegressionTest
@HiltAndroidTest
internal class DetailScreenTopBarTest : HiltInstrumentedTest() {

    @Test
    fun whenConversationIsLoadingThenSubjectContainsABlankString() {
        // given
        val state = ConversationDetailsPreviewData.Loading

        // when
        val robot = setupScreen(state = state)

        // then
        robot.detailTopBarSection {
            verify { hasSubject(DetailScreenTopBar.NoTitle) }
        }
    }

    @Test
    fun whenMessageIsLoadingThenSubjectContainsABlankString() {
        // given
        val state = MessageDetailsPreviewData.Loading

        // when
        val robot = setupScreen(state = state)

        // then
        robot.detailTopBarSection {
            verify { hasSubject(DetailScreenTopBar.NoTitle) }
        }
    }

    @Test
    fun whenConversationIsLoadedThenSubjectIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success
        val conversationState = state.conversationState as ConversationDetailMetadataState.Data

        // when
        val robot = setupScreen(state = state)

        // then
        robot.detailTopBarSection {
            verify { hasSubject(conversationState.conversationUiModel.subject) }
        }
    }

    @Test
    fun whenMessageIsLoadedThenSubjectIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data

        // when
        val robot = setupScreen(state = state)

        // then
        robot.detailTopBarSection {
            verify { hasSubject(messageState.messageDetailActionBar.subject) }
        }
    }

    private fun setupScreen(
        state: ConversationDetailState,
        actions: ConversationDetailScreen.Actions = ConversationDetailScreen.Actions.Empty
    ): ConversationDetailRobot = conversationDetailRobot {
        this@DetailScreenTopBarTest.composeTestRule.setContent {
            ConversationDetailScreen(state = state, actions = actions, scrollToMessageId = null)
        }
    }

    private fun setupScreen(
        state: MessageDetailState,
        actions: MessageDetailScreen.Actions = MessageDetailScreen.Actions.Empty
    ): MessageDetailRobot = messageDetailRobot {
        this@DetailScreenTopBarTest.composeTestRule.setContent {
            MessageDetailScreen(state = state, actions = actions)
        }
    }
}
