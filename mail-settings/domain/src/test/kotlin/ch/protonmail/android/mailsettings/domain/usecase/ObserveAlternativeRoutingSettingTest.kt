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

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveAlternativeRoutingSettingTest {

    private val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true).right()

    private val alternativeRoutingRepository: AlternativeRoutingRepository = mockk {
        every { observe() } returns flowOf(alternativeRoutingPreference)
    }

    private val observeAlternativeRoutingSetting = ObserveAlternativeRoutingSetting(alternativeRoutingRepository)

    @Test
    fun `should call observe method from repository when use case is invoked`() = runTest {
        // When
        observeAlternativeRoutingSetting().test {
            val actual = awaitItem()
            awaitComplete()

            // Then
            verify { alternativeRoutingRepository.observe() }
            assertEquals(alternativeRoutingPreference, actual)
        }
    }
}
