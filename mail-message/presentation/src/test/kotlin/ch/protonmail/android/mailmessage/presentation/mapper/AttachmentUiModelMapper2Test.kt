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

package ch.protonmail.android.mailmessage.presentation.mapper

import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import org.junit.Test
import kotlin.test.assertEquals

internal class AttachmentUiModelMapper2Test {

    private val attachmentUiModelMapper = AttachmentUiModelMapper2

    @Test
    fun `should map pdf message attachment with application pdf mime type to a ui model`() {
        // When
        val actual = attachmentUiModelMapper.toUiModel(MessageAttachmentSample.invoice)

        // Then
        val expected = AttachmentUiModelSample.invoice
        assertEquals(expected, actual)
    }

    @Test
    fun `should map message attachment with application doc mime type to a ui model`() {
        // When
        val actual = attachmentUiModelMapper.toUiModel(MessageAttachmentSample.document)

        // Then
        val expected = AttachmentUiModelSample.document
        assertEquals(expected, actual)
    }

    @Test
    fun `should map message attachment with multiple dots in the name to a ui model`() {
        // When
        val actual = attachmentUiModelMapper.toUiModel(MessageAttachmentSample.documentWithMultipleDots)

        // Then
        val expected = AttachmentUiModelSample.documentWithMultipleDots
        assertEquals(expected, actual)
    }
}
