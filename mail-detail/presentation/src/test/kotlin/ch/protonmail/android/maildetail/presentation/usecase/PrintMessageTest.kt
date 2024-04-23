package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import android.content.res.Resources
import android.print.PrintManager
import android.text.format.Formatter
import android.webkit.WebView
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PrintMessageTest {

    private val printManager = mockk<PrintManager>()
    private val resourcesMock = mockk<Resources> {
        every { getString(R.string.subject) } returns "Subject:"
        every { getString(R.string.from) } returns "From:"
        every { getString(R.string.to) } returns "To:"
        every { getString(R.string.cc) } returns "Cc:"
        every { getString(R.string.date) } returns "Date:"
        every { getQuantityString(R.plurals.attachment, any(), any()) } returns "4 Attachments"
    }
    private val context = mockk<Context> {
        every { getSystemService(Context.PRINT_SERVICE) } returns printManager
        every { resources } returns resourcesMock
    }

    private val printMessage = PrintMessage()

    @BeforeTest
    fun setUp() {
        mockkStatic(Formatter::class)
        every { Formatter.formatShortFileSize(any(), any()) } returns "12 MB"
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should load the message body together with additional information at the top in a web view`() {
        // Given
        val subject = "subject"
        val messageDetailHeaderUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        val messageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        mockkConstructor(WebView::class)
        every { anyConstructed<WebView>().webViewClient = any() } returns mockk()
        every { anyConstructed<WebView>().loadDataWithBaseURL(any(), any(), any(), any(), any()) } returns mockk()

        // When
        printMessage(
            context,
            subject,
            messageDetailHeaderUiModel,
            messageBodyUiModel,
            MessageBodyExpandCollapseMode.Collapsed
        ) { _, _ -> null }

        // Then
        val expectedData = """
            <html>
             <head></head>
             <body>
              <hr> Subject: subject 
              <br>
               From: Sender sender@pm.com 
              <br>
              To: Recipient1 recipient1@pm.com, Recipient2 recipient2@pm.com
              <br>
              Cc: Recipient3 recipient3@pm.com
              <br>
              Date: 08/11/2022, 17:16
              <br>
              4 Attachments (12 MB)
              <hr>This is a raw encrypted message body.
             </body>
            </html>
        """.trimIndent()
        verify {
            anyConstructed<WebView>().loadDataWithBaseURL(null, expectedData, "text/HTML", "UTF-8", null)
        }
    }
}
