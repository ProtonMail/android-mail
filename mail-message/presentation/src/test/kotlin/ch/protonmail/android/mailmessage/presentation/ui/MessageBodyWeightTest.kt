/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.presentation.ui

import android.net.Uri
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageBodyWeightTest {

    @Test
    fun `given a body spilled to a file, then it is classified huge`() {
        // Given
        val content = MessageBodyContent.File(mockk<Uri>())

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 0)

        // Then
        assertEquals(MessageBodyWeight.Huge, weight)
    }

    @Test
    fun `given a body that renders very tall, then it is classified huge`() {
        // Given
        val content = MessageBodyContent.Text("<p>a long thread</p>")

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 20_000)

        // Then
        assertEquals(MessageBodyWeight.Huge, weight)
    }

    @Test
    fun `given a long but not extreme body, then it is classified heavy not huge`() {
        // Given a body that renders long (12-15k px) but below the huge threshold
        val content = MessageBodyContent.Text("<p>a long message</p>")

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 13_000)

        // Then
        assertEquals(MessageBodyWeight.Heavy, weight)
    }

    @Test
    fun `given a body that renders past one screen, then it is classified medium`() {
        // Given
        val content = MessageBodyContent.Text("<p>a message</p>")

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 4_000)

        // Then
        assertEquals(MessageBodyWeight.Medium, weight)
    }

    @Test
    fun `given a body that renders short, then it is classified light`() {
        // Given
        val content = MessageBodyContent.Text("<p>hi there</p>")

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 300)

        // Then
        assertEquals(MessageBodyWeight.Light, weight)
    }

    @Test
    fun `given a body not yet measured, then it is classified light`() {
        // Given a just-expanded body whose rendered height is not known yet
        val content = MessageBodyContent.Text("<p>hi there</p>")

        // When
        val weight = MessageBodyWeight.of(content, heightPx = 0)

        // Then
        assertEquals(MessageBodyWeight.Light, weight)
    }

    @Test
    fun `given a short body full of images, then it is not penalised for the images`() {
        // Given the browser caps a decoded image to its displayed size, so image count no longer drives cost
        val content = MessageBodyContent.Text((1..8).joinToString("") { """<img src="proton-https://x/$it">""" })

        // When it renders short
        val weight = MessageBodyWeight.of(content, heightPx = 300)

        // Then
        assertEquals(MessageBodyWeight.Light, weight)
    }
}
