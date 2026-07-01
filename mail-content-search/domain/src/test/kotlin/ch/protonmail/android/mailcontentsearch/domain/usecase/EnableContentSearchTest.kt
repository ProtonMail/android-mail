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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class EnableContentSearchTest {

    private val userId = UserId("user-1")
    private val settingsRepository = mockk<ContentSearchSettingsRepository> {
        coEvery { setEnabled(userId, true) } returns Unit.right()
    }
    private val preferencesRepository = mockk<ContentSearchPreferencesRepository> {
        coEvery { clearUserOptedOut(userId) } returns Unit.right()
    }
    private val enableContentSearch = EnableContentSearch(settingsRepository, preferencesRepository)

    @Test
    fun `turns on the preference then clears the deliberate opt-out`() = runTest {
        // When
        enableContentSearch(userId)

        // Then
        coVerifyOrder {
            settingsRepository.setEnabled(userId, true)
            preferencesRepository.clearUserOptedOut(userId)
        }
    }

    @Test
    fun `does not clear the opt-out when enabling fails`() = runTest {
        // Given
        coEvery { settingsRepository.setEnabled(userId, true) } returns DataError.Local.Unknown.left()

        // When
        enableContentSearch(userId)

        // Then
        coVerify(exactly = 0) { preferencesRepository.clearUserOptedOut(userId) }
    }
}
