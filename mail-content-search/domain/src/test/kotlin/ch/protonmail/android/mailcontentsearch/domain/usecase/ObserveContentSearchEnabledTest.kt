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

import app.cash.turbine.test
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ObserveContentSearchEnabledTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchSettingsRepository>()
    private val observeContentSearchEnabled = ObserveContentSearchEnabled(repository)

    @Test
    fun `emits the enabled values observed from the repository`() = runTest {
        // Given
        every { repository.observeIsEnabled(userId) } returns flowOf(false, true)

        // When + Then
        observeContentSearchEnabled(userId).test {
            assertFalse(awaitItem())
            assertTrue(awaitItem())
            awaitComplete()
        }
    }
}
