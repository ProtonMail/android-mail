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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltipState
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageBannersUiModelTestData.messageBannersUiModel
import ch.protonmail.android.testdata.maildetail.MessageDetailFooterUiModelTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class MessageDetailReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val messageMetadataReducer: MessageDetailMetadataReducer = mockk {
        coEvery { newStateFrom(any(), any()) } returns reducedState.messageMetadataState
    }

    private val messageBannersReducer: MessageBannersReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.messageBannersState
    }

    private val messageBodyReducer: MessageBodyReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.messageBodyState
    }

    private val bottomBarReducer: BottomBarReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomBarState
    }

    private val bottomSheetReducer: BottomSheetReducer = mockk {
        every { newStateFrom(any(), any()) } returns reducedState.bottomSheetState
    }

    private val deleteDialogReducer: MessageDeleteDialogReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.deleteDialogState
    }

    private val reportPhishingDialogReducer: MessageReportPhishingDialogReducer = mockk {
        every { newStateFrom(any()) } returns reducedState.reportPhishingDialogState
    }

    private val mailLabelTextMapper = MailLabelTextMapper(mockk())

    private val customizeToolbarSpotlightReducer = mockk<MessageCustomizeToolbarSpotlightReducer>()

    private val detailReducer = MessageDetailReducer(
        messageMetadataReducer,
        messageBannersReducer,
        messageBodyReducer,
        bottomBarReducer,
        bottomSheetReducer,
        deleteDialogReducer,
        reportPhishingDialogReducer,
        mailLabelTextMapper,
        customizeToolbarSpotlightReducer
    )

    @Test
    fun `should reduce only the affected parts of the state`() = runTest {
        with(testInput) {
            val nextState = detailReducer.newStateFrom(currentState, operation)

            if (shouldReduceMessageMetadataState) {
                coVerify {
                    messageMetadataReducer.newStateFrom(
                        currentState.messageMetadataState,
                        operation as MessageDetailOperation.AffectingMessage
                    )
                }
            } else {
                assertEquals(currentState.messageMetadataState, nextState.messageMetadataState, testName)
            }

            if (shouldReduceMessageBannersState) {
                verify {
                    messageBannersReducer.newStateFrom(
                        operation as MessageDetailOperation.AffectingMessageBanners
                    )
                }
            } else {
                assertEquals(currentState.messageBannersState, nextState.messageBannersState, testName)
            }

            if (shouldReduceMessageBodyState) {
                verify {
                    messageBodyReducer.newStateFrom(
                        currentState.messageBodyState,
                        operation as MessageDetailOperation.AffectingMessageBody
                    )
                }
            } else {
                assertEquals(currentState.messageBodyState, nextState.messageBodyState, testName)
            }

            if (shouldReduceBottomBarState) {
                verify {
                    bottomBarReducer.newStateFrom(
                        currentState.bottomBarState,
                        (operation as MessageDetailEvent.MessageBottomBarEvent).bottomBarEvent
                    )
                }
            } else {
                assertEquals(currentState.bottomBarState, nextState.bottomBarState, testName)
            }

            if (shouldReduceBottomSheetState) {
                verify {
                    bottomSheetReducer.newStateFrom(
                        currentState.bottomSheetState,
                        any()
                    )
                }
            } else {
                assertEquals(currentState.bottomSheetState, nextState.bottomSheetState, testName)
            }

            if (shouldReduceToErrorEffect) {
                assertTrue(nextState.error.consume() is TextUiModel.TextRes)
            } else {
                assertEquals(currentState.error, nextState.error, testName)
            }

            if (shouldReduceExitEffect) {
                assertNotNull(nextState.exitScreenEffect.consume(), testName)
            } else {
                assertEquals(currentState.exitScreenEffect, nextState.exitScreenEffect, testName)
            }

            if (shouldReduceOpenMessageBodyLinkEffect) {
                assertNotNull(nextState.openMessageBodyLinkEffect.consume(), testName)
            } else {
                assertEquals(currentState.exitScreenEffect, nextState.exitScreenEffect, testName)
            }

            if (exitMessage != null) {
                assertEquals(exitMessage, nextState.exitScreenWithMessageEffect.consume())
            }

            if (shouldReduceDeleteDialogState) {
                verify { deleteDialogReducer.newStateFrom(any()) }
            } else {
                assertEquals(currentState.deleteDialogState, nextState.deleteDialogState, testName)
            }

            // Reducer should not change the requestLinkConfirmation flag, just copy it
            assertEquals(currentState.requestLinkConfirmation, nextState.requestLinkConfirmation)

            if (shouldReducePhishingLinkConfirmation) {
                assertEquals(
                    (operation as MessageDetailEvent.MessageWithLabelsEvent).messageWithLabels.message.isPhishing(),
                    nextState.requestPhishingLinkConfirmation
                )
            } else {
                assertEquals(currentState.requestPhishingLinkConfirmation, nextState.requestPhishingLinkConfirmation)
            }

            if (shouldReduceReportPhishingDialogState) {
                verify { reportPhishingDialogReducer.newStateFrom(any()) }
            } else {
                assertEquals(currentState.reportPhishingDialogState, nextState.reportPhishingDialogState, testName)
            }
        }
    }

    companion object {

        private val detailHeaderUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        private val detailFooterUiModel = MessageDetailFooterUiModelTestData.messageDetailFooterUiModel
        private val messageBodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        private val actionBarUiModel = MessageDetailActionBarUiModelTestData.uiModel
        private val currentState = MessageDetailState.Loading
        private val reducedState = MessageDetailState(
            messageMetadataState = MessageMetadataState.Data(
                actionBarUiModel,
                detailHeaderUiModel,
                detailFooterUiModel
            ),
            messageBannersState = MessageBannersState.Data(messageBannersUiModel),
            messageBodyState = MessageBodyState.Data(messageBodyUiModel),
            bottomBarState = BottomBarState.Data.Shown(listOf(ActionUiModelTestData.markUnread).toImmutableList()),
            bottomSheetState = BottomSheetState(
                MoveToBottomSheetState.Data(
                    MailLabelUiModelTestData.spamAndCustomFolder,
                    null,
                    MoveToBottomSheetEntryPoint.Message(messageBodyUiModel.messageId)
                )
            ),
            exitScreenEffect = Effect.empty(),
            exitScreenWithMessageEffect = Effect.empty(),
            error = Effect.empty(),
            openMessageBodyLinkEffect = Effect.empty(),
            openAttachmentEffect = Effect.empty(),
            openProtonCalendarIntent = Effect.empty(),
            requestLinkConfirmation = false,
            requestPhishingLinkConfirmation = false,
            deleteDialogState = DeleteDialogState.Hidden,
            reportPhishingDialogState = ReportPhishingDialogState.Hidden,
            spotlightTooltip = SpotlightTooltipState.Hidden
        )

        private val actions = listOf(
            TestInput(
                MessageViewAction.Star,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.UnStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.MarkUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = true,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.Trash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = ActionResult.UndoableActionResult(TextUiModel(string.message_moved_to_trash)),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.RequestMoveToBottomSheet,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.DismissBottomSheet,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.MoveToDestinationConfirmed(MailLabelText("testLabel")),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = ActionResult.UndoableActionResult(
                    TextUiModel(string.message_moved_to_selected_destination, "testLabel")
                ),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.LabelAsToggleAction(LabelId("customLabel")),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.LabelAsConfirmed(false),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.LabelAsConfirmed(true),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = ActionResult.DefinitiveActionResult(TextUiModel(string.message_moved_to_archive)),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.MessageBodyLinkClicked(mockk()),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = true,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.DeleteRequested,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = true
            ),
            TestInput(
                MessageViewAction.DeleteDialogDismissed,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = true
            ),
            TestInput(
                MessageViewAction.DeleteConfirmed,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = true
            ),
            TestInput(
                MessageViewAction.ReportPhishing(MessageIdSample.Invoice),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = false,
                shouldReduceReportPhishingDialogState = false
            ),
            TestInput(
                MessageViewAction.ReportPhishingConfirmed,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = true,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = false,
                shouldReduceReportPhishingDialogState = true
            ),
            TestInput(
                MessageViewAction.ReportPhishingDismissed,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = false,
                shouldReduceReportPhishingDialogState = true
            ),
            TestInput(
                MessageViewAction.SwitchViewMode(ViewModePreference.DarkMode),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = false,
                shouldReduceReportPhishingDialogState = false
            ),
            TestInput(
                MessageViewAction.PrintRequested,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = false,
                shouldReduceReportPhishingDialogState = false
            ),
            TestInput(
                MessageViewAction.Archive,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = ActionResult.UndoableActionResult(TextUiModel(string.message_moved_to_archive)),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageViewAction.Spam,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                exitMessage = ActionResult.UndoableActionResult(TextUiModel(string.message_moved_to_spam)),
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            )
        )

        private val events = listOf(
            TestInput(
                MessageDetailEvent.MessageWithLabelsEvent(
                    MessageWithLabelsSample.Invoice,
                    emptyList(),
                    FolderColorSettings(),
                    AutoDeleteSetting.Disabled
                ),
                shouldReduceMessageBodyState = false,
                shouldReduceMessageBannersState = true,
                shouldReduceMessageMetadataState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = true
            ),
            TestInput(
                MessageDetailEvent.MessageBodyEvent(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel,
                    MessageBodyExpandCollapseMode.NotApplicable
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.NoCachedMetadata,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = true,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = true,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorAddingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorRemovingStar,
                shouldReduceMessageMetadataState = true,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMarkingUnread,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToTrash,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        MailLabelUiModelTestData.spamAndCustomFolder,
                        entryPoint = MoveToBottomSheetEntryPoint.Message(messageBodyUiModel.messageId)
                    )
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.MessageBottomSheetEvent(
                    LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                        customLabelList = MailLabelUiModelTestData.customLabelList,
                        selectedLabels = listOf<LabelId>().toImmutableList(),
                        entryPoint = LabelAsBottomSheetEntryPoint.Message(messageBodyUiModel.messageId)
                    )
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.AttachmentStatusChanged(
                    attachmentId = AttachmentId("attachmentId"),
                    status = AttachmentWorkerStatus.Running
                ),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = true,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingAttachment,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorGettingAttachmentNotEnoughSpace,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorDeletingMessage,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = true
            ),
            TestInput(
                MessageDetailEvent.ErrorDeletingNoApplicableFolder,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceDeleteDialogState = true
            ),
            TestInput(
                MessageDetailEvent.ReportPhishingRequested(messageId = MessageIdSample.Invoice, isOffline = true),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceReportPhishingDialogState = true
            ),
            TestInput(
                MessageDetailEvent.ReportPhishingRequested(messageId = MessageIdSample.Invoice, isOffline = false),
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = false,
                shouldReduceBottomSheetState = true,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false,
                shouldReduceReportPhishingDialogState = true
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToArchive,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            ),
            TestInput(
                MessageDetailEvent.ErrorMovingToSpam,
                shouldReduceMessageMetadataState = false,
                shouldReduceMessageBannersState = false,
                shouldReduceMessageBodyState = false,
                shouldReduceBottomBarState = false,
                shouldReduceExitEffect = false,
                shouldReduceToErrorEffect = true,
                shouldReduceBottomSheetState = false,
                shouldReduceOpenMessageBodyLinkEffect = false,
                shouldReducePhishingLinkConfirmation = false
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (actions + events)
                .map { testInput ->
                    val testName = """
                        Operation: ${testInput.operation}
                        
                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val operation: MessageDetailOperation,
        val shouldReduceMessageMetadataState: Boolean,
        val shouldReduceMessageBannersState: Boolean,
        val shouldReduceMessageBodyState: Boolean,
        val shouldReduceBottomBarState: Boolean,
        val shouldReduceExitEffect: Boolean,
        val shouldReduceBottomSheetState: Boolean,
        val shouldReduceToErrorEffect: Boolean,
        val shouldReduceOpenMessageBodyLinkEffect: Boolean,
        val shouldReducePhishingLinkConfirmation: Boolean,
        val exitMessage: ActionResult? = null,
        val shouldReduceDeleteDialogState: Boolean = false,
        val shouldReduceReportPhishingDialogState: Boolean = false
    )
}
