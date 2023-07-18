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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoesMessageBodyHaveEmbeddedImagesTest {

    private val doesMessageBodyHaveEmbeddedImages = DoesMessageBodyHaveEmbeddedImages()

    @Test
    fun `should return true when message body has an embedded image attachment`() {
        // Given
        val messageBody = DecryptedMessageBodyTestData.messageBodyWithEmbeddedImage

        // When
        val actual = doesMessageBodyHaveEmbeddedImages(messageBody)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when message body has an embedded octet stream attachment`() {
        // Given
        val messageBody = DecryptedMessageBodyTestData.messageBodyWithEmbeddedOctetStream

        // When
        val actual = doesMessageBodyHaveEmbeddedImages(messageBody)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when message body has an invalid embedded attachment`() {
        // Given
        val messageBody = DecryptedMessageBodyTestData.messageBodyWithInvalidEmbeddedAttachment

        // When
        val actual = doesMessageBodyHaveEmbeddedImages(messageBody)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when message body has only a regular attachment`() {
        // Given
        val messageBody = DecryptedMessageBodyTestData.messageBodyWithAttachment

        // When
        val actual = doesMessageBodyHaveEmbeddedImages(messageBody)

        // Then
        assertFalse(actual)
    }
}
