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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

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
            .expandHeader()

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
            .expandHeader()

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
            .expandHeader()

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
            .expandHeader()

        // then
        robot.verify {
            val messageState = state.messageMetadataState as MessageMetadataState.Data
            sizeIsDisplayed(messageState.messageDetailHeader.size)
        }
    }

    @Test
    fun whenTrashIsClickedThenActionIsCalled() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            bottomBarState = BottomBarState.Data(
                actions = listOf(ActionUiModelSample.Trash)
            )
        )

        // when
        var trashClicked = false
        setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onTrashClick = { trashClicked = true }
            )
        ).moveToTrash()

        // then
        assertTrue(trashClicked)
    }

    @Test
    fun whenMessageWithLabelsIsLoadedThenFirstLabelIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.MessageWithLabels
        val label = (state.messageMetadataState as MessageMetadataState.Data).messageDetailHeader.labels.first()

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify { labelIsDisplayed(label.name) }
    }

    @Test
    fun whenUnreadClickedThenCallbackIsInvoked() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            bottomBarState = BottomBarState.Data(
                actions = listOf(ActionUiModelSample.MarkUnread)
            )
        )
        var unreadClicked = false

        // when
        setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onUnreadClick = { unreadClicked = true }
            )
        ).markAsUnread()

        // then
        assertTrue(unreadClicked)
    }

    @Test
    fun whenExitStateThenCallbackIsInvoked() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            exitScreenEffect = Effect.of(Unit)
        )
        var didExit = false

        // when
        setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onExit = { didExit = true }
            )
        )

        // then
        assertTrue(didExit)
    }

    @Test
    fun whenMessageBodyIsLoadedThenMessageBodyIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageBody = (state.messageBodyState as MessageBodyState.Data).messageBodyUiModel.messageBody

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify { messageBodyIsDisplayed(messageBody) }
    }

    @Test
    fun whenMessageBodyLoadingFailedWithNoNetworkThenErrorMessageIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            messageBodyState = MessageBodyState.Error.Data(isNetworkError = true)
        )
        val errorMessage = R.string.error_offline_loading_message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify { messageBodyLoadingErrorMessageIsDisplayed(errorMessage) }
    }

    @Test
    fun whenMessageBodyLoadingFailedThenErrorMessageAndReloadButtonIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            messageBodyState = MessageBodyState.Error.Data(isNetworkError = false)
        )
        val errorMessage = R.string.error_loading_message

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            messageBodyLoadingErrorMessageIsDisplayed(errorMessage)
            messageBodyReloadButtonIsDisplayed()
        }
    }

    @Test
    fun whenMessageBodyDecryptionFailedThenEncryptedBodyAndErrorMessageAreShown() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            messageBodyState = MessageBodyState.Error.Decryption(MessageBodyUiModelTestData.messageBodyUiModel)
        )
        val messageBody = (state.messageBodyState as MessageBodyState.Error.Decryption).encryptedMessageBody.messageBody

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.verify {
            messageBodyDecryptionErrorMessageIsDisplayed()
            messageBodyIsDisplayed(messageBody)
        }
    }

    private fun setUpScreen(
        state: MessageDetailState,
        actions: MessageDetailScreen.Actions = MessageDetailScreen.Actions.Empty
    ): MessageDetailRobot = composeTestRule.MessageDetailRobot {
        MessageDetailScreen(state = state, actions = actions)
    }
}
