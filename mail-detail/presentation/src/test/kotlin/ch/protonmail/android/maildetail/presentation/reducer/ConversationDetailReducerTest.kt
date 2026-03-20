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

import java.util.UUID
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailconversation.domain.entity.HiddenMessagesBanner
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ActionResultMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.model.ScrollToMessageState
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoUiModelSample
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.assertIs
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class ConversationDetailReducerTest(
    @Suppress("unused") private val testName: String,
    private val testInput: TestInput
) {

    private val mailLabelTextMapper = mockk<MailLabelTextMapper> {
        every { this@mockk.mapToString(any()) } returns "String"
    }

    private val actionResultMapper = ActionResultMapper(mailLabelTextMapper)
    private val bottomBarReducer = mockk<BottomBarReducer>(relaxed = true)
    private val messagesReducer = mockk<ConversationDetailMessagesReducer>(relaxed = true)
    private val metadataReducer = mockk<ConversationDetailMetadataReducer>(relaxed = true)
    private val bottomSheetReducer = mockk<BottomSheetReducer>(relaxed = true)
    private val deleteDialogReducer = mockk<ConversationDeleteDialogReducer>(relaxed = true)
    private val reportPhishingDialogReducer = mockk<ConversationReportPhishingDialogReducer>(relaxed = true)
    private val blockSenderDialogReducer = mockk<ConversationBlockSenderDialogReducer>(relaxed = true)
    private val trashedMessagesBannerReducer = mockk<HiddenMessagesBannerReducer>(relaxed = true)
    private val markAsLegitimateDialogReducer = mockk<MarkAsLegitimateDialogReducer>(relaxed = true)
    private val editScheduledMessageDialogReducer = mockk<EditScheduledMessageDialogReducer>(relaxed = true)
    private val reducer = ConversationDetailReducer(
        bottomBarReducer = bottomBarReducer,
        messagesReducer = messagesReducer,
        metadataReducer = metadataReducer,
        bottomSheetReducer = bottomSheetReducer,
        deleteDialogReducer = deleteDialogReducer,
        reportPhishingDialogReducer = reportPhishingDialogReducer,
        blockSenderDialogReducer = blockSenderDialogReducer,
        hiddenMessagesBannerReducer = trashedMessagesBannerReducer,
        markAsLegitimateDialogReducer = markAsLegitimateDialogReducer,
        editScheduledMessageDialogReducer = editScheduledMessageDialogReducer,
        actionResultMapper = actionResultMapper
    )

    @Test
    fun `does call the correct sub-reducers`() = runTest {
        with(testInput) {
            val result = reducer.newStateFrom(ConversationDetailState.Loading, operation)

            if (reducesMessages) {
                coVerify { messagesReducer.newStateFrom(any(), operationAffectingMessages()) }
            } else {
                verify { messagesReducer wasNot Called }
            }

            if (reducesConversation) {
                verify { metadataReducer.newStateFrom(any(), operationAffectingConversation()) }
            } else {
                verify { metadataReducer wasNot Called }
            }

            if (reducesBottomBar) {
                verify { bottomBarReducer.newStateFrom(any(), operationAffectingBottomBar().bottomBarEvent) }
            } else {
                verify { bottomBarReducer wasNot Called }
            }

            if (reducesBottomSheet) {
                verify { bottomSheetReducer.newStateFrom(any(), any()) }
            } else {
                verify { bottomSheetReducer wasNot Called }
            }

            if (reducesErrorBar) {
                assertNotNull(result.error.consume())
            } else {
                assertNull(result.error.consume())
            }

            if (reducesMessageBar) {
                assertNotNull(result.actionResult.consume())
            } else {
                assertNull(result.actionResult.consume())
            }

            if (reducesExit) {
                assertNotNull(result.exitScreenEffect.consume())
            } else {
                assertNull(result.exitScreenEffect.consume())
            }

            if (reducesLoadingError) {
                assertNotNull(result.loadingErrorEffect.consume())
            } else {
                assertNull(result.loadingErrorEffect.consume())
            }

            if (expectedExitMessage != null) {
                assertEquals(expectedExitMessage, result.exitScreenActionResult.consume())
            }

            if (reducesLinkClick) {
                assertNotNull(result.openMessageBodyLinkEffect.consume())
            } else {
                assertNull(result.openMessageBodyLinkEffect.consume())
            }

            if (reducesMessageScroll) {
                assertIsNot<ScrollToMessageState.NoScrollTarget>(result.scrollToMessageState)
            } else {
                assertIs<ScrollToMessageState.NoScrollTarget>(result.scrollToMessageState)
            }

            if (reducesDeleteDialog) {
                verify { deleteDialogReducer.newStateFrom(any()) }
            } else {
                verify { deleteDialogReducer wasNot Called }
            }

            if (reducesTrashedMessagesBanner) {
                verify { trashedMessagesBannerReducer.newStateFrom(any()) }
            } else {
                verify { trashedMessagesBannerReducer wasNot Called }
            }

            if (reducesReportPhishingDialog) {
                verify { reportPhishingDialogReducer.newStateFrom(any()) }
            } else {
                verify { reportPhishingDialogReducer wasNot Called }
            }

            if (reducesBlockSenderDialog) {
                verify { blockSenderDialogReducer.newStateFrom(any()) }
            } else {
                verify { blockSenderDialogReducer wasNot Called }
            }

            if (reducesMarkAsLegitimateDialog) {
                verify { markAsLegitimateDialogReducer.newStateFrom(any()) }
            } else {
                verify { markAsLegitimateDialogReducer wasNot Called }
            }
            if (reducesEditScheduleSendDialog) {
                verify { editScheduledMessageDialogReducer.newStateFrom(any()) }
            } else {
                verify { editScheduledMessageDialogReducer wasNot Called }
            }
        }
    }

    @Test
    fun `initial valid scroll request produces scroll requested state with correct index`() = runTest {
        // Given
        val targetId = MessageIdUiModel("target-id")

        val initialState = ConversationDetailState.Loading.copy(
            scrollToMessageState = ScrollToMessageState.NoScrollTarget
        )

        val messages = listOf(
            ConversationDetailMessageUiModelSample.SepWeatherForecast,
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(messageId = targetId),
            ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(
                messageId = MessageIdUiModel("other")
            )
        ).toImmutableList()

        val op = ConversationDetailEvent.MessagesData(
            messagesUiModels = messages,
            requestScrollToMessageId = targetId,
            filterByLocation = null
        )

        // When
        val result = reducer.newStateFrom(initialState, op)

        // Then
        assertEquals(
            ScrollToMessageState.ScrollRequested(
                targetMessageId = targetId,
                targetMessageIndex = 1
            ),
            result.scrollToMessageState
        )
    }

    @Test
    fun `ignores new scroll requests after the initial scroll is initiated`() = runTest {
        // Given
        val existingTarget = MessageIdUiModel("existing-target")
        val incomingTarget = MessageIdUiModel("incoming-target")

        val initialState = ConversationDetailState.Loading.copy(
            scrollToMessageState = ScrollToMessageState.ScrollRequested(
                targetMessageId = existingTarget,
                targetMessageIndex = 1
            )
        )

        val op = ConversationDetailEvent.MessagesData(
            messagesUiModels = listOf(
                ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded.copy(messageId = incomingTarget)
            ).toImmutableList(),
            requestScrollToMessageId = incomingTarget,
            filterByLocation = null
        )

        // When
        val result = reducer.newStateFrom(initialState, op)

        // Then
        assertEquals(initialState.scrollToMessageState, result.scrollToMessageState)
    }

    @Test
    fun `stays in No scroll target state when scroll target does not exist in message list`() = runTest {
        // Given
        val missing = MessageIdUiModel("missing-id")

        val initialState = ConversationDetailState.Loading.copy(
            scrollToMessageState = ScrollToMessageState.NoScrollTarget
        )

        val op = ConversationDetailEvent.MessagesData(
            messagesUiModels = allMessagesFirstExpanded,
            requestScrollToMessageId = missing,
            filterByLocation = null
        )

        // When
        val result = reducer.newStateFrom(initialState, op)

        // Then
        assertEquals(ScrollToMessageState.NoScrollTarget, result.scrollToMessageState)
    }

    @Test
    fun `completes the scroll with the given message id`() = runTest {
        // Given
        val completedId = MessageIdUiModel("done-id")

        val initialState = ConversationDetailState.Loading.copy(
            scrollToMessageState = ScrollToMessageState.ScrollRequested(
                targetMessageId = completedId,
                targetMessageIndex = 0
            )
        )

        // When
        val result = reducer.newStateFrom(
            initialState,
            ConversationDetailViewAction.ScrollRequestCompleted(completedId)
        )

        // Then
        assertEquals(ScrollToMessageState.ScrollCompleted(completedId), result.scrollToMessageState)
    }

    data class TestInput(
        val operation: ConversationDetailOperation,
        val reducesConversation: Boolean,
        val reducesMessages: Boolean,
        val reducesBottomBar: Boolean,
        val reducesErrorBar: Boolean,
        val reducesMessageBar: Boolean,
        val reducesExit: Boolean,
        val reducesLoadingError: Boolean,
        val expectedExitMessage: ActionResult?,
        val reducesBottomSheet: Boolean,
        val reducesLinkClick: Boolean,
        val reducesMessageScroll: Boolean,
        val reducesDeleteDialog: Boolean,
        val reducesTrashedMessagesBanner: Boolean,
        val reducesReportPhishingDialog: Boolean,
        val reducesBlockSenderDialog: Boolean,
        val reducesMarkAsLegitimateDialog: Boolean,
        val reducesEditScheduleSendDialog: Boolean
    ) {

        fun operationAffectingBottomBar() = operation as ConversationDetailEvent.ConversationBottomBarEvent
        fun operationAffectingConversation() = operation as ConversationDetailOperation.AffectingConversation
        fun operationAffectingMessages() = operation as ConversationDetailOperation.AffectingMessages
    }

    private companion object {

        val messageId = MessageIdUiModel(UUID.randomUUID().toString())
        private val participant =
            ParticipantUiModel(
                "Test User",
                "test@proton.me",
                R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        val avatar = AvatarUiModel.ParticipantAvatar("TU", "test@proton.me", null, Color.Red)

        val actions = listOf(
            ConversationDetailViewAction.SnoozeDismissed affects listOf(BottomSheet),
            ConversationDetailViewAction.MarkRead affects listOf(BottomSheet),
            ConversationDetailViewAction.MarkUnread affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestConversationMoveToBottomSheet affects BottomSheet,
            ConversationDetailViewAction.MoveToCompleted(
                MailLabelText(""), MoveToBottomSheetEntryPoint.Conversation
            ) affects listOf(BottomSheet, MessageBar),
            ConversationDetailViewAction.Star affects listOf(Conversation, BottomSheet),
            ConversationDetailViewAction.UnStar affects listOf(Conversation, BottomSheet),
            ConversationDetailViewAction.RequestConversationLabelAsBottomSheet affects BottomSheet,
            ConversationDetailViewAction.RequestContactActionsBottomSheet(
                participant, avatar, null
            ) affects BottomSheet,
            ConversationDetailViewAction.LabelAsCompleted(
                wasArchived = false,
                entryPoint = LabelAsBottomSheetEntryPoint.Conversation
            ) affects BottomSheet,
            ConversationDetailViewAction.MessageBodyLinkClicked(messageId, mockk()) affects LinkClick,
            ConversationDetailViewAction.DeleteConfirmed affects listOf(
                DeleteDialog,
                BottomSheet
            ),
            ConversationDetailViewAction.SwitchViewMode(
                MessageId(messageId.id), MessageTheme.Dark, MessageTheme.Light
            ) affects listOf(BottomSheet),
            ConversationDetailViewAction.MarkMessageUnread(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestMessageLabelAsBottomSheet(MessageId(messageId.id)) affects BottomSheet,
            ConversationDetailViewAction.MoveMessage.System.Trash(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.MoveMessage.System.Archive(MessageId(messageId.id))
                affects listOf(BottomSheet),
            ConversationDetailViewAction.MoveMessage.System.Spam(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.MoveMessage.System.Inbox(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.StarMessage(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.UnStarMessage(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestMessageMoveToBottomSheet(
                MessageId(messageId.id)
            ) affects listOf(BottomSheet),
            ConversationDetailViewAction.ReportPhishing(
                MessageId(messageId.id)
            ) affects listOf(BottomSheet, ReportPhishingDialog),
            ConversationDetailViewAction.ReportPhishingConfirmed(
                MessageId(messageId.id)
            ) affects listOf(ReportPhishingDialog),
            ConversationDetailViewAction.ReportPhishingDismissed affects listOf(ReportPhishingDialog),
            ConversationDetailViewAction.BlockSender(
                MessageIdUiModel(messageId.id), participant.participantAddress, null
            ) affects listOf(BottomSheet, BlockSenderDialog),
            ConversationDetailViewAction.BlockSenderConfirmed(
                MessageIdUiModel(messageId.id), participant.participantAddress
            ) affects listOf(BlockSenderDialog),
            ConversationDetailViewAction.BlockSenderDismissed affects listOf(BlockSenderDialog),
            ConversationDetailViewAction.MarkMessageAsLegitimate(
                MessageId(messageId.id), isPhishing = true
            ) affects listOf(MarkAsLegitimateDialog),
            ConversationDetailViewAction.MarkMessageAsLegitimateConfirmed(
                MessageId(messageId.id)
            ) affects listOf(MarkAsLegitimateDialog),
            ConversationDetailViewAction.MarkMessageAsLegitimateDismissed affects listOf(MarkAsLegitimateDialog),
            ConversationDetailViewAction.EditScheduleSendMessageDismissed affects listOf(EditScheduleSendDialog),
            ConversationDetailViewAction.EditScheduleSendMessageRequested(messageId)
                affects listOf(EditScheduleSendDialog),
            ConversationDetailViewAction.EditScheduleSendMessageConfirmed(messageId)
                affects listOf(EditScheduleSendDialog, Messages),
            ConversationDetailViewAction.PrintMessage(mockk(), MessageId(messageId.id))
                affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestBlockedTrackersBottomSheet(null) affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestEncryptionInfoBottomSheet(
                EncryptionInfoUiModelSample.StoredWithZeroAccessEncryption
            ) affects listOf(BottomSheet)
        )

        val attachmentId = AttachmentId("test-attachment-id")

        val events = listOf(
            ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions) affects BottomBar,
            ConversationDetailEvent.ConversationData(
                ConversationDetailMetadataUiModelSample.WeatherForecast,
                HiddenMessagesBanner.ContainsTrashedMessages,
                showAllMessages = false
            )
                affects listOf(Conversation, HiddenMessagesBanner),
            ConversationDetailEvent.ErrorAddStar affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorRemoveStar affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorLoadingConversation affects LoadingError,
            ConversationDetailEvent.ErrorLoadingMessages affects LoadingError,
            ConversationDetailEvent.ErrorMarkingAsUnread affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorMarkingAsRead affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorMovingConversation affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.ErrorMovingMessage affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorMovingToTrash affects listOf(ErrorBar, BottomSheet),
            ConversationDetailEvent.ErrorLabelingConversation affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.MessagesData(
                emptyList<ConversationDetailMessageUiModel>().toImmutableList(),
                null,
                null
            ) affects listOf(Messages),
            ConversationDetailEvent.MessagesData(
                allMessagesFirstExpanded,
                allMessagesFirstExpanded.first().messageId,
                null
            ) affects listOf(Messages, MessageScroll),
            ConversationDetailEvent.ExpandDecryptedMessage(
                MessageIdUiModel(UUID.randomUUID().toString()),
                ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded
            ) affects Messages,
            ConversationDetailEvent.CollapseDecryptedMessage(
                MessageIdUiModel(UUID.randomUUID().toString()),
                ConversationDetailMessageUiModelSample.AugWeatherForecast
            ) affects Messages,
            ConversationDetailEvent.ErrorExpandingDecryptMessageError(
                MessageIdUiModel(UUID.randomUUID().toString())
            ) affects listOf(ErrorBar, Messages),
            ConversationDetailEvent.ErrorExpandingRetrieveMessageError(
                MessageIdUiModel(UUID.randomUUID().toString())
            ) affects listOf(ErrorBar, Messages),
            ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline(
                MessageIdUiModel(UUID.randomUUID().toString())
            ) affects listOf(ErrorBar, Messages),
            ConversationDetailEvent.ErrorGettingAttachment affects ErrorBar,
            ConversationDetailEvent.ErrorDeletingConversation affects listOf(ErrorBar, BottomSheet, DeleteDialog),
            ConversationDetailEvent.ErrorDeletingMessage affects listOf(ErrorBar, BottomSheet, DeleteDialog),
            ConversationDetailEvent.ExitScreen affects listOf(Exit, BottomSheet),
            ConversationDetailEvent.ExitScreenWithMessage(
                ConversationDetailViewAction.MoveToTrash
            ) affects listOf(
                BottomSheet,
                ExitWithResult(
                    UndoableActionResult(TextUiModel(string.conversation_moved_to_trash))
                )
            ),
            ConversationDetailEvent.ExitScreenWithMessage(
                ConversationDetailViewAction.DeleteConfirmed
            ) affects listOf(
                BottomSheet,
                ExitWithResult(DefinitiveActionResult(TextUiModel(string.conversation_deleted)))
            ),
            ConversationDetailEvent.LastMessageMoved(MailLabelText("String")) affects listOf(
                BottomSheet,
                ExitWithResult(
                    UndoableActionResult(TextUiModel.TextResWithArgs(string.message_moved_to, listOf("String")))
                )
            ),
            ConversationDetailEvent.LastMessageDeleted affects listOf(
                BottomSheet,
                ExitWithResult(
                    DefinitiveActionResult(TextUiModel.TextRes(string.message_deleted))
                )
            ),
            ConversationDetailEvent.MessageMoved(MailLabelText("String")) affects listOf(BottomSheet, MessageBar),
            ConversationDetailEvent.ErrorMovingMessage affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.ErrorUnsnoozing affects listOf(ErrorBar),
            ConversationDetailViewAction.SnoozeCompleted("String") affects listOf(BottomSheet, MessageBar),
            ConversationDetailEvent.AttachmentDownloadStarted(attachmentId) affects listOf()
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (actions + events)
            .map { operation ->
                val testName = "Operation: $operation"
                arrayOf(testName, operation)
            }
    }
}

private infix fun ConversationDetailOperation.affects(entities: List<Entity>) = ConversationDetailReducerTest.TestInput(
    operation = this,
    reducesConversation = entities.contains(Conversation),
    reducesMessages = entities.contains(Messages),
    reducesBottomBar = entities.contains(BottomBar),
    reducesErrorBar = entities.contains(ErrorBar),
    reducesExit = entities.contains(Exit),
    reducesLoadingError = entities.contains(LoadingError),
    reducesMessageBar = entities.contains(MessageBar),
    expectedExitMessage = entities.firstNotNullOfOrNull { (it as? ExitWithResult)?.result },
    reducesBottomSheet = entities.contains(BottomSheet),
    reducesLinkClick = entities.contains(LinkClick),
    reducesMessageScroll = entities.contains(MessageScroll),
    reducesDeleteDialog = entities.contains(DeleteDialog),
    reducesTrashedMessagesBanner = entities.contains(HiddenMessagesBanner),
    reducesReportPhishingDialog = entities.contains(ReportPhishingDialog),
    reducesBlockSenderDialog = entities.contains(BlockSenderDialog),
    reducesMarkAsLegitimateDialog = entities.contains(MarkAsLegitimateDialog),
    reducesEditScheduleSendDialog = entities.contains(EditScheduleSendDialog)
)

private infix fun ConversationDetailOperation.affects(entity: Entity) = this.affects(listOf(entity))

private sealed interface Entity
private data object Messages : Entity
private data object Conversation : Entity
private data object BottomBar : Entity
private data object Exit : Entity
private data object LoadingError : Entity
private data class ExitWithResult(val result: ActionResult) : Entity
private data object MessageBar : Entity
private data object ErrorBar : Entity
private data object BottomSheet : Entity
private data object LinkClick : Entity
private data object MessageScroll : Entity
private data object DeleteDialog : Entity
private data object HiddenMessagesBanner : Entity
private data object ReportPhishingDialog : Entity
private data object BlockSenderDialog : Entity
private data object MarkAsLegitimateDialog : Entity
private data object EditScheduleSendDialog : Entity

private val allMessagesFirstExpanded = listOf(
    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
    ConversationDetailMessageUiModelSample.SepWeatherForecast
).toImmutableList()
