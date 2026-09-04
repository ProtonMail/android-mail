package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import ch.protonmail.android.mailcomposer.domain.usecase.AddInlineAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.AddStandardAttachment
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AddAttachmentTest {

    private val context = mockk<Context>()
    private val addStandardAttachment = mockk<AddStandardAttachment>()
    private val addInlineAttachment = mockk<AddInlineAttachment>()
    private val pastedImageRepository = mockk<PastedImageRepository>()

    private val addAttachment = AddAttachment(
        context,
        addStandardAttachment,
        addInlineAttachment,
        pastedImageRepository
    )

    @Test
    fun `returns rust defined content id when successfully adding inline attachment`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expectedCid = "rust-defined-cid"
        val expected = AddAttachment.AddAttachmentResult.InlineAttachmentAdded(expectedCid)
        coEvery { context.contentResolver.getType(fileUri) } returns "image/jpeg"
        coEvery { addInlineAttachment(fileUri) } returns expectedCid.right()

        // When
        val actual = addAttachment(fileUri, DraftMimeType.Html)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns rust standard attachment added result when successfully adding attachment`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expected = AddAttachment.AddAttachmentResult.StandardAttachmentAdded
        coEvery { context.contentResolver.getType(fileUri) } returns "application/pdf"
        coEvery { addStandardAttachment(fileUri) } returns Unit.right()

        // When
        val actual = addAttachment(fileUri, DraftMimeType.Html)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns error when adding attachment fails`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expected = AddAttachmentError.AttachmentTooLarge
        coEvery { context.contentResolver.getType(fileUri) } returns "application/pdf"
        coEvery { addStandardAttachment(fileUri) } returns expected.left()

        // When
        val actual = addAttachment(fileUri, DraftMimeType.Html)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns error when adding inline attachment fails`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expected = AddAttachmentError.TooManyAttachments
        coEvery { context.contentResolver.getType(fileUri) } returns "image/png"
        coEvery { addInlineAttachment(fileUri) } returns expected.left()

        // When
        val actual = addAttachment(fileUri, DraftMimeType.Html)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `adds any attachments as standard when force standard disposition called`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expected = AddAttachment.AddAttachmentResult.StandardAttachmentAdded
        coEvery { context.contentResolver.getType(fileUri) } returns "image/jpeg"
        coEvery { addStandardAttachment(fileUri) } returns Unit.right()

        // When
        val actual = addAttachment.forcingStandardDisposition(fileUri)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `adds any attachments as standard when draft mime type is plain text`() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        val expected = AddAttachment.AddAttachmentResult.StandardAttachmentAdded
        coEvery { context.contentResolver.getType(fileUri) } returns "image/jpeg"
        coEvery { addStandardAttachment(fileUri) } returns Unit.right()

        // When
        val actual = addAttachment.invoke(fileUri, DraftMimeType.PlainText)

        // Then
        assertEquals(expected.right(), actual)
    }


}
