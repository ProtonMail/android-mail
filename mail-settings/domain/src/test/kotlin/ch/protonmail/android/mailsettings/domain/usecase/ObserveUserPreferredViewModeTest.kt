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
package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.type.IntEnum
import org.junit.Test
import kotlin.test.assertEquals

class ObserveUserPreferredViewModeTest {

    private val observeMailSettings = mockk<ObserveMailSettings>()

    private val observeUserPreferredViewMode = ObserveUserPreferredViewMode(
        observeMailSettings
    )

    @Test
    fun `emits view mode from mail settings when present`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expected = ViewMode.NoConversationGrouping
        coEvery { observeMailSettings(userId) } returns flowOf(MailSettingsTestData.mailSettingsMessageViewMode)

        // When
        val actual = observeUserPreferredViewMode(userId).first()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `emits default view mode when mail settings contain no valid view mode info`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expected = ObserveUserPreferredViewMode.DefaultViewMode
        val mailSettings = MailSettingsTestData.mailSettings.copy(
            viewMode = IntEnum(3, null)
        )
        coEvery { observeMailSettings(userId) } returns flowOf(mailSettings)

        // When
        val actual = observeUserPreferredViewMode(userId).first()

        // Then
        assertEquals(expected, actual)
    }
}
