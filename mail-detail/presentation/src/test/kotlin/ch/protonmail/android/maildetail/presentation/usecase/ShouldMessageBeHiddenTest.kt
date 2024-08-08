package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Archive
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Trash
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldMessageBeHiddenTest {

    private val shouldMessageBeHidden = ShouldMessageBeHidden()

    @Test
    fun `should return true when a trashed message is opened from a non-trash location and the filter is ON`() {
        // When
        val actual = shouldMessageBeHidden(
            Archive.labelId, listOf(Trash.labelId), shouldHideMessagesBasedOnTrashFilter = true
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when a trashed message is opened from a non-trash location and the filter is OFF`() {
        // When
        val actual = shouldMessageBeHidden(
            Archive.labelId, listOf(Trash.labelId), shouldHideMessagesBasedOnTrashFilter = false
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true when a non-trashed message is opened from the trash location and the filter in ON`() {
        // When
        val actual = shouldMessageBeHidden(
            Trash.labelId, listOf(Archive.labelId), shouldHideMessagesBasedOnTrashFilter = true
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when a non-trashed message is opened from the trash location and the filter in OFF`() {
        // When
        val actual = shouldMessageBeHidden(
            Trash.labelId, listOf(Archive.labelId), shouldHideMessagesBasedOnTrashFilter = false
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when a non-trashed message is opened from a non-trash location and the filter is ON`() {
        // When
        val actual = shouldMessageBeHidden(
            Archive.labelId, listOf(Archive.labelId), shouldHideMessagesBasedOnTrashFilter = true
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when a non-trashed message is opened from a non-trash location and the filter is OFF`() {
        // When
        val actual = shouldMessageBeHidden(
            Archive.labelId, listOf(Archive.labelId), shouldHideMessagesBasedOnTrashFilter = false
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when a trashed message is opened from the trash location and the filter is ON`() {
        // When
        val actual = shouldMessageBeHidden(
            Trash.labelId, listOf(Trash.labelId), shouldHideMessagesBasedOnTrashFilter = true
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when a trashed message is opened from the trash location and the filter is OFF`() {
        // When
        val actual = shouldMessageBeHidden(
            Trash.labelId, listOf(Trash.labelId), shouldHideMessagesBasedOnTrashFilter = false
        )

        // Then
        assertFalse(actual)
    }
}
