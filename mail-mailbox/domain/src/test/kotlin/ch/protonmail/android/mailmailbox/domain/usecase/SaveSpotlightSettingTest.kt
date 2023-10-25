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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailmailbox.domain.model.SpotlightPreference
import ch.protonmail.android.mailmailbox.domain.repository.SpotlightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SaveSpotlightSettingTest {

    private val spotlightPreference = SpotlightPreference(display = true)

    private val spotlightRepository: SpotlightRepository = mockk()

    private val saveSpotlight = SaveSpotlight(spotlightRepository)

    @Test
    fun `should return success when preference is saved successfully`() = runTest {
        // Given
        val expectedResult = Unit.right()
        coEvery { spotlightRepository.save(spotlightPreference) } returns expectedResult

        // When
        val result = saveSpotlight(spotlightPreference.display)

        // Then
        coVerify { spotlightRepository.save(spotlightPreference) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should return failure when preference is not saved successfully`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        coEvery { spotlightRepository.save(spotlightPreference) } returns expectedResult

        // When
        val result = saveSpotlight(spotlightPreference.display)

        // Then
        assertEquals(expectedResult, result)
    }
}
