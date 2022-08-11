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

package ch.protonmail.android.mailsettings.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SaveAlternativeRoutingSettingTest {

    private val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true)

    private val alternativeRoutingRepository: AlternativeRoutingRepository = mockk()

    private val saveAlternativeRoutingSetting = SaveAlternativeRoutingSetting(alternativeRoutingRepository)

    @Test
    fun `should return success when preference is saved successfully`() = runTest {
        // Given
        val expectedResult = Unit.right()
        coEvery { alternativeRoutingRepository.save(alternativeRoutingPreference) } returns expectedResult

        // When
        val result = saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled)

        // Then
        coVerify { alternativeRoutingRepository.save(alternativeRoutingPreference) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should return failure when preference is not saved successfully`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        coEvery { alternativeRoutingRepository.save(alternativeRoutingPreference) } returns expectedResult

        // When
        val result = saveAlternativeRoutingSetting(alternativeRoutingPreference.isEnabled)

        // Then
        assertEquals(expectedResult, result)
    }
}
