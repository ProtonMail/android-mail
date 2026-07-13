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

package ch.protonmail.android.mailcontentsearch.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.AllowMobileDataForContentSearchIndexing
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ContentSearchPreferencesRepositoryImplTest {

    private val appSettingsRepository = mockk<AppSettingsRepository>()
    private val repository = ContentSearchPreferencesRepositoryImpl(appSettingsRepository)

    @Test
    fun `returns the allow mobile data value stored in the rust app settings`() = runTest {
        // Given
        val appSettings = AppSettings.default().copy(
            useMobileDataForContentSearchIndexing = AllowMobileDataForContentSearchIndexing.NotEnabled
        )
        every { appSettingsRepository.observeAppSettings() } returns flowOf(appSettings)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `defaults to allowing mobile data when the rust app settings enable it`() = runTest {
        // Given
        val appSettings = AppSettings.default().copy(
            useMobileDataForContentSearchIndexing = AllowMobileDataForContentSearchIndexing.Enabled
        )
        every { appSettingsRepository.observeAppSettings() } returns flowOf(appSettings)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `persists the allow mobile data value through the rust app settings repository`() = runTest {
        // Given
        coEvery { appSettingsRepository.updateUseMobileDataForContentSearch(true) } returns Unit.right()

        // When
        val result = repository.setAllowMobileData(true)

        // Then
        coVerify { appSettingsRepository.updateUseMobileDataForContentSearch(true) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `returns a preferences error when the rust app settings update fails`() = runTest {
        // Given
        coEvery {
            appSettingsRepository.updateUseMobileDataForContentSearch(true)
        } returns DataError.Local.NotFound.left()

        // When
        val result = repository.setAllowMobileData(true)

        // Then
        assertEquals(PreferencesError.left(), result)
    }
}
