package ch.protonmail.android.mailcommon.presentation.viewmodel

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.UndoLastOperation
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UndoOperationViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val undoLastOperation = mockk<UndoLastOperation>()

    private val viewModel = UndoOperationViewModel(undoLastOperation)

    @Test
    fun `emits success when undo operation succeeds`() = runTest {
        // Given
        coEvery { undoLastOperation() } returns Unit.right()

        // When
        viewModel.submitUndo()

        // Then
        assertEquals(Effect.of(Unit), viewModel.state.value.undoSucceeded)
        assertEquals(Effect.empty<Unit>(), viewModel.state.value.undoFailed)
    }

    @Test
    fun `emits failure when undo operation fails`() = runTest {
        // Given
        coEvery { undoLastOperation() } returns UndoLastOperation.Error.UndoFailed.left()

        // When
        viewModel.submitUndo()

        // Then
        assertEquals(Effect.of(Unit), viewModel.state.value.undoFailed)
        assertEquals(Effect.empty<Unit>(), viewModel.state.value.undoSucceeded)
    }
}
