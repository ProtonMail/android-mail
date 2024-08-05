package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Trash
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldMessageBeHiddenTest {

    private val shouldMessageBeHidden = ShouldMessageBeHidden()

    @Test
    fun `should return true when a trashed message is opened from a non-trash location`() {
        // When
        val actual = shouldMessageBeHidden(Archive.labelId, listOf(Trash.labelId))

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when a non-trashed message is opened from the trash location`() {
        // When
        val actual = shouldMessageBeHidden(Trash.labelId, listOf(Archive.labelId))

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when a non-trashed message is opened from a non-trash location`() {
        // When
        val actual = shouldMessageBeHidden(Archive.labelId, listOf(Archive.labelId))

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when a trashed message is opened from the trash location`() {
        // When
        val actual = shouldMessageBeHidden(Trash.labelId, listOf(Trash.labelId))

        // Then
        assertFalse(actual)
    }
}
