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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class ApplyContentSearchMobileDataPreferenceTest {

    private val isContentSearchAllowedOnMobileData = mockk<IsContentSearchAllowedOnMobileData>()
    private val contentSearchRepository = mockk<ContentSearchRepository>()

    private val applyContentSearchMobileDataPreference = ApplyContentSearchMobileDataPreference(
        isContentSearchAllowedOnMobileData,
        contentSearchRepository
    )

    @Test
    fun `pushes the app-wide preference when the account flag disagrees with it`() = runTest {
        // Given
        coEvery { isContentSearchAllowedOnMobileData() } returns false
        coEvery { contentSearchRepository.isMeteredConnectionAllowed(UserId) } returns true.right()
        coEvery { contentSearchRepository.setMeteredConnectionAllowed(UserId, false) } returns Unit.right()

        // When
        applyContentSearchMobileDataPreference(UserId)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserId, false) }
    }

    @Test
    fun `does not write when the account flag already matches the app-wide preference`() = runTest {
        // Given
        coEvery { isContentSearchAllowedOnMobileData() } returns true
        coEvery { contentSearchRepository.isMeteredConnectionAllowed(UserId) } returns true.right()

        // When
        applyContentSearchMobileDataPreference(UserId)

        // Then
        coVerify(exactly = 0) { contentSearchRepository.setMeteredConnectionAllowed(any(), any()) }
    }

    @Test
    fun `writes the preference when the account flag cannot be read`() = runTest {
        // Given
        coEvery { isContentSearchAllowedOnMobileData() } returns true
        coEvery {
            contentSearchRepository.isMeteredConnectionAllowed(UserId)
        } returns DataError.Local.Unknown.left()
        coEvery { contentSearchRepository.setMeteredConnectionAllowed(UserId, true) } returns Unit.right()

        // When
        applyContentSearchMobileDataPreference(UserId)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserId, true) }
    }

    @Test
    fun `swallows a failure to write the preference`() = runTest {
        // Given
        coEvery { isContentSearchAllowedOnMobileData() } returns false
        coEvery { contentSearchRepository.isMeteredConnectionAllowed(UserId) } returns true.right()
        coEvery {
            contentSearchRepository.setMeteredConnectionAllowed(UserId, false)
        } returns DataError.Local.Unknown.left()

        // When
        applyContentSearchMobileDataPreference(UserId)

        // Then
        coVerify(exactly = 1) { contentSearchRepository.setMeteredConnectionAllowed(UserId, false) }
    }

    private companion object {

        val UserId = UserId("user-id")
    }
}
