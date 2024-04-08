package ch.protonmail.android.mailcommon.data.repository

import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoableOperationInMemoryRepositoryTest {

    private val undoableOperationRepo = UndoableOperationInMemoryRepository()

    @Test
    fun `stores and returns the last given operation in memory`() = runTest {
        // Given
        val expected = UndoableOperation.MoveMessages(mapOf(Pair("msgId", LabelIdSample.Inbox)), LabelIdSample.Archive)
        undoableOperationRepo.storeOperation(expected)

        // When
        val actual = undoableOperationRepo.getLastOperation()

        // Then
        assertEquals(expected, actual)
    }

}
