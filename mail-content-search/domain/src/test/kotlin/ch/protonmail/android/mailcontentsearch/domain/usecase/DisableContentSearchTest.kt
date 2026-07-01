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

internal class DisableContentSearchTest {

    private val userId = UserId("user-1")
    private val settingsRepository = mockk<ContentSearchSettingsRepository> {
        coEvery { setEnabled(userId, false) } returns Unit.right()
    }
    private val preferencesRepository = mockk<ContentSearchPreferencesRepository> {
        coEvery { markUserOptedOut(userId) } returns Unit.right()
    }
    private val disableContentSearch = DisableContentSearch(settingsRepository, preferencesRepository)

    @Test
    fun `turns off the preference then records the deliberate opt-out`() = runTest {
        // When
        disableContentSearch(userId)

        // Then
        coVerifyOrder {
            settingsRepository.setEnabled(userId, false)
            preferencesRepository.markUserOptedOut(userId)
        }
    }

    @Test
    fun `does not record an opt-out when disabling fails`() = runTest {
        // Given
        coEvery { settingsRepository.setEnabled(userId, false) } returns DataError.Local.Unknown.left()

        // When
        disableContentSearch(userId)

        // Then
        coVerify(exactly = 0) { preferencesRepository.markUserOptedOut(userId) }
    }
}
