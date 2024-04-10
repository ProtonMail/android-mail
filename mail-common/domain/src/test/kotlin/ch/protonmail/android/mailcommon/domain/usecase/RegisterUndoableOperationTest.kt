package ch.protonmail.android.mailcommon.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoableOperationRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RegisterUndoableOperationTest {

    private val undoableOperationRepository = mockk<UndoableOperationRepository>()

    private val registerUndoableOperation = RegisterUndoableOperation(undoableOperationRepository)

    @Test
    fun `registers undoable operation with the repository`() = runTest {
        // Given
        val lambda = {
            println("logic to undo the operation")
            Unit.right()
        }
        val operation = UndoableOperation.UndoMoveMessages(lambda)
        coEvery { undoableOperationRepository.storeOperation(operation) } just Runs

        // When
        registerUndoableOperation(operation)

        // Then
        coVerify { undoableOperationRepository.storeOperation(operation) }
    }
}
