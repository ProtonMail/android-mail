/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.composer.data.usecase

import java.io.File
import java.io.StringWriter
import ch.protonmail.android.composer.data.sample.SendMessageSample
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import com.github.mangstadt.vinnie.io.FoldedLineWriter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.internal.util.io.IOUtil.writeText
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("MaxLineLength")
@OptIn(ExperimentalEncodingApi::class)
class GenerateMimeBodyTest {

    private val sut = GenerateMimeBody()

    @Test
    fun `generates MIME attachment part with Base64 encoded file name`() = runTest {

        // Given
        val expectedAttachment = MessageAttachmentSample.document
        val expectedFileText = "expected content of a text file"
        val expectedAttachmentFile = expectFileWithText(expectedFileText)

        val expectedBase64FileName = Base64.encode(expectedAttachment.name.toByteArray())

        val expectedFileNameTemplate = "=?UTF-8?B?$expectedBase64FileName?="

        val stringWriter = StringWriter()
        FoldedLineWriter(stringWriter).use {
            it.write("Content-Transfer-Encoding: base64")
            it.writeln()
            it.write("Content-Type: ${expectedAttachment.mimeType}; filename=\"$expectedFileNameTemplate\"; name=\"$expectedFileNameTemplate\"")
            it.writeln()
            it.write("Content-Disposition: attachment; filename=\"$expectedFileNameTemplate\"; name=\"$expectedFileNameTemplate\"")
            it.writeln()
            it.writeln()
            it.write(Base64.encode(expectedAttachmentFile.readBytes()))
        }
        // FoldedLineWriter uses CRLF
        val expectedQuotedPrintableAttachmentPart = stringWriter.toString().replace("\r\n", "\n")

        // When
        val actual = sut(
            SendMessageSample.CleartextBody,
            MimeType.PlainText,
            listOf(expectedAttachment),
            mapOf(expectedAttachment.attachmentId to expectedAttachmentFile)
        )

        // Then
        assert(actual.contains(expectedQuotedPrintableAttachmentPart))
    }

    private fun expectFileWithText(text: String): File = File.createTempFile("file", "txt").also {
        writeText(text, it)
    }
}
