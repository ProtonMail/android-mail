package ch.protonmail.android.mailcommon.data.repository

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoableOperationInMemoryRepositoryTest {

    private val undoableOperationRepo = UndoableOperationInMemoryRepository()

    @Test
    fun `stores and returns the last given operation in memory`() = runTest {
        // Given
        val lambda = {
            println("logic to undo the operation")
            Unit.right()
        }
        val expected = UndoableOperation.UndoMoveMessages(lambda)
        undoableOperationRepo.storeOperation(expected)

        // When
        val actual = undoableOperationRepo.getLastOperation()

        // Then
        assertEquals(expected, actual)
    }

}
