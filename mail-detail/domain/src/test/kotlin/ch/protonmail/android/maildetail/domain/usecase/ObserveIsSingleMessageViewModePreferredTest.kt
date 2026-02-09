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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserPreferredViewMode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveIsSingleMessageViewModePreferredTest {

    private val observeUserPreferredViewMode = mockk<ObserveUserPreferredViewMode>()

    private val observeIsSingleMessageViewModePreferred = ObserveIsSingleMessageViewModePreferred(
        observeUserPreferredViewMode = observeUserPreferredViewMode
    )

    @Test
    fun `emits true when view mode is NoConversationGrouping`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        coEvery { observeUserPreferredViewMode(userId) } returns flowOf(ViewMode.NoConversationGrouping)

        // When
        val actual = observeIsSingleMessageViewModePreferred(userId).toList()

        // Then
        assertEquals(listOf(true), actual)
    }

    @Test
    fun `emits false when view mode is ConversationGrouping`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        coEvery { observeUserPreferredViewMode(userId) } returns flowOf(ViewMode.ConversationGrouping)

        // When
        val actual = observeIsSingleMessageViewModePreferred(userId).toList()

        // Then
        assertEquals(listOf(false), actual)
    }

}
