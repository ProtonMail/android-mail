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

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.detail.ExtendedHeaderRecipientEntry
import ch.protonmail.android.uitest.models.labels.LabelEntry
import ch.protonmail.android.uitest.robot.detail.messageDetailRobot
import ch.protonmail.android.uitest.robot.detail.section.bottomBarSection
import ch.protonmail.android.uitest.robot.detail.section.messageBodySection
import ch.protonmail.android.uitest.robot.detail.section.messageHeaderSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertTrue

@RegressionTest
@HiltAndroidTest
internal class MessageDetailScreenTest : HiltInstrumentedTest() {

    @Test
    fun whenMessageIsLoadedThenMessageHeaderIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message

        // when
        val robot = messageDetailRobot { setUpScreen(state = state) }

        // then
        robot.messageHeaderSection {
            verify { headerIsDisplayed() }
        }
    }

    @Test
    fun whenMessageIsLoadedThenAvatarIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val avatarInitial = (messageState.messageDetailHeader.avatar as AvatarUiModel.ParticipantInitial).run {
            AvatarInitial.WithText(value)
        }

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasAvatarInitial(avatarInitial) }
        }
    }

    @Test
    fun whenMessageIsLoadedThenSenderNameIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasSenderName(messageState.messageDetailHeader.sender.participantName) }
        }
    }

    @Test
    fun whenMessageIsLoadedThenSenderAddressIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasSenderAddress(messageState.messageDetailHeader.sender.participantAddress) }
        }
    }

    @Test
    fun whenMessageIsLoadedThenTimeIsDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val time = messageState.messageDetailHeader.time as TextUiModel.Text

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasTime(time.value) }
        }
    }

    @Test
    fun whenMessageIsLoadedThenRecipientsAreDisplayedInMessageHeader() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val recipients = messageState.messageDetailHeader.allRecipients as TextUiModel.Text

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasRecipient(recipients.value) }
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenExpandedRecipientsAreDisplayed() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val recipients = messageState.messageDetailHeader.toRecipients.mapIndexed { idx: Int, element ->
            ExtendedHeaderRecipientEntry.To(index = idx, element.participantName, element.participantAddress)
        }.toTypedArray()

        val robot = setUpScreen(state = state)

        // when
        robot.messageHeaderSection { expandHeader() }

        // then
        robot.messageHeaderSection {
            expanded {
                verify { hasRecipients(*recipients) }
            }
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenExtendedTimeIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val time = messageState.messageDetailHeader.extendedTime as TextUiModel.Text
        val robot = setUpScreen(state = state)

        // when
        robot.messageHeaderSection { expandHeader() }

        // then
        robot.messageHeaderSection {
            expanded {
                verify { hasTime(time.value) }
            }
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenLocationNameIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val robot = setUpScreen(state = state)

        // when
        robot.messageHeaderSection { expandHeader() }

        // then
        robot.messageHeaderSection {
            expanded {
                verify { hasLocation(messageState.messageDetailHeader.location.name) }
            }
        }
    }

    @Test
    fun whenMessageIsLoadedAndMessageHeaderIsClickedThenMessageSizeIsShown() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageState = state.messageMetadataState as MessageMetadataState.Data
        val robot = setUpScreen(state = state)

        // when
        robot.messageHeaderSection { expandHeader() }

        // then
        robot.messageHeaderSection {
            expanded {
                verify { hasSize(messageState.messageDetailHeader.size) }
            }
        }
    }

    @Test
    fun whenMessageIsLoadedAndHasOneRecipientThenReplyQuickActionButtonIsShown() {
        // given
        val state = MessageDetailHeaderTestData.MessageWithOneRecipient

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasReplyButton() }
        }
    }

    @Test
    fun whenMessageIsLoadedAndHasMoreThanOneRecipientThenReplyAllQuickActionButtonIsShown() {
        // given
        val state = MessageDetailHeaderTestData.MessageWithMultipleRecipients

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasReplyAllButton() }
        }
    }

    @Test
    fun whenTrashIsClickedThenActionIsCalled() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            bottomBarState = BottomBarState.Data.Shown(
                actions = listOf(ActionUiModelSample.Trash).toImmutableList()
            )
        )

        var trashClicked = false

        val robot = setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onTrashClick = { trashClicked = true }
            )
        )

        // when
        robot.bottomBarSection { moveToTrash() }

        // then
        assertTrue(trashClicked)
    }

    @Test
    fun whenMessageWithLabelsIsLoadedThenFirstLabelIsDisplayed() {
        // given
        val state = MessageDetailsPreviewData.MessageWithLabels
        val label = (state.messageMetadataState as MessageMetadataState.Data).messageDetailHeader.labels.first()
        val labelEntry = LabelEntry(index = 0, text = label.name)

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { hasLabels(labelEntry) }
        }
    }

    @Test
    fun whenUnreadClickedThenCallbackIsInvoked() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            bottomBarState = BottomBarState.Data.Shown(
                actions = listOf(ActionUiModelSample.MarkUnread).toImmutableList()
            )
        )
        var unreadClicked = false

        // when
        val robot = setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onUnreadClick = { unreadClicked = true }
            )
        )

        robot.bottomBarSection { markAsUnread() }

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
    fun whenPlainTextMessageBodyIsLoadedThenPlainTextMessageBodyIsDisplayedInWebView() {
        // given
        val state = MessageDetailsPreviewData.Message
        val messageBody = (state.messageBodyState as MessageBodyState.Data).messageBodyUiModel.messageBodyWithoutQuote

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageBodySection {
            verify { messageInWebViewContains(messageBody) }
        }
    }

    @Test
    fun whenHtmlMessageBodyIsLoadedThenHtmlMessageBodyIsDisplayedInWebView() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            messageBodyState = MessageBodyState.Data(MessageBodyUiModelTestData.htmlMessageBodyUiModel)
        )
        val messageBody = """
            Dear Test,
            This is an HTML message body.
            Kind regards,
            Developer
        """.trimIndent()

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageBodySection {
            verify { messageInWebViewContains(messageBody, tagName = "div") }
        }
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
        robot.messageBodySection {
            verify { loadingErrorMessageIsDisplayed(errorMessage) }
        }
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
        robot.messageBodySection {
            verify {
                loadingErrorMessageIsDisplayed(errorMessage)
                bodyReloadButtonIsDisplayed()
            }
        }
    }

    @Test
    fun whenMessageBodyDecryptionFailedThenEncryptedBodyAndErrorMessageAreShown() {
        // given
        val state = MessageDetailsPreviewData.Message.copy(
            messageBodyState = MessageBodyState.Error.Decryption(MessageBodyUiModelTestData.plainTextMessageBodyUiModel)
        )
        val messageBody = (state.messageBodyState as MessageBodyState.Error.Decryption).encryptedMessageBody.messageBody

        // when
        val robot = setUpScreen(state = state)

        // then
        robot.messageBodySection {
            verify {
                bodyDecryptionErrorMessageIsDisplayed()
                messageInWebViewContains(messageBody)
            }
        }
    }

    @Test
    fun whenMessageBodyLinkWasClickedThenCallbackIsInvoked() {
        // Given
        val uri = Uri.EMPTY
        val state = MessageDetailsPreviewData.Message.copy(
            openMessageBodyLinkEffect = Effect.of(uri)
        )
        var isMessageBodyLinkOpened = false

        // When
        setUpScreen(
            state = state,
            actions = MessageDetailScreen.Actions.Empty.copy(
                onOpenMessageBodyLink = { isMessageBodyLinkOpened = true }
            )
        )

        // Then
        assertTrue(isMessageBodyLinkOpened)
    }

    private fun setUpScreen(
        state: MessageDetailState,
        actions: MessageDetailScreen.Actions = MessageDetailScreen.Actions.Empty
    ) = messageDetailRobot {
        this@MessageDetailScreenTest.composeTestRule.setContent {
            MessageDetailScreen(state = state, actions = actions)
        }
    }
}
