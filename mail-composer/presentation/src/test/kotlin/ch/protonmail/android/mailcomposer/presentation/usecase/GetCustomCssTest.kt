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

package ch.protonmail.android.mailcomposer.presentation.usecase

import android.content.Context
import ch.protonmail.android.mailcomposer.presentation.R
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.AfterTest

internal class GetCustomCssTest {

    private val mockContext = mockk<Context>()

    private val getCustomCss = GetCustomCss(mockContext)

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `loads the css reset raw resource`() = runTest {
        // Given
        every {
            mockContext.resources.openRawResource(R.raw.css_reset_with_custom_props)
        } returns "".byteInputStream()

        // When
        getCustomCss()

        // Then
        verify(exactly = 1) { mockContext.resources.openRawResource(R.raw.css_reset_with_custom_props) }
        confirmVerified(mockContext.resources)
    }
}
