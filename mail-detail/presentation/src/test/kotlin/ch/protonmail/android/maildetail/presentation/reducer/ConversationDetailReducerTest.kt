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
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class ConversationDetailReducerTest(
    @Suppress("unused") private val testName: String,
    private val testInput: TestInput
) {

    private val bottomBarReducer = mockk<BottomBarReducer>(relaxed = true)
    private val messagesReducer = mockk<ConversationDetailMessagesReducer>(relaxed = true)
    private val metadataReducer = mockk<ConversationDetailMetadataReducer>(relaxed = true)
    private val bottomSheetReducer = mockk<BottomSheetReducer>(relaxed = true)
    private val deleteDialogReducer = mockk<ConversationDeleteDialogReducer>(relaxed = true)
    private val reportPhishingDialogReducer = mockk<ConversationReportPhishingDialogReducer>(relaxed = true)
    private val trashedMessagesBannerReducer = mockk<TrashedMessagesBannerReducer>(relaxed = true)
    private val mailLabelTextMapper = MailLabelTextMapper(mockk())
    private val customizeToolbarSpotlightReducer = mockk<ConversationCustomizeToolbarSpotlightReducer>()
    private val reducer = ConversationDetailReducer(
        bottomBarReducer = bottomBarReducer,
        messagesReducer = messagesReducer,
        metadataReducer = metadataReducer,
        bottomSheetReducer = bottomSheetReducer,
        deleteDialogReducer = deleteDialogReducer,
        reportPhishingDialogReducer = reportPhishingDialogReducer,
        trashedMessagesBannerReducer = trashedMessagesBannerReducer,
        mailLabelTextMapper = mailLabelTextMapper,
        customizeToolbarSpotlightReducer = customizeToolbarSpotlightReducer
    )

    @Test
    fun `does call the correct sub-reducers`() {
        with(testInput) {
            val result = reducer.newStateFrom(ConversationDetailState.Loading, operation)

            if (reducesMessages) {
                verify { messagesReducer.newStateFrom(any(), operationAffectingMessages()) }
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
                assertNotNull(result.message.consume())
            } else {
                assertNull(result.message.consume())
            }

            if (reducesExit) {
                assertNotNull(result.exitScreenEffect.consume())
            } else {
                assertNull(result.exitScreenEffect.consume())
            }

            if (expectedExitMessage != null) {
                assertEquals(expectedExitMessage, result.exitScreenWithMessageEffect.consume())
            }

            if (reducesLinkClick) {
                assertNotNull(result.openMessageBodyLinkEffect.consume())
            } else {
                assertNull(result.openMessageBodyLinkEffect.consume())
            }

            if (reducesMessageScroll) {
                assertNotNull(result.scrollToMessage)
            } else {
                assertNull(result.scrollToMessage)
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
        }
    }

    data class TestInput(
        val operation: ConversationDetailOperation,
        val reducesConversation: Boolean,
        val reducesMessages: Boolean,
        val reducesBottomBar: Boolean,
        val reducesErrorBar: Boolean,
        val reducesMessageBar: Boolean,
        val reducesExit: Boolean,
        val expectedExitMessage: ActionResult?,
        val reducesBottomSheet: Boolean,
        val reducesLinkClick: Boolean,
        val reducesMessageScroll: Boolean,
        val reducesDeleteDialog: Boolean,
        val reducesTrashedMessagesBanner: Boolean,
        val reducesSpotlight: Boolean
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
        val avatar = AvatarUiModel.ParticipantInitial("TU")

        val actions = listOf(
            ConversationDetailViewAction.MarkUnread affects Exit,
            ConversationDetailViewAction.MoveToDestinationConfirmed(
                MailLabelText("spam"),
                MoveToBottomSheetEntryPoint.Conversation
            ) affects listOf(
                BottomSheet,
                ExitWithResult(
                    UndoableActionResult(TextUiModel(string.conversation_moved_to_selected_destination, "spam"))
                )
            ),
            ConversationDetailViewAction.MoveToDestinationConfirmed(
                MailLabelText("spam"), MoveToBottomSheetEntryPoint.Message(MessageId(messageId.id))
            ) affects BottomSheet,
            ConversationDetailViewAction.RequestMoveToBottomSheet affects BottomSheet,
            ConversationDetailViewAction.DismissBottomSheet affects BottomSheet,
            ConversationDetailViewAction.MoveToDestinationSelected(
                SystemLabelId.Archive.toMailLabelSystem().id
            ) affects BottomSheet,
            ConversationDetailViewAction.Star affects Conversation,
            ConversationDetailViewAction.Trash affects ExitWithResult(
                UndoableActionResult(TextUiModel(string.conversation_moved_to_trash))
            ),
            ConversationDetailViewAction.UnStar affects Conversation,
            ConversationDetailViewAction.RequestConversationLabelAsBottomSheet affects BottomSheet,
            ConversationDetailViewAction.RequestContactActionsBottomSheet(participant, avatar) affects BottomSheet,
            ConversationDetailViewAction.LabelAsToggleAction(LabelIdSample.Label2022) affects BottomSheet,
            ConversationDetailViewAction.LabelAsConfirmed(
                false,
                LabelAsBottomSheetEntryPoint.Conversation
            ) affects BottomSheet,
            ConversationDetailViewAction.LabelAsConfirmed(
                true,
                LabelAsBottomSheetEntryPoint.Conversation
            ) affects listOf(
                BottomSheet,
                ExitWithResult(DefinitiveActionResult(TextUiModel(string.conversation_moved_to_archive)))
            ),
            ConversationDetailViewAction.LabelAsConfirmed(
                true, LabelAsBottomSheetEntryPoint.Message(MessageId(messageId.id))
            ) affects BottomSheet,
            ConversationDetailViewAction.MessageBodyLinkClicked(messageId, mockk()) affects LinkClick,
            ConversationDetailViewAction.RequestScrollTo(messageId) affects MessageScroll,
            ConversationDetailViewAction.DeleteConfirmed affects listOf(
                DeleteDialog,
                ExitWithResult(DefinitiveActionResult(TextUiModel(string.conversation_deleted)))
            ),
            ConversationDetailViewAction.SwitchViewMode(
                MessageId(messageId.id), ViewModePreference.LightMode
            ) affects listOf(BottomSheet, Messages),
            ConversationDetailViewAction.PrintRequested(MessageId(messageId.id)) affects listOf(BottomSheet, Messages),
            ConversationDetailViewAction.MarkMessageUnread(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestMessageLabelAsBottomSheet(MessageId(messageId.id)) affects BottomSheet,
            ConversationDetailViewAction.MoveMessage.MoveToTrash(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.MoveMessage.MoveToArchive(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.MoveMessage.MoveToArchive(MessageId(messageId.id)) affects listOf(BottomSheet),
            ConversationDetailViewAction.RequestMessageMoveToBottomSheet(
                MessageId(messageId.id)
            ) affects listOf(BottomSheet)
        )

        val events = listOf(
            ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions) affects BottomBar,
            ConversationDetailEvent.ConversationData(ConversationDetailMetadataUiModelSample.WeatherForecast)
                affects Conversation,
            ConversationDetailEvent.ErrorAddStar affects ErrorBar,
            ConversationDetailEvent.ErrorRemoveStar affects ErrorBar,
            ConversationDetailEvent.ErrorLoadingConversation affects listOf(Conversation, Messages),
            ConversationDetailEvent.ErrorLoadingMessages affects Messages,
            ConversationDetailEvent.ErrorMarkingAsUnread affects ErrorBar,
            ConversationDetailEvent.ErrorMovingConversation affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.ErrorMovingMessage affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.ErrorMovingToTrash affects ErrorBar,
            ConversationDetailEvent.ErrorLabelingConversation affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.MessagesData(
                emptyList<ConversationDetailMessageUiModel>().toImmutableList(),
                emptyMap(),
                null,
                null,
                false
            ) affects listOf(Messages, TrashedMessagesBanner),
            ConversationDetailEvent.MessagesData(
                allMessagesFirstExpanded,
                emptyMap(),
                allMessagesFirstExpanded.first().messageId,
                null,
                false
            ) affects listOf(Messages, MessageScroll, TrashedMessagesBanner),
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
            ConversationDetailEvent.ErrorDeletingConversation affects listOf(ErrorBar, DeleteDialog),
            ConversationDetailEvent.ErrorDeletingNoApplicableFolder affects listOf(ErrorBar, DeleteDialog),
            ConversationDetailEvent.LastMessageMoved(MailLabelText("spam")) affects listOf(
                BottomSheet,
                ExitWithResult(
                    DefinitiveActionResult(TextUiModel.TextResWithArgs(string.message_moved_to, listOf("spam")))
                )
            ),
            ConversationDetailEvent.MessageMoved(MailLabelText("spam")) affects listOf(BottomSheet, MessageBar),
            ConversationDetailEvent.ErrorMovingMessage affects listOf(BottomSheet, ErrorBar),
            ConversationDetailEvent.RequestCustomizeToolbarSpotlight affects Spotlight
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
    reducesMessageBar = entities.contains(MessageBar),
    reducesExit = entities.contains(Exit),
    expectedExitMessage = entities.firstNotNullOfOrNull { (it as? ExitWithResult)?.result },
    reducesBottomSheet = entities.contains(BottomSheet),
    reducesLinkClick = entities.contains(LinkClick),
    reducesMessageScroll = entities.contains(MessageScroll),
    reducesDeleteDialog = entities.contains(DeleteDialog),
    reducesTrashedMessagesBanner = entities.contains(TrashedMessagesBanner),
    reducesSpotlight = entities.contains(Spotlight)
)

private infix fun ConversationDetailOperation.affects(entity: Entity) = this.affects(listOf(entity))

private sealed interface Entity
private data object Messages : Entity
private data object Conversation : Entity
private data object BottomBar : Entity
private data object Exit : Entity
private data class ExitWithResult(val result: ActionResult) : Entity
private data object ErrorBar : Entity
private data object MessageBar : Entity
private data object BottomSheet : Entity
private data object LinkClick : Entity
private data object MessageScroll : Entity
private data object DeleteDialog : Entity
private data object TrashedMessagesBanner : Entity
private data object Spotlight : Entity

private val allMessagesFirstExpanded = listOf(
    ConversationDetailMessageUiModelSample.AugWeatherForecastExpanded,
    ConversationDetailMessageUiModelSample.SepWeatherForecast
).toImmutableList()
