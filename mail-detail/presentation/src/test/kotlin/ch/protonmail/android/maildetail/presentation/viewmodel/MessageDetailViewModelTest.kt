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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelMessage
import ch.protonmail.android.maildetail.domain.usecase.StarMessage
import ch.protonmail.android.maildetail.domain.usecase.UnStarMessage
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.maildetail.presentation.reducer.LabelAsBottomSheetReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageBodyReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData
import ch.protonmail.android.testdata.message.MessageAttachmentTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageDetailViewModelTest {

    private val rawMessageId = "detailMessageId"
    private val decryptedMessageBody = DecryptedMessageBody("Decrypted message body.", MimeType.PlainText)
    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageDetailActionBarUiModelMapper = MessageDetailActionBarUiModelMapper()
    private val messageDetailReducer = MessageDetailReducer(
        MessageDetailMetadataReducer(),
        MessageBodyReducer(),
        BottomBarReducer(),
        BottomSheetReducer(MoveToBottomSheetReducer(), LabelAsBottomSheetReducer())
    )
    private val defaultFolderColorSettings = FolderColorSettings()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(userId, any()) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message,
                emptyList()
            ).right()
        )
    }
    private val getDecryptedMessageBody = mockk<GetDecryptedMessageBody> {
        coEvery { this@mockk(userId, any()) } returns decryptedMessageBody.right()
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns rawMessageId
    }
    private val observeDetailActions = mockk<ObserveMessageDetailActions> {
        every { this@mockk.invoke(userId, MessageId(rawMessageId)) } returns flowOf(
            nonEmptyListOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observeMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
        every { this@mockk.invoke(userId) } returns flowOf(
            MailLabels(
                systemLabels = listOf(MailLabel.System(MailLabelId.System.Spam)),
                folders = listOf(buildCustomFolder(id = "folder1")),
                labels = listOf()
            )
        )
    }
    private val observeFolderColorSettings =
        mockk<ObserveFolderColorSettings> {
            every { this@mockk.invoke(userId) } returns flowOf(defaultFolderColorSettings)
        }
    private val observeCustomMailLabels = mockk<ObserveCustomMailLabels> {
        every { this@mockk.invoke(userId) } returns flowOf(
            listOf(
                MailLabelTestData.customLabelOne,
                MailLabelTestData.customLabelTwo
            ).right()
        )
    }
    private val observeAttachmentWorkerStatus = mockk<ObserveMessageAttachmentStatus>()
    private val markUnread = mockk<MarkMessageAsUnread> {
        coEvery { this@mockk(userId, MessageId(rawMessageId)) } returns MessageSample.Invoice.right()
    }
    private val markRead = mockk<MarkMessageAsRead> {
        coEvery { this@mockk(userId, MessageId(rawMessageId)) } returns MessageSample.Invoice.right()
    }
    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(userId) } returns ContactTestData.contacts.right()
    }
    private val starMessage = mockk<StarMessage> {
        coEvery { this@mockk.invoke(userId, MessageId(rawMessageId)) } returns MessageTestData.starredMessage.right()
    }
    private val unStarMessage = mockk<UnStarMessage> {
        coEvery { this@mockk.invoke(userId, MessageId(rawMessageId)) } returns MessageTestData.message.right()
    }
    private val messageDetailHeaderUiModelMapper = mockk<MessageDetailHeaderUiModelMapper> {
        coEvery {
            toUiModel(
                any(),
                ContactTestData.contacts,
                defaultFolderColorSettings
            )
        } returns messageDetailHeaderUiModel
    }
    private val messageBodyUiModelMapper = mockk<MessageBodyUiModelMapper> {
        coEvery {
            toUiModel(userId, decryptedMessageBody)
        } returns MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        every {
            toUiModel(MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY)
        } returns MessageBodyUiModelTestData.plainTextMessageBodyUiModel
    }
    private val moveMessage: MoveMessage = mockk {
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = MessageId(rawMessageId),
                labelId = SystemLabelId.Trash.labelId
            )
        } returns
            with(MessageSample) { Invoice.moveTo(LabelIdSample.Trash) }.right()
    }
    private val relabelMessage: RelabelMessage = mockk {
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = MessageId(rawMessageId),
                currentLabelIds = any(),
                updatedLabelIds = any()
            )
        } returns with(MessageSample) {
            Invoice.labelAs(
                listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        }.right()
    }
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val getDownloadingAttachmentsForMessages = mockk<GetDownloadingAttachmentsForMessages>()
    private val getEmbeddedImageAvoidDuplicatedExecution = mockk<GetEmbeddedImageAvoidDuplicatedExecution>()

    private val viewModel by lazy {
        MessageDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            messageDetailReducer = messageDetailReducer,
            observeMessageWithLabels = observeMessageWithLabels,
            getDecryptedMessageBody = getDecryptedMessageBody,
            actionUiModelMapper = actionUiModelMapper,
            observeDetailActions = observeDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            observeCustomMailLabels = observeCustomMailLabels,
            observeMessageAttachmentStatus = observeAttachmentWorkerStatus,
            markUnread = markUnread,
            markRead = markRead,
            getContacts = getContacts,
            starMessage = starMessage,
            unStarMessage = unStarMessage,
            savedStateHandle = savedStateHandle,
            messageDetailHeaderUiModelMapper = messageDetailHeaderUiModelMapper,
            messageBodyUiModelMapper = messageBodyUiModelMapper,
            messageDetailActionBarUiModelMapper = messageDetailActionBarUiModelMapper,
            moveMessage = moveMessage,
            relabelMessage = relabelMessage,
            getAttachmentIntentValues = getAttachmentIntentValues,
            getDownloadingAttachmentsForMessages = getDownloadingAttachmentsForMessages,
            getEmbeddedImageAvoidDuplicatedExecution = getEmbeddedImageAvoidDuplicatedExecution
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        viewModel.state.test {
            // Then
            assertEquals(MessageDetailState.Loading, awaitItem())
        }
    }

    @Test
    fun `no message state is emitted when there is no primary user`() = runTest {
        // Given
        givenNoLoggedInUser()

        // When
        viewModel.state.test {
            initialStateEmitted()
        }
    }

    @Test
    fun `throws exception when message id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns null

        // Then
        val thrown = assertThrows(IllegalStateException::class.java) { viewModel.state }
        // Then
        assertEquals("No Message id given", thrown.message)
    }

    @Test
    fun `message state is data when use case returns message metadata`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        val subject = "message subject"
        val isStarred = true
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = subject,
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyEmitted()
            // Then
            val expected = MessageMetadataState.Data(
                MessageDetailActionBarUiModel(
                    subject,
                    isStarred
                ),
                messageDetailHeaderUiModel
            )
            assertEquals(expected, awaitItem().messageMetadataState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is data when use case returns body`() = runTest {
        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Data(
                MessageBodyUiModelTestData.plainTextMessageBodyUiModel
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is error when use case returns error`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Data(isNetworkError = false)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is network error when use case returns network error`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Remote.Http(NetworkError.NoNetwork)).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Data(isNetworkError = true)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is loading when reload action was triggered`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

        viewModel.state.test {
            initialStateEmitted()
            messageBodyLoadingErrorEmitted()

            // When
            viewModel.submit(MessageViewAction.Reload)

            // Then
            assertEquals(MessageBodyState.Loading, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is decryption error when use case returns decryption error`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Decryption(MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Decryption(
                MessageBodyUiModelTestData.plainTextMessageBodyUiModel
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = "message subject",
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())
        every { observeDetailActions.invoke(userId, MessageId(rawMessageId)) } returns flowOf(
            nonEmptyListOf(Action.Reply, Action.Archive).right()
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            // Then
            val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
            val expected = BottomBarState.Data(actionUiModels)
            assertEquals(expected, lastEmittedItem().bottomBarState)
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns no actions`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = "message subject",
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        every { observeDetailActions.invoke(userId, MessageId(rawMessageId)) } returns
            flowOf(DataError.Local.NoDataCached.left())

        // When
        viewModel.state.test {
            advanceUntilIdle()
            // Then
            val expected = BottomBarState.Error.FailedLoadingActions
            assertEquals(expected, lastEmittedItem().bottomBarState)
        }
    }

    @Test
    fun `message detail state is dismiss message screen when mark unread is successful`() = runTest {
        // Given
        coEvery { markUnread(userId, MessageId(rawMessageId)) } returns MessageSample.Invoice.right()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.MarkUnread)
            advanceUntilIdle()
            // Then
            assertNotNull(lastEmittedItem().exitScreenEffect.consume())
        }
    }

    @Test
    fun `message detail state is error marking unread when mark unread fails`() = runTest {
        // Given
        coEvery { markUnread(userId, MessageId(rawMessageId)) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            // When
            viewModel.submit(MessageViewAction.MarkUnread)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_mark_unread_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `starred message metadata is emitted when star action is successful`() = runTest {
        viewModel.state.test {
            advanceUntilIdle()
            // When
            viewModel.submit(MessageViewAction.Star)
            advanceUntilIdle()
            // Then
            val actual = assertIs<MessageMetadataState.Data>(lastEmittedItem().messageMetadataState)
            assertTrue(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `error starring message is emitted when star action fails`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery { starMessage.invoke(userId, messageId) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.Star)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_star_operation_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `unStarred message metadata is emitted when unStar action is successful`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        val messageWithLabels = MessageWithLabels(MessageTestData.starredMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        viewModel.state.test {
            advanceUntilIdle()
            // When
            viewModel.submit(MessageViewAction.UnStar)
            advanceUntilIdle()
            // Then
            val actual = assertIs<MessageMetadataState.Data>(lastEmittedItem().messageMetadataState)
            assertFalse(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `error unStarring message is emitted when unStar action fails`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery { unStarMessage.invoke(userId, messageId) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.UnStar)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_unstar_operation_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `when trash action is submitted, use case is called and success message is emitted`() = runTest {
        // Given
        val expectedMessage = TextUiModel(R.string.message_moved_to_trash)

        // when
        viewModel.submit(MessageViewAction.Trash)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { moveMessage(userId, MessageId(rawMessageId), SystemLabelId.Trash.labelId) }
            assertEquals(expectedMessage, awaitItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `when error moving to trash, error is emitted`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                MessageId(rawMessageId),
                SystemLabelId.Trash.labelId
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.Trash)
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_move_to_trash_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `verify order of emitted states when starring a message`() = runTest {
        // When
        viewModel.state.test {
            // Then
            initialStateEmitted()
            messageBodyEmitted()
            val dataState = MessageDetailState.Loading.copy(
                messageMetadataState = MessageMetadataState.Data(
                    MessageDetailActionBarUiModelTestData.uiModel,
                    messageDetailHeaderUiModel
                ),
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                )
            )
            assertEquals(dataState, awaitItem())
            val bottomState = dataState.copy(
                bottomBarState = BottomBarState.Data(
                    listOf(
                        ActionUiModelTestData.reply,
                        ActionUiModelTestData.archive,
                        ActionUiModelTestData.markUnread
                    )
                )
            )
            assertEquals(bottomState, awaitItem())
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.Star)
            val actual = assertIs<MessageMetadataState.Data>(awaitItem().messageMetadataState)
            assertTrue(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `selecting a move to destination emits MailLabelUiModel list with selected option`() = runTest {
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.RequestMoveToBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            advanceUntilIdle()
            val actual = assertIs<MoveToBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            assertTrue { actual.moveToDestinations.first { it.id == MailLabelId.System.Spam }.isSelected }
        }
    }

    @Test
    fun `verify move to is called and dismiss is set when destination gets confirmed`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                MessageId(rawMessageId),
                MailLabelId.System.Spam.labelId
            )
        } returns MessageSample.Invoice.right()

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.RequestMoveToBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationConfirmed("spam"))
            advanceUntilIdle()

            // Then
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerify { moveMessage.invoke(userId, MessageId(rawMessageId), MailLabelId.System.Spam.labelId) }
        }
    }

    @Test
    fun `when error moving a message, error is emitted`() = runTest {
        // Given
        coEvery { moveMessage(userId, MessageId(rawMessageId), any()) } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.MoveToDestinationConfirmed("spam"))
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_move_message_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `toggle a label emits LabelUiModelWithSelectedState list with selected option`() = runTest {
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            val actual = assertIs<LabelAsBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            assertTrue {
                actual.labelUiModelsWithSelectedState
                    .first { it.labelUiModel.id.labelId == MailLabelTestData.customLabelOne.id.labelId }
                    .selectedState == LabelSelectedState.Selected
            }
        }
    }

    @Test
    fun `verify relabel message is called when destination gets confirmed`() = runTest {
        // Given
        coEvery {
            relabelMessage(
                userId = userId,
                messageId = MessageId(rawMessageId),
                currentLabelIds = listOf(),
                updatedLabelIds = listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        } returns MessageSample.Invoice.right()

        // When
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelTwo.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsConfirmed(false))
            advanceUntilIdle()

            // Then
            assertNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerify {
                relabelMessage.invoke(
                    userId = userId,
                    messageId = MessageId(rawMessageId),
                    currentLabelIds = listOf(),
                    updatedLabelIds = listOf(
                        MailLabelTestData.customLabelOne.id.labelId,
                        MailLabelTestData.customLabelTwo.id.labelId
                    )
                )
            }
        }
    }

    @Test
    fun `verify relabel message and move to is called and dismiss is set when destination gets confirmed`() = runTest {
        // Given
        coEvery { moveMessage(userId, MessageId(rawMessageId), any()) } returns MessageSample.Invoice.right()

        coEvery {
            relabelMessage(
                userId = userId,
                messageId = MessageId(rawMessageId),
                currentLabelIds = listOf(),
                updatedLabelIds = listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        } returns MessageSample.Invoice.right()

        // When
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelTwo.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsConfirmed(true))
            advanceUntilIdle()

            // Then
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerifySequence {
                moveMessage(userId, MessageId(rawMessageId), MailLabelId.System.Archive.labelId)
                relabelMessage.invoke(
                    userId = userId,
                    messageId = MessageId(rawMessageId),
                    currentLabelIds = listOf(),
                    updatedLabelIds = listOf(
                        MailLabelTestData.customLabelOne.id.labelId,
                        MailLabelTestData.customLabelTwo.id.labelId
                    )
                )
            }
        }
    }

    @Test
    fun `when error relabeling a message, error is emitted`() = runTest {
        // Given
        coEvery {
            relabelMessage(
                userId = userId,
                messageId = MessageId(rawMessageId),
                currentLabelIds = any(),
                updatedLabelIds = any()
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
        advanceUntilIdle()
        viewModel.submit(MessageViewAction.LabelAsConfirmed(false))
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_relabel_message_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `should mark the message as read on message decryption`() = runTest {
        viewModel.state.test {
            // When
            initialStateEmitted()
            messageBodyEmitted()

            // Then
            coVerify { markRead.invoke(userId, MessageId(rawMessageId)) }
        }
    }

    @Test
    fun `should not mark the message as read if there is an error reading the message`() = runTest {
        viewModel.state.test {
            // Given
            val messageId = MessageId(rawMessageId)
            coEvery {
                getDecryptedMessageBody(userId, messageId)
            } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

            // When
            initialStateEmitted()
            messageBodyLoadingErrorEmitted()

            // Then
            coVerify(exactly = 0) { markRead.invoke(any(), any()) }
        }
    }

    @Test
    fun `state should contain uri when message body link is clicked`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val expected = Effect.of(uri)

        viewModel.state.test {
            initialStateEmitted()
            messageBodyEmitted()

            // When
            viewModel.submit(MessageViewAction.MessageBodyLinkClicked(uri))

            // Then
            assertEquals(expected, awaitItem().openMessageBodyLinkEffect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all attachments are shown when all attachments should be shown`() = runTest {
        // Given
        val expectedMessageBody = DecryptedMessageBody(
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithMultipleDots,
                MessageAttachmentTestData.image
            )
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel


        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            // When
            viewModel.submit(MessageViewAction.ShowAllAttachments)

            // Then
            val actualDataState = awaitItem().messageBodyState as MessageBodyState.Data
            assertEquals(4, actualDataState.messageBodyUiModel.attachments?.attachments?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `attachment metadata of all attachments is observed when messagebody with attachment loaded successfully`() =
        runTest {
            // Given
            val expectedMessageBody = DecryptedMessageBody(
                value = "Plain message body",
                mimeType = MimeType.PlainText,
                attachments = listOf(
                    MessageAttachmentTestData.invoice,
                    MessageAttachmentTestData.document,
                    MessageAttachmentTestData.documentWithMultipleDots,
                    MessageAttachmentTestData.image
                )
            )
            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel


            viewModel.state.test {
                initialStateEmitted()
                // When
                messageBodyWithAttachmentEmitted()

                // Then
                coVerifyOrder {
                    observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), AttachmentId("invoice"))
                    observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), AttachmentId("document"))
                    observeAttachmentWorkerStatus(
                        userId,
                        MessageId(rawMessageId),
                        AttachmentId("complicated.document.name")
                    )
                    observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), AttachmentId("image"))
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify get attachment is called when attachment is clicked`() = runTest {
        // Given
        val expectedIntentValues = OpenAttachmentIntentValues(mimeType = "application/pdf", uri = mockk())
        val expectedMessageBody = DecryptedMessageBody(
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithMultipleDots,
                MessageAttachmentTestData.image
            )
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), any())
        } returns flowOf()
        coEvery {
            getAttachmentIntentValues(userId, MessageId(rawMessageId), AttachmentId("invoice"))
        } returns expectedIntentValues.right()
        coEvery { getDownloadingAttachmentsForMessages(userId, listOf(MessageId(rawMessageId))) } returns listOf()

        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            // When
            viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
            val actualState = awaitItem()

            // Then
            coVerify { getAttachmentIntentValues(userId, MessageId(rawMessageId), AttachmentId("invoice")) }
            assertEquals(Effect.of(expectedIntentValues), actualState.openAttachmentEffect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify error is shown when getting attachment failed`() = runTest {
        // Given
        val expectedMessageBody = DecryptedMessageBody(
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentTestData.invoice,
                MessageAttachmentTestData.document,
                MessageAttachmentTestData.documentWithMultipleDots,
                MessageAttachmentTestData.image
            )
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), any())
        } returns flowOf()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } returns DataError.Local.NoDataCached.left()
        coEvery { getDownloadingAttachmentsForMessages(userId, listOf(MessageId(rawMessageId))) } returns listOf()

        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            // When
            viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
            val actualState = awaitItem()

            // Then
            assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_failed)), actualState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify get attachment is not called and error is shown when other attachment is currently downloading`() =
        runTest {
            // Given
            val expectedIntentValues = OpenAttachmentIntentValues(mimeType = "application/pdf", uri = mockk())
            val expectedMessageBody = DecryptedMessageBody(
                value = "Plain message body",
                mimeType = MimeType.PlainText,
                attachments = listOf(
                    MessageAttachmentTestData.invoice,
                    MessageAttachmentTestData.document,
                    MessageAttachmentTestData.documentWithMultipleDots,
                    MessageAttachmentTestData.image
                )
            )
            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
            coEvery {
                observeAttachmentWorkerStatus(userId, MessageId(rawMessageId), any())
            } returns flowOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())
            coEvery {
                getAttachmentIntentValues(userId, MessageId(rawMessageId), AttachmentId("invoice"))
            } returns expectedIntentValues.right()
            coEvery {
                getDownloadingAttachmentsForMessages(userId, listOf(MessageId(rawMessageId)))
            } returns listOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())

            viewModel.state.test {
                initialStateEmitted()
                messageBodyWithAttachmentEmitted()
                skipItems(6) // skip emitted attachment worker status

                // When
                viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
                val actualState = awaitItem()

                // Then
                coVerify { getAttachmentIntentValues wasNot Called }
                assertEquals(Effect.of(TextUiModel(R.string.error_attachment_download_in_progress)), actualState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `returns get embedded image result when getting was successful`() = runTest {
        // Given
        val contentId = "contentId"
        val byteArray = "I'm a byte array".toByteArray()
        val expectedResult = GetEmbeddedImageResult(byteArray, "image/png")
        coEvery {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId,
                MessageId(rawMessageId),
                contentId,
                any()
            )
        } returns expectedResult

        // When
        val actual = viewModel.loadEmbeddedImage(contentId)

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `returns null when get embedded image returned an error`() = runTest {
        // Given
        val contentId = "contentId"
        coEvery {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId,
                MessageId(rawMessageId),
                contentId,
                any()
            )
        } returns null

        // When
        val actual = viewModel.loadEmbeddedImage(contentId)

        // Then
        assertNull(actual)
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.initialStateEmitted() {
        assertEquals(MessageDetailState.Loading, awaitItem())
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                )
            ),
            awaitItem()
        )
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyWithAttachmentEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
                )
            ),
            awaitItem()
        )
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyLoadingErrorEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Error.Data(isNetworkError = false)
            ),
            awaitItem()
        )
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns flowOf(null)
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.lastEmittedItem(): MessageDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }

}
