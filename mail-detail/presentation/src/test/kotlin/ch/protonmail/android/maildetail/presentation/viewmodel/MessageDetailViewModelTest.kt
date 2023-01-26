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
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.GetMessageBody
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.StarMessage
import ch.protonmail.android.maildetail.domain.usecase.UnStarMessage
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageBodyReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessageDetailViewModelTest {

    private val rawMessageId = "detailMessageId"
    private val decryptedMessageBody = DecryptedMessageBody("Decrypted message body.")
    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageDetailActionBarUiModelMapper = MessageDetailActionBarUiModelMapper()
    private val messageDetailReducer = MessageDetailReducer(
        MessageDetailMetadataReducer(),
        MessageBodyReducer(),
        BottomBarReducer(),
        BottomSheetReducer(MoveToBottomSheetReducer())
    )

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
    private val getMessageBody = mockk<GetMessageBody> {
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
            every { this@mockk.invoke(userId) } returns flowOf(FolderColorSettings())
        }
    private val markUnread = mockk<MarkMessageAsUnread> {
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
        every { toUiModel(any(), ContactTestData.contacts) } returns messageDetailHeaderUiModel
    }
    private val messageBodyUiModelMapper = mockk<MessageBodyUiModelMapper> {
        every { toUiModel(decryptedMessageBody) } returns MessageBodyUiModelTestData.messageBodyUiModel
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

    private val viewModel by lazy {
        MessageDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            messageDetailReducer = messageDetailReducer,
            observeMessageWithLabels = observeMessageWithLabels,
            getMessageBody = getMessageBody,
            actionUiModelMapper = actionUiModelMapper,
            observeDetailActions = observeDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            markUnread = markUnread,
            getContacts = getContacts,
            starMessage = starMessage,
            unStarMessage = unStarMessage,
            savedStateHandle = savedStateHandle,
            messageDetailHeaderUiModelMapper = messageDetailHeaderUiModelMapper,
            messageBodyUiModelMapper = messageBodyUiModelMapper,
            messageDetailActionBarUiModelMapper = messageDetailActionBarUiModelMapper,
            moveMessage = moveMessage
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
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
                MessageBodyUiModelTestData.messageBodyUiModel
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is error when use case returns error`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery { getMessageBody(userId, messageId) } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error(isNetworkError = false)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is network error when use case returns network error`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery { getMessageBody(userId, messageId) } returns DataError.Remote.Http(NetworkError.NoNetwork).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error(isNetworkError = true)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is loading when reload action was triggered`() = runTest {
        // Given
        val messageId = MessageId(rawMessageId)
        coEvery { getMessageBody(userId, messageId) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            initialStateEmitted()
            messageBodyErrorEmitted()

            // When
            viewModel.submit(MessageViewAction.Reload)

            // Then
            assertEquals(MessageBodyState.Loading, awaitItem().messageBodyState)
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
                messageBodyState = MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyUiModel)
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

    private suspend fun ReceiveTurbine<MessageDetailState>.initialStateEmitted() {
        assertEquals(MessageDetailState.Loading, awaitItem())
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Data(MessageBodyUiModelTestData.messageBodyUiModel)
            ),
            awaitItem()
        )
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyErrorEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Error(isNetworkError = false)
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
