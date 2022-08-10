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

package ch.protonmail.android.mailsettings.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingLocalDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternativeRoutingRepositoryImplTest {

    private val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true).right()
    private val alternativeRoutingPreferenceFlow = flowOf(alternativeRoutingPreference)

    private val alternativeRoutingLocalDataSource: AlternativeRoutingLocalDataSource = mockk {
        every { observe() } returns alternativeRoutingPreferenceFlow
    }

    private val alternativeRoutingRepository = AlternativeRoutingRepositoryImpl(alternativeRoutingLocalDataSource)

    @Test
    fun `returns value from the local data source`() = runTest {
        // When
        alternativeRoutingRepository.observe().test {
            // Then
            assertEquals(AlternativeRoutingPreference(isEnabled = true).right(), awaitItem())
            awaitComplete()
        }
    }
}
