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

package ch.protonmail.android.maildetail.presentation.viewmodel

import android.text.format.Formatter
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetCurrentEpochTimeDuration
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitial
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.StarConversation
import ch.protonmail.android.maildetail.domain.usecase.UnStarConversation
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.DetailAvatarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageLocationUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ParticipantUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMessagesReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.LabelAsBottomSheetReducer
import ch.protonmail.android.maildetail.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.message.MessageAttachmentTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.Contact
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration

class ConversationDetailViewModelIntegrationTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast

    // region mock observe use cases
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeConversationMessagesWithLabels: ObserveConversationMessagesWithLabels = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithLabel,
                MessageWithLabelsSample.InvoiceWithTwoLabels
            ).right()
        )
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            listOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val observeMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
        every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(
            MailLabels(
                systemLabels = listOf(MailLabel.System(MailLabelId.System.Spam)),
                folders = listOf(MailLabelTestData.buildCustomFolder(id = "folder1")),
                labels = listOf()
            )
        )
    }
    private val observeFolderColorSettings =
        mockk<ObserveFolderColorSettings> {
            every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(FolderColorSettings())
        }
    private val observeCustomMailLabels = mockk<ObserveCustomMailLabels> {
        every { this@mockk.invoke(UserIdSample.Primary) } returns flowOf(
            MailLabelTestData.listOfCustomLabels.right()
        )
    }
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(UserIdSample.Primary, any()) } returns mockk()
    }
    // endregion

    // region mock action use cases
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val relabelConversation: RelabelConversation = mockk()
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns conversationId.id
    }
    private val starConversation: StarConversation = mockk()
    private val unStarConversation: UnStarConversation = mockk()
    private val getDecryptedMessageBody: GetDecryptedMessageBody = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns DecryptedMessageBody("", MimeType.Html).right()
    }
    private val getContacts: GetContacts = mockk {
        coEvery { this@mockk.invoke(any()) } returns emptyList<Contact>().right()
    }

    private val markMessageAsRead: MarkMessageAsRead = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns MessageSample.Invoice.right()
    }
    private val getCurrentEpochTimeDuration: GetCurrentEpochTimeDuration = mockk {
        coEvery { this@mockk.invoke() } returns Duration.parse("PT0S")
    }
    // endregion

    // region mappers
    private val actionUiModelMapper = ActionUiModelMapper()
    private val colorMapper = ColorMapper()
    private val resolveParticipantName = ResolveParticipantName()
    private val formatShortTime: FormatShortTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }
    private val formatExtendedTime: FormatExtendedTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }

    private
    val getInitial = GetInitial()

    private val conversationMessageMapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = DetailAvatarUiModelMapper(getInitial),
        expirationTimeMapper = ExpirationTimeMapper(getCurrentEpochTimeDuration),
        colorMapper = colorMapper,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = MessageLocationUiModelMapper(colorMapper),
        resolveParticipantName = resolveParticipantName,
        messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
            colorMapper = colorMapper,
            context = mockk(),
            detailAvatarUiModelMapper = DetailAvatarUiModelMapper(getInitial),
            formatExtendedTime = formatExtendedTime,
            formatShortTime = formatShortTime,
            messageLocationUiModelMapper = MessageLocationUiModelMapper(colorMapper),
            participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName),
            resolveParticipantName = resolveParticipantName
        ),
        messageBodyUiModelMapper = MessageBodyUiModelMapper()
    )

    private val conversationMetadataMapper = ConversationDetailMetadataUiModelMapper()
    // endregion

    private val reducer = ConversationDetailReducer(
        bottomBarReducer = BottomBarReducer(),
        metadataReducer = ConversationDetailMetadataReducer(),
        messagesReducer = ConversationDetailMessagesReducer(),
        bottomSheetReducer = BottomSheetReducer(
            moveToBottomSheetReducer = MoveToBottomSheetReducer(),
            labelAsBottomSheetReducer = LabelAsBottomSheetReducer()
        )
    )

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            actionUiModelMapper = actionUiModelMapper,
            conversationMessageMapper = conversationMessageMapper,
            conversationMetadataMapper = conversationMetadataMapper,
            markConversationAsUnread = markConversationAsUnread,
            moveConversation = move,
            relabelConversation = relabelConversation,
            observeContacts = observeContacts,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessagesWithLabels,
            observeDetailActions = observeConversationDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            observeCustomMailLabels = observeCustomMailLabels,
            reducer = reducer,
            savedStateHandle = savedStateHandle,
            starConversation = starConversation,
            unStarConversation = unStarConversation,
            getDecryptedMessageBody = getDecryptedMessageBody,
            markMessageAsRead = markMessageAsRead,
            observeMessageWithLabels = observeMessageWithLabels,
            getContacts = getContacts,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Formatter::formatShortFileSize)
        every { Formatter.formatShortFileSize(any(), any()) } returns "0"
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        unmockkStatic(Formatter::formatShortFileSize)
    }

    @Test
    fun `Should expand automatically the message in the conversation and handle attachments`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            value = "",
            mimeType = MimeType.Html,
            attachments = listOf(
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithReallyLongFileName,
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.image
            )
        ).right()

        viewModel.state.test {
            // when
            advanceUntilIdle()
            val newState = lastEmittedItem().messagesState as ConversationDetailsMessagesState.Data

            // then
            val expandedMessage = newState.messages.first { it.messageId == messages.first().messageId }
            assertIs<ConversationDetailMessageUiModel.Expanded>(expandedMessage)
            assertEquals(
                messages.first().messageBodyUiModel.attachments,
                expandedMessage.messageBodyUiModel.attachments
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<ConversationDetailState>.lastEmittedItem(): ConversationDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }
}