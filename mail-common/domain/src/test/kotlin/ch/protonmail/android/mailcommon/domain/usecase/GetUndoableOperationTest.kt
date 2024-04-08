package ch.protonmail.android.mailcommon.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoableOperationRepository
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetUndoableOperationTest {

    private val undoableOperationRepository = mockk<UndoableOperationRepository>()

    private val getUndoableOperation = GetUndoableOperation(undoableOperationRepository)

    @Test
    fun `get last undoable operation from the repository`() = runTest {
        // Given
        val expected = UndoableOperation.MoveMessages(mapOf(Pair("msgId", LabelIdSample.Inbox)), LabelIdSample.Archive)
        coEvery { undoableOperationRepository.getLastOperation() } returns expected

        // When
        val actual = getUndoableOperation()

        // Then
        assertEquals(expected, actual)
    }
}
