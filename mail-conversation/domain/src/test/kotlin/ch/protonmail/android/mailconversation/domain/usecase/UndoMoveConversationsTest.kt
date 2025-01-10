package ch.protonmail.android.mailconversation.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoableOperationRepository
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetUndoableOperation
import ch.protonmail.android.mailcommon.domain.usecase.RegisterUndoableOperation
import ch.protonmail.android.mailcommon.domain.usecase.UndoLastOperation
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test

class UndoMoveConversationsTest {

    private val userId = UserIdSample.Primary
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val conversationRepository = mockk<ConversationRepository>()
    private val observeMailLabels = mockk<ObserveMailLabels>()
    private val observeExclusiveMailLabels = mockk<ObserveExclusiveMailLabels>()
    private val decrementUnreadCount: DecrementUnreadCount = mockk()
    private val incrementUnreadCount: IncrementUnreadCount = mockk()
    private val registerUndoableOperation = mockk<RegisterUndoableOperation>()
    private val undoableOperationRepository = mockk<UndoableOperationRepository>()

    private val moveConversations: MoveConversations = spyk(
        MoveConversations(
            conversationRepository = conversationRepository,
            observeExclusiveMailLabels = observeExclusiveMailLabels,
            observeMailLabels = observeMailLabels,
            incrementUnreadCount = incrementUnreadCount,
            decrementUnreadCount = decrementUnreadCount,
            registerUndoableOperation = registerUndoableOperation
        )
    )

    private val getUndoableOperation = GetUndoableOperation(undoableOperationRepository)
    private val undoLastOperation = UndoLastOperation(getUndoableOperation)

    @Test
    fun `when undo is called then move conversations is called to reverse the operation`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")
        val expectedConversations = listOf(ConversationSample.Newsletter, ConversationSample.AlphaAppFeedback)
        val conversationIds = expectedConversations.map { it.conversationId }
        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectRegisterUndoOperationSucceeds()
        // Mocking "do" (move) expected calls
        expectMoveSucceeds(destinationLabel, conversationIds, expectedConversations)
        expectObserveCachedConversationsSucceeds(conversationIds, expectedConversations)
        // Mocking "undo" (move back) expected calls
        expectObserveCachedConversationsSucceeds(listOf(ConversationIdSample.Newsletter))
        expectObserveCachedConversationsSucceeds(listOf(ConversationIdSample.AlphaAppFeedback))
        expectMoveSucceeds(LabelIdSample.Archive, listOf(ConversationIdSample.Newsletter))
        expectMoveSucceeds(LabelIdSample.Inbox, listOf(ConversationIdSample.AlphaAppFeedback))

        val undoOperationSlot = slot<UndoableOperation>()
        moveConversations(userId, conversationIds, destinationLabel)
        coVerify { registerUndoableOperation(capture(undoOperationSlot)) }
        coEvery { undoableOperationRepository.getLastOperation() } returns undoOperationSlot.captured

        // When
        undoLastOperation()

        // Then
        coVerify { moveConversations(userId, listOf(ConversationIdSample.Newsletter), LabelIdSample.Archive) }
        coVerify { moveConversations(userId, listOf(ConversationIdSample.AlphaAppFeedback), LabelIdSample.Inbox) }
    }

    @Test
    fun `when undo is called then move conversations is called with conversations grouped by label`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")
        val expectedConversations = listOf(ConversationSample.Newsletter, ConversationSample.AppointmentReminder)
        val conversationIds = expectedConversations.map { it.conversationId }
        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectRegisterUndoOperationSucceeds()
        // Mocking "do" (move) expected calls
        expectMoveSucceeds(destinationLabel, conversationIds, expectedConversations)
        expectObserveCachedConversationsSucceeds(conversationIds, expectedConversations)
        // Mocking "undo" (move back) expected calls
        expectMoveSucceeds(LabelIdSample.Archive, conversationIds)

        val undoOperationSlot = slot<UndoableOperation>()
        moveConversations(userId, conversationIds, destinationLabel)
        coVerify { registerUndoableOperation(capture(undoOperationSlot)) }
        coEvery { undoableOperationRepository.getLastOperation() } returns undoOperationSlot.captured

        // When
        undoLastOperation()

        // Then
        coVerify { moveConversations(userId, conversationIds, LabelIdSample.Archive) }
    }


    private fun expectObserveCachedConversationsSucceeds(
        conversationIds: List<ConversationId>,
        expectedConversations: List<Conversation>? = emptyList()
    ) {
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(expectedConversations!!)
    }

    private fun expectRegisterUndoOperationSucceeds() {
        coEvery { registerUndoableOperation(any<UndoableOperation.UndoMoveConversations>()) } just Runs
    }

    private fun expectMoveSucceeds(
        destinationLabel: LabelId,
        conversationIds: List<ConversationId>,
        expectedList: List<Conversation>? = emptyList()
    ) {
        val exclusiveList = exclusiveMailLabels.map { it.id.labelId }
        coEvery {
            conversationRepository.move(userId, conversationIds, exclusiveList, exclusiveList, destinationLabel)
        } returns expectedList!!.right()
    }

    private fun expectObserveExclusiveMailLabelSucceeds() {
        every { observeExclusiveMailLabels(userId) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }

    private fun expectObserveMailLabelsSucceeds() {
        every { observeMailLabels(userId, any()) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }

}
