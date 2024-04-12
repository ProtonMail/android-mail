package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import ch.protonmail.android.maildetail.presentation.usecase.PrintMessage.Companion.JobName
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlin.test.Test

class PrintMessageTest {

    private val printManager = mockk<PrintManager>()
    private val context = mockk<Context> {
        every { getSystemService(Context.PRINT_SERVICE) } returns printManager
    }
    private val printAdapter = mockk<PrintDocumentAdapter>()

    private val printMessage = PrintMessage()

    @Test
    fun `should call print method from the print manager`() {
        // Given
        mockkConstructor(PrintAttributes.Builder::class)
        val printAttributes = mockk<PrintAttributes>()
        every { anyConstructed<PrintAttributes.Builder>().build() } returns printAttributes
        every { printManager.print(JobName, printAdapter, printAttributes) } returns mockk()

        // When
        printMessage(context, printAdapter)

        // Then
        verify { printManager.print(JobName, printAdapter, printAttributes) }
    }
}
