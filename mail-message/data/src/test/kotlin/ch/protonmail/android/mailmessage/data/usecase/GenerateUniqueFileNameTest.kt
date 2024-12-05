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

package ch.protonmail.android.mailmessage.data.usecase

import java.time.Instant
import ch.protonmail.android.mailmessage.data.local.usecase.GenerateUniqueFileName
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GenerateUniqueFileNameTest(
    private val input: String,
    private val expected: String
) {

    @Before
    fun setup() {
        mockkStatic(Instant::class)
        every { Instant.now().toEpochMilli() } returns MockTimestamp
    }

    @Test
    fun `test filename generation`() {
        val generateUniqueFileName = GenerateUniqueFileName()
        val result = generateUniqueFileName(input)
        assertEquals(expected, result)
    }

    companion object {

        private const val MockTimestamp = 1_701_778_710_000

        @JvmStatic
        @Parameterized.Parameters(name = "Test {index}: input={0}, expected={1}")
        fun data() = listOf(
            // Basic cases
            arrayOf("photo.jpg", "photo_1701778710000.jpg"),
            arrayOf("document.pdf", "document_1701778710000.pdf"),
            arrayOf("invite.ics", "invite_1701778710000.ics"),
            arrayOf("  invite.ics", "  invite_1701778710000.ics"),

            // No extension
            arrayOf("readme", "readme_1701778710000"),

            // Multiple dots
            arrayOf("my.favourite.photo.jpg", "my.favourite.photo_1701778710000.jpg"),

            // Long filenames
            arrayOf(
                "${"very_long_filename".repeat(20)}.txt",
                "${"very_long_filename".repeat(14).take(237)}_1701778710000.txt"
            ),

            // Special characters
            arrayOf("my photo (1).jpg", "my photo (1)_1701778710000.jpg"),

            // Empty string
            arrayOf("", "_1701778710000"),

            // Only extension
            arrayOf(".txt", "_1701778710000.txt"),

            // Unicode characters
            arrayOf("фото.jpg", "фото_1701778710000.jpg")
        )
    }
}
