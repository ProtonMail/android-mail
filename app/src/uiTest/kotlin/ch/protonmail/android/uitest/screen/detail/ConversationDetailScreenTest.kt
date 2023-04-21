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
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewData
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.test.annotations.suite.SmokeExtendedTest
import ch.protonmail.android.uitest.robot.detail.conversation.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.conversation.bottomSheetSection
import ch.protonmail.android.uitest.robot.detail.conversation.detailTopBarSection
import ch.protonmail.android.uitest.robot.detail.conversation.messageBodySection
import ch.protonmail.android.uitest.robot.detail.conversation.messageHeaderSection
import ch.protonmail.android.uitest.robot.detail.conversation.messagesCollapsedSection
import ch.protonmail.android.uitest.util.getString
import org.junit.Ignore
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
@SmokeExtendedTest
internal class ConversationDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenConversationIsLoadedThenSubjectIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds
        val conversationState = state.conversationState as ConversationDetailMetadataState.Data

        // when
        val robot = setupScreen(state = state)

        // then
        robot.detailTopBarSection {
            verify { hasSubject(conversationState.conversationUiModel.subject) }
        }
    }

    @Test
    fun whenMessageIsLoadedSenderInitialIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds

        // when
        val robot = setupScreen(state = state)

        // then
        val messagesState = state.messagesState as ConversationDetailsMessagesState.Data
        when (val firstMessage = messagesState.messages.first()) {
            is ConversationDetailMessageUiModel.Collapsed -> {
                val initial = firstMessage.avatar as AvatarUiModel.ParticipantInitial
                robot.messagesCollapsedSection {
                    verify { avatarInitialIsDisplayed(index = 0, text = initial.value) }
                }
            }

            is ConversationDetailMessageUiModel.Expanded -> Unit
            is ConversationDetailMessageUiModel.Expanding -> Unit
        }
    }

    @Test
    fun whenMessageIsLoadedTimeIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds

        // when
        val robot = setupScreen(state = state)

        // then
        robot.run {
            val messagesState = state.messagesState as ConversationDetailsMessagesState.Data
            when (val firstMessage = messagesState.messages.first()) {
                is ConversationDetailMessageUiModel.Collapsed -> {
                    messagesCollapsedSection {
                        verify { timeIsDisplayed(index = 0, value = getString(firstMessage.shortTime)) }
                    }
                }

                is ConversationDetailMessageUiModel.Expanded -> {
                    messageHeaderSection {
                        verify {
                            hasTime(
                                value = getString(firstMessage.messageDetailHeaderUiModel.time)
                            )
                        }
                    }
                }

                is ConversationDetailMessageUiModel.Expanding -> Unit
            }
        }
    }

    @Test
    fun whenDraftMessageIsLoadedDraftIconIsDisplayed() {
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
        robot.messagesCollapsedSection {
            verify { avatarDraftIsDisplayed(index = 0) }
        }
    }

    @Test
    fun whenRepliedMessageIsLoadedRepliedIconIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceReplied
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify { repliedIconIsDisplayed(index = 0) }
        }
    }

    @Test
    fun whenRepliedAllMessageIsLoadedRepliedIconIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceRepliedAll
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify { repliedAllIconIsDisplayed(index = 0) }
        }
    }

    @Test
    fun whenForwardedMessageIsLoadedForwardedIconIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceForwarded
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify { forwardedIconIsDisplayed(index = 0) }
        }
    }

    @Test
    fun whenMessagesAreLoadedThenSenderIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds

        // when
        val robot = setupScreen(state = state)

        // then
        robot.run {
            val messagesState = state.messagesState as ConversationDetailsMessagesState.Data
            when (val firstMessage = messagesState.messages.first()) {
                is ConversationDetailMessageUiModel.Collapsed ->
                    messagesCollapsedSection {
                        verify { senderNameIsDisplayed(index = 0, value = firstMessage.sender) }
                    }

                is ConversationDetailMessageUiModel.Expanded -> verify {
                    messageHeaderSection {
                        verify {
                            senderIsDisplayed(firstMessage.messageDetailHeaderUiModel.sender.participantName)
                        }
                    }
                }

                is ConversationDetailMessageUiModel.Expanding -> Unit
            }
        }
    }

    @Test
    fun whenMessageWithExpirationIsLoadedThenExpirationIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.ExpiringInvitation
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify { expirationIsDisplayed(index = 0, value = "12h") }
        }
    }

    @Test
    fun whenStarredMessageIsLoadedThenStarIconIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.StarredInvoice
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify {
                starIconIsDisplayed(index = 0)
            }
        }
    }

    @Test
    @Ignore("The component is correctly displayed, but the test fails to match it")
    fun whenMessageWithAttachmentIsLoadedThenAttachmentIconIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify {
                attachmentIconIsDisplayed(index = 0)
            }
        }
    }

    @Test
    fun whenTrashIsClickedThenActionIsCalled() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds.copy(
            bottomBarState = BottomBarState.Data(
                actions = listOf(ActionUiModelSample.Trash)
            )
        )

        // when
        var trashClicked = false
        val robot = setupScreen(
            state = state,
            actions = ConversationDetailScreen.Actions.Empty.copy(
                onTrashClick = { trashClicked = true }
            )
        )

        robot.bottomSheetSection { moveToTrash() }

        // then
        assertTrue(trashClicked)
    }

    @Test
    fun whenErrorThenErrorMessageIsDisplayed() {
        // given
        val message = TextUiModel("Something terrible happened!")
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds.copy(
            error = Effect.of(message)
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify { errorMessageIsDisplayed(message) }
    }

    @Test
    fun whenUnreadClickedThenCallbackIsInvoked() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds.copy(
            bottomBarState = BottomBarState.Data(
                actions = listOf(ActionUiModelSample.MarkUnread)
            )
        )
        var unreadClicked = false

        // when
        val robot = setupScreen(
            state = state,
            actions = ConversationDetailScreen.Actions.Empty.copy(
                onUnreadClick = { unreadClicked = true }
            )
        )

        robot.bottomSheetSection { markAsUnread() }

        // then
        assertTrue(unreadClicked)
    }

    @Test
    fun whenExitStateThenCallbackIsInvoked() {
        // given
        val state = ConversationDetailsPreviewData.SuccessWithRandomMessageIds.copy(
            exitScreenEffect = Effect.of(Unit)
        )
        var didExit = false

        // when
        setupScreen(
            state = state,
            actions = ConversationDetailScreen.Actions.Empty.copy(
                onExit = { didExit = true }
            )
        )

        // then
        assertTrue(didExit)
    }

    @Test
    fun whenOfflineStateThenOfflineErrorMessageIsDisplayed() {
        // given
        val message = TextUiModel("You're offline. Please go back online to load messages")
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Offline
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.verify { errorMessageIsDisplayed(message) }
    }

    @Test
    fun whenConversationWithExpandedMessagesIsLoadedThenMessageHeaderIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messageHeaderSection {
            verify { headerIsDisplayed() }
        }
    }

    @Test
    fun whenConversationWithExpandedMessagesIsLoadedThenMessageBodyIsDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messageBodySection {
            verify {
                messageBodyIsDisplayedInWebView(
                    ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded.messageBodyUiModel.messageBody
                )
            }
        }
    }

    @Test
    fun whenConversationWithExpandedMessagesIsLoadedThenTheCollapsedHeaderIsNotDisplayed() {
        // given
        val state = ConversationDetailsPreviewData.Success.copy(
            messagesState = ConversationDetailsMessagesState.Data(
                messages = listOf(
                    ConversationDetailMessageUiModelSample.InvoiceWithLabelExpanded
                )
            )
        )

        // when
        val robot = setupScreen(state = state)

        // then
        robot.messagesCollapsedSection {
            verify { collapsedHeaderIsNotDisplayed() }
        }
    }

    private fun setupScreen(
        state: ConversationDetailState,
        actions: ConversationDetailScreen.Actions = ConversationDetailScreen.Actions.Empty
    ): ConversationDetailRobot = composeTestRule.ConversationDetailRobot {
        ConversationDetailScreen(state = state, actions = actions)
    }
}
