package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import android.net.Uri
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import ch.protonmail.android.mailcomposer.domain.usecase.AddInlineAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.AddStandardAttachment
import ch.protonmail.android.mailcomposer.presentation.usecase.AddAttachmentTypeTest.ExpectedAttachmentOperation.AddInlineAttachmentOp
import ch.protonmail.android.mailcomposer.presentation.usecase.AddAttachmentTypeTest.ExpectedAttachmentOperation.AddStandardAttachmentOp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AddAttachmentTypeTest(
    @Suppress("unused") private val testName: String,
    val mimeType: String?,
    val expected: ExpectedAttachmentOperation
) {

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
    fun test() = runTest {
        // Given
        val fileUri = mockk<Uri>()
        every { context.contentResolver.getType(fileUri) } returns mimeType
        when (expected) {
            AddInlineAttachmentOp -> coEvery { addInlineAttachment(fileUri) } returns "".right()
            AddStandardAttachmentOp -> coEvery { addStandardAttachment(fileUri) } returns Unit.right()
        }

        // When
        addAttachment(fileUri = fileUri, draftMimeType = DraftMimeType.Html)

        // Then
        when (expected) {
            AddInlineAttachmentOp -> coVerify { addInlineAttachment(fileUri) }
            AddStandardAttachmentOp -> coVerify { addStandardAttachment(fileUri) }
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "Adds inline attachment when mime is image/jpg",
                "image/jpg",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds inline attachment when mime is image/webp",
                "image/webp",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds inline attachment when mime is image/jpeg",
                "image/jpeg",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds inline attachment when mime is image/gif",
                "image/gif",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds inline attachment when mime is image/apng",
                "image/apng",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds inline attachment when mime is image/png",
                "image/png",
                AddInlineAttachmentOp
            ),
            arrayOf(
                "Adds standard attachment when mime is null",
                null,
                AddStandardAttachmentOp
            ),
            arrayOf(
                "Adds standard attachment when mime is anything else",
                "application/pdf",
                AddStandardAttachmentOp
            )
        )
    }

    sealed interface ExpectedAttachmentOperation {
        data object AddStandardAttachmentOp : ExpectedAttachmentOperation
        data object AddInlineAttachmentOp : ExpectedAttachmentOperation
    }

}
