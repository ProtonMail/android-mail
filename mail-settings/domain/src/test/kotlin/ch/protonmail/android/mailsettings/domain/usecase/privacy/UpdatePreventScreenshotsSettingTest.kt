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
import ch.protonmail.android.mailsettings.domain.repository.PreventScreenshotsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class UpdatePreventScreenshotsSettingTest {

    private val preventScreenshotsRepository = mockk<PreventScreenshotsRepository>()
    private val updatePreventScreenshotsSetting = UpdatePreventScreenshotsSetting(preventScreenshotsRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when the repository cannot update with a new value, an error is returned`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        coEvery { preventScreenshotsRepository.update(any()) } returns PreferencesError.left()

        // When
        val result = updatePreventScreenshotsSetting(newValue = true)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { preventScreenshotsRepository.update(true) }
        confirmVerified(preventScreenshotsRepository)
    }

    @Test
    fun `when the repository updates the new value for the setting, success is returned`() = runTest {
        // Given
        val expectedResult = Unit.right()
        coEvery { preventScreenshotsRepository.update(any()) } returns Unit.right()

        // When
        val result = updatePreventScreenshotsSetting(newValue = true)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { preventScreenshotsRepository.update(true) }
        confirmVerified(preventScreenshotsRepository)
    }
}
