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

package ch.protonmail.android.mailcommon

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class AppInBackgroundStateTest {

    @Test
    fun `should emit nothing when first instantiated`() = runTest {
        // Given
        val appInBackgroundState = AppInBackgroundState()

        // When + Then
        appInBackgroundState.observe().test {
            expectNoEvents()
        }
    }

    @Test
    fun `should emit the correct value when set`() = runTest {
        // Given
        val appInBackgroundState = AppInBackgroundState()
        val expectedResult = false

        // When
        appInBackgroundState.setAppInBackground(isAppInBackground = expectedResult)

        // Then
        appInBackgroundState.observe().test {
            assertEquals(expectedResult, awaitItem())
        }
    }

    @Test
    fun `should default to true when fetching the background value directly when first instantiated`() {
        // Given
        val appInBackgroundState = AppInBackgroundState()
        val expectedResult = true

        // When
        val actual = appInBackgroundState.isAppInBackground()

        // Then
        assertEquals(expectedResult, actual)
    }
}
