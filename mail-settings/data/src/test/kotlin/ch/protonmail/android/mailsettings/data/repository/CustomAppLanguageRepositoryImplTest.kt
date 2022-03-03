/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.model.CustomAppLanguagePreference
import ch.protonmail.android.mailsettings.domain.repository.CustomAppLanguageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CustomAppLanguageRepositoryImplTest {

    private lateinit var customLanguageRepository: CustomAppLanguageRepository

    @Before
    fun setUp() {
        customLanguageRepository = CustomAppLanguageRepositoryImpl()
    }

    @Test
    fun `returns auto detect when no preference is stored locally`() = runTest {
        // When
        customLanguageRepository.observe().test {
            // Then
            assertEquals(CustomAppLanguagePreference("Auto-Detect"), awaitItem())
            awaitComplete()
        }
    }
}
