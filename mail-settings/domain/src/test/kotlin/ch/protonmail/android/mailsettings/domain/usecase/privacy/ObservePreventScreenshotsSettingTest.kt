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

package ch.protonmail.android.mailsettings.domain.usecase.privacy

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.PreventScreenshotsPreference
import ch.protonmail.android.mailsettings.domain.repository.PreventScreenshotsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ObservePreventScreenshotsSettingTest {

    private val preventScreenshotsRepository = mockk<PreventScreenshotsRepository>()
    private val observePreventScreenshotsSetting = ObservePreventScreenshotsSetting(preventScreenshotsRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when current value cannot be read from the repository, an error is returned`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        every { observePreventScreenshotsSetting() } returns flowOf(PreferencesError.left())

        // When
        val result = observePreventScreenshotsSetting().first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when current value is read correctly from the repository, it is returned as a preference value`() = runTest {
        // Given
        val expectedResult = PreventScreenshotsPreference(isEnabled = false).right()
        every {
            observePreventScreenshotsSetting()
        } returns flowOf(PreventScreenshotsPreference(isEnabled = false).right())

        // When
        val result = observePreventScreenshotsSetting().first()

        // Then
        assertEquals(expectedResult, result)
    }
}
