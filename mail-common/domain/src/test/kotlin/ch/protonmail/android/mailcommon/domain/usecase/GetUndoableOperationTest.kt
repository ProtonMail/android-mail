package ch.protonmail.android.mailcommon.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoableOperationRepository
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
        val lambda = {
            println("logic to undo the operation")
            Unit.right()
        }
        val expected = UndoableOperation.UndoMoveMessages(lambda)
        coEvery { undoableOperationRepository.getLastOperation() } returns expected

        // When
        val actual = getUndoableOperation()

        // Then
        assertEquals(expected, actual)
    }
}
