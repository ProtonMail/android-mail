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

package ch.protonmail.android.maillabel.presentation.mapper

import android.content.Context
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

internal class MailLabelTextMapperTest {

    private val context = mockk<Context>()
    private val mapper = MailLabelTextMapper(context)

    @Test
    fun `should return the string as is when mapping a raw value`() {
        // Given
        val expectedString = "string"
        val mailLabelText = MailLabelText(expectedString)

        // When
        val actual = mapper.mapToString(mailLabelText)

        // Then
        assertEquals(expectedString, actual)
    }

    @Test
    fun `should return the resolved string when mapping a stringRes based value`() {
        // Given
        val expectedString = "string"
        val stringRes = 123_456_789
        val mailLabelText = MailLabelText(stringRes)

        every { context.getString(stringRes) } returns expectedString

        // When
        val actual = mapper.mapToString(mailLabelText)

        // Then
        assertEquals(expectedString, actual)
        verify(exactly = 1) { context.getString(stringRes) }
    }
}
