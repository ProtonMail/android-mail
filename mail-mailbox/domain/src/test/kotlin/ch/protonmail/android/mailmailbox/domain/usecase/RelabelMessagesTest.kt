package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class RelabelMessagesTest {

    private val userId = UserIdSample.Primary

    private val messageRepository: MessageRepository = mockk()
    private val relabel = RelabelMessages(messageRepository)

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // Given
        val expectedMessageIds = listOf(MessageIdSample.Invoice)
        val error = DataError.Local.NoDataCached.left()
        coEvery {
            messageRepository.relabel(
                userId = userId,
                messageIds = expectedMessageIds,
                labelsToBeRemoved = listOf(LabelId("labelId")),
                labelsToBeAdded = listOf(LabelId("labelId2"))
            )
        } returns error

        // When
        val result = relabel(
            userId = UserIdSample.Primary,
            messageIds = expectedMessageIds,
            currentSelections = LabelSelectionList(
                selectedLabels = listOf(LabelId("labelId")),
                partiallySelectionLabels = emptyList()
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = listOf(LabelId("labelId2")),
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        assertEquals(error, result)
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is changed to remove`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.HtmlInvoice)
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val oldPartialSelectedLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"), LabelId("5"))
        val addedLabels = listOf(LabelId("4"))
        coEvery {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns mockk()

        // When
        relabel(
            UserIdSample.Primary,
            messageIds,
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = oldPartialSelectedLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        coVerify {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is changed to add`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.HtmlInvoice)
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val oldPartialSelectedLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"), LabelId("5"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"), LabelId("5"))
        coEvery {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns mockk()

        // When
        relabel(
            UserIdSample.Primary,
            messageIds,
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = oldPartialSelectedLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        coVerify {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case passing correct add and remove label lists when partial selection is unchanged`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.HtmlInvoice)
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val partialSelectionLabels = listOf(LabelId("5"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"))
        coEvery {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns mockk()

        // When
        relabel(
            UserIdSample.Primary,
            messageIds,
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = partialSelectionLabels
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = partialSelectionLabels
            )
        )

        // Then
        coVerify {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }

    @Test
    fun `use case only forwards affected labels to repository`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.HtmlInvoice)
        val oldLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("3"))
        val newLabelIds = listOf(LabelId("1"), LabelId("2"), LabelId("4"))
        val removedLabels = listOf(LabelId("3"))
        val addedLabels = listOf(LabelId("4"))
        coEvery {
            messageRepository.relabel(
                userId = userId,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        } returns mockk()

        // When
        relabel(
            userId,
            messageIds,
            currentSelections = LabelSelectionList(
                selectedLabels = oldLabelIds,
                partiallySelectionLabels = emptyList()
            ),
            updatedSelections = LabelSelectionList(
                selectedLabels = newLabelIds,
                partiallySelectionLabels = emptyList()
            )
        )

        // Then
        coVerify {
            messageRepository.relabel(
                userId = UserIdSample.Primary,
                messageIds = messageIds,
                labelsToBeRemoved = removedLabels,
                labelsToBeAdded = addedLabels
            )
        }
    }
}
