/*
 * Copyright (c) 2025 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintConfiguration
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessage
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessageDocumentBuilder
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintWebViewHandler
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PrintMessageTest {

    private val bodyBuilder = mockk<PrintMessageDocumentBuilder>()
    private val webViewHandler = mockk<PrintWebViewHandler>()
    private lateinit var printMessage: PrintMessage

    @BeforeTest
    fun setup() {
        printMessage = PrintMessage(bodyBuilder, webViewHandler)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should trigger print webView`() = runTest {
        // Given
        val subject = "subject"
        val context = mockk<Context>()
        val messageHeader = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        val messageBody = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        val loadEmbeddedImage = { messageId: MessageId, string: String -> null }
        val printConfiguration = PrintConfiguration()
        val expectedBody = "body"
        val expectedConfig = PrintWebViewHandler.PrintWebViewConfig(
            context = context,
            htmlContent = expectedBody,
            subject = subject,
            resourceConfig = PrintWebViewHandler.ResourceLoadingConfig(
                messageId = messageBody.messageId,
                loadEmbeddedImage = loadEmbeddedImage,
                showRemoteContent = printConfiguration.showRemoteContent,
                showEmbeddedImages = printConfiguration.showEmbeddedImages
            )
        )
        coEvery { bodyBuilder.buildDocument(context, subject, messageHeader, messageBody) } returns expectedBody
        every { webViewHandler.createPrintWebView(expectedConfig) } returns mockk()

        // When
        printMessage.invoke(context, subject, messageHeader, messageBody, loadEmbeddedImage, printConfiguration)

        // Then
        verify(exactly = 1) { webViewHandler.createPrintWebView(expectedConfig) }
        confirmVerified(webViewHandler)
    }
}
