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

import java.io.ByteArrayInputStream
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessageDocumentBuilder
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessageHeaderBuilder
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PrintMessageBodyBuilderTest {

    private val headerBuilder = mockk<PrintMessageHeaderBuilder>()
    private val context = mockk<Context>()
    private lateinit var builder: PrintMessageDocumentBuilder

    @BeforeTest
    fun setup() {
        builder = PrintMessageDocumentBuilder(headerBuilder, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should build the document by concatenating header and body`() = runTest {
        // Given
        val header = "Header text\n"
        val subject = "subject"
        val headerUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        val bodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        val expectedBody = (bodyUiModel.messageBody as MessageBodyContent.Text).value
        every { headerBuilder.buildHeader(subject, headerUiModel, bodyUiModel.attachments) } returns header

        // When
        val body = builder.buildDocument(context, subject, headerUiModel, bodyUiModel)

        // Then
        assertEquals(header + expectedBody, body)
    }

    @Test
    fun `should build the document by reading a file-backed body`() = runTest {
        // Given
        val header = "Header text\n"
        val subject = "subject"
        val fileBody = "<html>Large cached body</html>"
        val contentUri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val headerUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        val bodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel.copy(
            messageBody = MessageBodyContent.File(contentUri)
        )
        every { headerBuilder.buildHeader(subject, headerUiModel, bodyUiModel.attachments) } returns header
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(contentUri) } returns ByteArrayInputStream(
            fileBody.toByteArray()
        )

        // When
        val body = builder.buildDocument(context, subject, headerUiModel, bodyUiModel)

        // Then
        assertEquals(header + fileBody, body)
    }

    @Test
    fun `should build the document with empty body when the file stream cannot be opened`() = runTest {
        // Given
        val header = "Header text\n"
        val subject = "subject"
        val contentUri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>()
        val headerUiModel = MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        val bodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel.copy(
            messageBody = MessageBodyContent.File(contentUri)
        )
        every { headerBuilder.buildHeader(subject, headerUiModel, bodyUiModel.attachments) } returns header
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(contentUri) } returns null

        // When
        val body = builder.buildDocument(context, subject, headerUiModel, bodyUiModel)

        // Then
        assertEquals(header, body)
    }
}
