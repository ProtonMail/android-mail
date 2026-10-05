/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.usecase

import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ShouldShowNps
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShouldShowNpsTest {

    private val inMemoryMailboxRepository = mockk<InMemoryMailboxRepository>()
    private val isNpsEnabledInApp = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns true
    }
    private val isNpsFeedbackEnabled = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns true
    }
    private val showRatingBooster = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns false
    }

    private val shouldShowNps = ShouldShowNps(
        inMemoryMailboxRepository,
        isNpsEnabledInApp,
        isNpsFeedbackEnabled,
        showRatingBooster
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the user has not left the reader yet, when observed, then the survey is not offered`() = runTest {
        // Given
        every { inMemoryMailboxRepository.observeScreenViewCount() } returns flowOf(0)

        // When
        val result = shouldShowNps().toList()

        // Then
        assertEquals(listOf(false), result)
    }

    @Test
    fun `given the user is back from the reader, when observed, then the survey is offered`() = runTest {
        // Given
        every { inMemoryMailboxRepository.observeScreenViewCount() } returns flowOf(0, 1)

        // When
        val result = shouldShowNps().toList()

        // Then
        assertEquals(listOf(false, true), result)
    }

    @Test
    fun `given the backend NPS flag is off, when back from the reader, then the survey is not offered`() = runTest {
        // Given
        coEvery { isNpsFeedbackEnabled.get() } returns false
        every { inMemoryMailboxRepository.observeScreenViewCount() } returns flowOf(1)

        // When
        val result = shouldShowNps().toList()

        // Then
        assertEquals(listOf(false), result)
    }

    @Test
    fun `given the client NPS flag is off, when back from the reader, then the survey is not offered`() = runTest {
        // Given
        coEvery { isNpsEnabledInApp.get() } returns false
        every { inMemoryMailboxRepository.observeScreenViewCount() } returns flowOf(1)

        // When
        val result = shouldShowNps().toList()

        // Then
        assertEquals(listOf(false), result)
    }
}
