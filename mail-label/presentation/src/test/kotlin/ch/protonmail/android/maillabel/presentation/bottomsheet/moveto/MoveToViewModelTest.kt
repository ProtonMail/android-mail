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

package ch.protonmail.android.maillabel.presentation.bottomsheet.moveto

import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class MoveToViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val getMoveToLocations = mockk<GetMoveToLocations>()
    private val moveMessages = mockk<MoveMessages>()
    private val moveConversations = mockk<MoveConversations>()
    private val reducer = spyk<MoveToReducer>()

    @BeforeTest
    fun setup() {
        every { observePrimaryUserId() } returns flowOf(userId)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit an error when no items are provided`() = runTest {
        // Given
        val initialData = defaultInitialData.copy(items = emptyList())

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(MoveToState.Error, awaitItem())
        }
    }

    @Test
    fun `should emit move to data error when data can't be fetched`() = runTest {
        // Given
        val viewMode = ViewMode.NoConversationGrouping
        val initialData =
            defaultInitialData.copy(
                entryPoint = MoveToBottomSheetEntryPoint.Mailbox.SelectionMode(
                    itemCount = 1,
                    viewMode = viewMode
                )
            )

        coEvery {
            getMoveToLocations.forMailbox(userId, labelId, items, viewMode)
        } returns DataError.Local.NoDataCached.left()

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(MoveToState.Error, awaitItem())
        }
    }

    @Test
    fun `should emit move to data loaded for conversation`() = runTest {
        // Given
        val initialData = defaultInitialData.copy(entryPoint = MoveToBottomSheetEntryPoint.Conversation)
        val conversationId = ConversationId("item1")
        expectLoadedDataForConversation(conversationId)

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(defaultDataState, awaitItem())
        }
    }

    @Test
    fun `should emit move to data loaded for message`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val initialData = defaultInitialData.copy(
            entryPoint = MoveToBottomSheetEntryPoint.Message(messageId)
        )
        expectLoadedDataForMessage(messageId, initialData.entryPoint)

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(defaultDataState, awaitItem())
        }
    }

    @Test
    fun `should emit move to data loaded for selection mode`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val entryPoint = MoveToBottomSheetEntryPoint.Mailbox.SelectionMode(
            itemCount = 1,
            viewMode = ViewMode.NoConversationGrouping
        )
        val initialData = defaultInitialData.copy(entryPoint = entryPoint)
        val labelAsItemId = MoveToItemId(messageId.id)
        expectLoadedDataForMailbox(entryPoint, listOf(labelAsItemId))

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(defaultDataState, awaitItem())
        }
    }

    @Test
    fun `should emit move to data loaded for swipe action`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val labelAsItemId = MoveToItemId(messageId.id)
        val initialData = defaultInitialData.copy(
            entryPoint = MoveToBottomSheetEntryPoint.Mailbox.MoveToSwipeAction(
                ViewMode.ConversationGrouping,
                labelAsItemId
            )
        )

        expectLoadedDataForMailbox(
            initialData.entryPoint as MoveToBottomSheetEntryPoint.Mailbox,
            listOf(labelAsItemId)
        )

        // When + Then
        viewModel(initialData).state.test {
            assertEquals(defaultDataState, awaitItem())
        }
    }

    @Test
    fun `should trigger move to as action and update state on operation confirmed (conversation)`() = runTest {
        // Given
        val conversationId = ConversationId("item1")
        val labelId = MailLabelId.Custom.Folder(labelId)
        val mailLabelText = MailLabelText("Text")

        val uiModel = MoveToBottomSheetDestinationUiModel.Custom(
            id = labelId,
            text = TextUiModel("Text"),
            icon = 1,
            iconTint = null,
            iconPaddingStart = 0.dp
        )

        val initialState = MoveToState.Data(
            entryPoint = defaultInitialData.entryPoint,
            customDestinations = listOf(uiModel).toImmutableList(),
            systemDestinations = emptyList<MoveToBottomSheetDestinationUiModel.System>().toImmutableList(),
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )

        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))

        expectLoadedDataForConversation(conversationId = conversationId, initialState = initialState)

        coEvery {
            moveConversations.invoke(
                any<UserId>(),
                any<List<ConversationId>>(),
                any<LabelId>()
            )
        } returns Unit.right()

        // When
        val confirmAction =
            MoveToOperation.MoveToAction.MoveToDestinationSelected(mailLabelId = labelId, mailLabelText = mailLabelText)

        // Then
        verifyConversationMove(
            defaultInitialData,
            initialState,
            confirmAction,
            updatedState,
            conversationId
        )
    }

    @Test
    fun `should trigger move to action and update state on operation confirmed (message)`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val labelId = MailLabelId.System(labelId)
        val mailLabelText = MailLabelText("Text")

        val uiModel = MoveToBottomSheetDestinationUiModel.System(
            id = labelId,
            text = TextUiModel("Text"),
            icon = 1,
            iconTint = null
        )

        val initialData = defaultInitialData.copy(
            entryPoint = MoveToBottomSheetEntryPoint.Message(messageId)
        )

        val initialState = MoveToState.Data(
            entryPoint = initialData.entryPoint,
            customDestinations = emptyList<MoveToBottomSheetDestinationUiModel.Custom>().toImmutableList(),
            systemDestinations = listOf(uiModel).toImmutableList(),
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )

        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))
        expectLoadedDataForMessage(messageId, initialState = initialState)

        coEvery {
            moveMessages.invoke(any<UserId>(), any<List<MessageId>>(), any<LabelId>())
        } returns Unit.right()

        // When
        val action =
            MoveToOperation.MoveToAction.MoveToDestinationSelected(mailLabelId = labelId, mailLabelText = mailLabelText)

        // Then
        verifyMessageMove(initialData, initialState, action, updatedState, messageId)
    }

    @Test
    fun `should trigger move to action and update state on operation confirmed (swipe action, message)`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val labelId = MailLabelId.Custom.Folder(labelId)

        val mailLabelText = MailLabelText("Text")

        val uiModel = MoveToBottomSheetDestinationUiModel.Custom(
            id = labelId,
            text = TextUiModel("Text"),
            icon = 1,
            iconTint = null,
            iconPaddingStart = 0.dp
        )

        val items = listOf(MoveToItemId(messageId.id))

        val entryPoint =
            MoveToBottomSheetEntryPoint.Mailbox.MoveToSwipeAction(ViewMode.NoConversationGrouping, items.first())
        val initialData = defaultInitialData.copy(entryPoint = entryPoint)

        val initialState = MoveToState.Data(
            entryPoint = initialData.entryPoint,
            customDestinations = listOf(uiModel).toImmutableList(),
            systemDestinations = emptyList<MoveToBottomSheetDestinationUiModel.System>().toImmutableList(),
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )

        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))
        expectLoadedDataForMailbox(entryPoint, items, initialState = initialState)

        coEvery {
            moveMessages.invoke(any<UserId>(), any<List<MessageId>>(), any<LabelId>())
        } returns Unit.right()

        // When
        val action =
            MoveToOperation.MoveToAction.MoveToDestinationSelected(mailLabelId = labelId, mailLabelText = mailLabelText)

        // Then
        verifyMessageMove(initialData, initialState, action, updatedState, messageId)
    }

    @Test
    fun `should trigger label as action and update state on operation confirmed (swipe action, convo)`() = runTest {
        // Given
        val conversationId = ConversationId("item1")
        val labelId = MailLabelId.System(labelId)
        val mailLabelText = MailLabelText("Text")

        val uiModel = MoveToBottomSheetDestinationUiModel.System(
            id = labelId,
            text = TextUiModel(mailLabelText.toString()),
            icon = 1,
            iconTint = null
        )

        val initialState = MoveToState.Data(
            entryPoint = defaultInitialData.entryPoint,
            customDestinations = emptyList<MoveToBottomSheetDestinationUiModel.Custom>().toImmutableList(),
            systemDestinations = listOf(uiModel).toImmutableList(),
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )

        val entryPoint =
            MoveToBottomSheetEntryPoint.Mailbox.MoveToSwipeAction(ViewMode.ConversationGrouping, items.first())
        val initialData = defaultInitialData.copy(entryPoint = entryPoint)

        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))
        expectLoadedDataForMailbox(entryPoint, items, initialState = initialState)

        coEvery {
            moveConversations.invoke(
                any<UserId>(),
                any<List<ConversationId>>(),
                any<LabelId>()
            )
        } returns Unit.right()

        // When
        val action =
            MoveToOperation.MoveToAction.MoveToDestinationSelected(mailLabelId = labelId, mailLabelText = mailLabelText)

        // Then
        verifyConversationMove(initialData, initialState, action, updatedState, conversationId)
    }

    @Test
    fun `should trigger move to action and update state on category selected (conversation)`() = runTest {
        // Given
        val conversationId = ConversationId("item1")
        val category = CategorySystemLabelId.Social
        val mailLabelText = MailLabelText("Social")
        val inboxUiModel = MoveToBottomSheetDestinationUiModel.Inbox(
            id = MailLabelId.System(labelId),
            text = TextUiModel("Inbox"),
            icon = 1,
            iconTint = null,
            categories = emptyList()
        )
        val initialState = MoveToState.Data(
            entryPoint = defaultInitialData.entryPoint,
            customDestinations = emptyList<MoveToBottomSheetDestinationUiModel.Custom>().toImmutableList(),
            systemDestinations = emptyList<MoveToBottomSheetDestinationUiModel.System>().toImmutableList(),
            inboxDestination = inboxUiModel,
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )
        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))

        expectLoadedDataForConversation(conversationId = conversationId, initialState = initialState)

        coEvery {
            moveConversations.invoke(any<UserId>(), any<List<ConversationId>>(), any<LabelId>())
        } returns Unit.right()

        // When
        val action = MoveToOperation.MoveToAction.MoveToDestinationSelected(
            mailLabelId = MailLabelId.Category(category.labelId),
            mailLabelText = mailLabelText
        )

        // Then
        verifyConversationMove(
            initialData = defaultInitialData,
            initialState = initialState,
            action = action,
            updatedState = updatedState,
            conversationId = conversationId,
            expectedLabelId = category.labelId
        )
    }

    @Test
    fun `should trigger move to action and update state on category selected (message)`() = runTest {
        // Given
        val messageId = MessageId("item1")
        val category = ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId.Promotions
        val mailLabelText = MailLabelText("Promotions")
        val initialData = defaultInitialData.copy(entryPoint = MoveToBottomSheetEntryPoint.Message(messageId))

        val initialState = MoveToState.Data(
            entryPoint = initialData.entryPoint,
            customDestinations = emptyList<MoveToBottomSheetDestinationUiModel.Custom>().toImmutableList(),
            systemDestinations = emptyList<MoveToBottomSheetDestinationUiModel.System>().toImmutableList(),
            shouldDismissEffect = Effect.empty(),
            errorEffect = Effect.empty()
        )

        val dismissData = MoveToState.MoveToDismissData(mailLabelText)
        val updatedState = initialState.copy(shouldDismissEffect = Effect.of(dismissData))
        expectLoadedDataForMessage(messageId, initialState = initialState)

        coEvery {
            moveMessages.invoke(any<UserId>(), any<List<MessageId>>(), any<LabelId>())
        } returns Unit.right()

        // When
        val action = MoveToOperation.MoveToAction.MoveToDestinationSelected(
            mailLabelId = MailLabelId.Category(category.labelId),
            mailLabelText = mailLabelText
        )

        // Then
        verifyMessageMove(
            initialData = initialData,
            initialState = initialState,
            action = action,
            updatedState = updatedState,
            messageId = messageId,
            expectedLabelId = category.labelId
        )
    }

    private fun viewModel(initialData: MoveToBottomSheet.InitialData) = MoveToViewModel(
        initialData = initialData,
        observePrimaryUserId = observePrimaryUserId,
        getMoveToLocations = getMoveToLocations,
        moveMessages = moveMessages,
        moveConversations = moveConversations,
        reducer = reducer
    )

    private suspend fun verifyMessageMove(
        initialData: MoveToBottomSheet.InitialData,
        initialState: MoveToState.Data,
        action: MoveToOperation.MoveToAction,
        updatedState: MoveToState.Data,
        messageId: MessageId,
        expectedLabelId: LabelId = labelId
    ) {
        val viewModel = viewModel(initialData)
        viewModel.state.test {
            assertEquals(initialState, awaitItem())

            viewModel.submit(action)
            assertEquals(updatedState, awaitItem())
        }

        verify { moveConversations wasNot called }
        coVerify(exactly = 1) {
            moveMessages(
                userId = userId,
                messageIds = listOf(messageId),
                labelId = expectedLabelId
            )
        }
        confirmVerified(moveMessages, moveConversations)
    }

    private suspend fun verifyConversationMove(
        initialData: MoveToBottomSheet.InitialData,
        initialState: MoveToState.Data,
        action: MoveToOperation.MoveToAction,
        updatedState: MoveToState.Data,
        conversationId: ConversationId,
        expectedLabelId: LabelId = labelId
    ) {
        val viewModel = viewModel(initialData)
        viewModel.state.test {
            assertEquals(initialState, awaitItem())

            viewModel.submit(action)
            assertEquals(updatedState, awaitItem())
        }

        verify { moveMessages wasNot called }
        coVerify(exactly = 1) {
            moveConversations(userId, listOf(conversationId), expectedLabelId)
        }
        confirmVerified(moveMessages, moveConversations)
    }

    private fun expectLoadedDataForConversation(
        conversationId: ConversationId,
        entryPoint: MoveToBottomSheetEntryPoint = MoveToBottomSheetEntryPoint.Conversation,
        initialState: MoveToState = defaultDataState
    ) {
        setupInitialDataLoading(destinations = defaultDestinations, entryPoint = entryPoint, state = initialState)

        coEvery {
            getMoveToLocations.forConversation(
                userId,
                labelId, conversationId
            )
        } returns defaultDestinations.right()
    }

    private fun expectLoadedDataForMessage(
        messageId: MessageId,
        entryPoint: MoveToBottomSheetEntryPoint = MoveToBottomSheetEntryPoint.Message(messageId),
        initialState: MoveToState = defaultDataState
    ) {
        setupInitialDataLoading(destinations = defaultDestinations, entryPoint = entryPoint, state = initialState)

        coEvery {
            getMoveToLocations.forMessage(userId, labelId, messageId)
        } returns defaultDestinations.right()
    }

    private fun expectLoadedDataForMailbox(
        entryPoint: MoveToBottomSheetEntryPoint.Mailbox,
        items: List<MoveToItemId>,
        initialState: MoveToState = defaultDataState
    ) {
        setupInitialDataLoading(defaultDestinations, entryPoint, initialState)

        coEvery {
            getMoveToLocations.forMailbox(userId, labelId, items, entryPoint.viewMode)
        } returns defaultDestinations.right()
    }

    private fun setupInitialDataLoading(
        destinations: List<MailLabel>,
        entryPoint: MoveToBottomSheetEntryPoint,
        state: MoveToState
    ) {
        every {
            reducer.newStateFrom(
                contentState = MoveToState.Loading,
                event = MoveToOperation.MoveToEvent.InitialData(destinations, entryPoint)
            )
        } returns state
    }

    private companion object {

        val userId = UserId("userId")
        val labelId = LabelId("labelId")
        val items = listOf(MoveToItemId("item1"))

        val defaultDataState = mockk<MoveToState.Data>()

        val defaultInitialData = MoveToBottomSheet.InitialData(
            userId = userId,
            currentLocationLabelId = labelId,
            items = items,
            entryPoint = MoveToBottomSheetEntryPoint.Conversation
        )

        val defaultDestinations = listOf<MailLabel>(
            MailLabelTestData.archiveSystemLabel,
            MailLabelTestData.label2021
        )
    }
}
