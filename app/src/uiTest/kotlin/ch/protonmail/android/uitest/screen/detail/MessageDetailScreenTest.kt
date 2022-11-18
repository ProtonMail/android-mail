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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import org.junit.Rule
import kotlin.test.Test

class MessageDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenMessageIsLoadedThenMessageHeaderIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            messageHeaderIsDisplayed()
        }
    }

    @Test
    fun whenMessageIsLoadedThenAvatarIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            avatarIsDisplayed()
        }
    }

    @Test
    fun whenMessageIsLoadedThenSenderNameIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            senderNameIsDisplayed(messageState.messageDetailHeader.sender.participantName)
        }
    }

    @Test
    fun whenMessageIsLoadedThenSenderAddressIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            senderAddressIsDisplayed(messageState.messageDetailHeader.sender.participantAddress)
        }
    }

    @Test
    fun whenMessageIsLoadedThenTimeIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            timeIsDisplayed(messageState.messageDetailHeader.time as TextUiModel.Text)
        }
    }

    @Test
    fun whenMessageIsLoadedThenRecipientsAreDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            allRecipientsAreDisplayed(messageState.messageDetailHeader.allRecipients as TextUiModel.Text)
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenExpandedRecipientsAreDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            expandedRecipientsAreDisplayed(messageState.messageDetailHeader.toRecipients)
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenExtendedTimeIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            extendedTimeIsDisplayed(messageState.messageDetailHeader.extendedTime as TextUiModel.Text)
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenLocationNameIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            locationNameIsDisplayed(messageState.messageDetailHeader.location.name)
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenMessageSizeIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            sizeIsDisplayed(messageState.messageDetailHeader.size)
        }
    }

    private fun setUpScreen(
        state: MessageDetailState,
        actions: MessageDetailScreen.Actions = MessageDetailScreen.Actions.Empty
    ): MessageDetailRobot = composeTestRule.MessageDetailRobot {
        MessageDetailScreen(state = state, actions = actions)
    }
}
