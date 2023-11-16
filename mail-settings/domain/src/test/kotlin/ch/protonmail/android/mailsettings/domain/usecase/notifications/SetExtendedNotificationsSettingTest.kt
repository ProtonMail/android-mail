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

package ch.protonmail.android.mailsettings.domain.usecase.notifications

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.repository.NotificationsSettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SetExtendedNotificationsSettingTest {

    private val notificationsSettingsRepository = mockk<NotificationsSettingsRepository>()
    private val setNotificationsExtendedSetting = SetExtendedNotificationsSetting(
        notificationsSettingsRepository
    )

    @Test
    fun `should not return any error when the update is successful`() = runTest {
        // Given
        coEvery {
            notificationsSettingsRepository.updateExtendedNotificationsSetting(false)
        } returns Unit.right()

        // When
        val result = setNotificationsExtendedSetting(false)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should propagate the error when it is unable to update the value locally`() = runTest {
        // Given
        coEvery {
            notificationsSettingsRepository.updateExtendedNotificationsSetting(false)
        } returns PreferencesError.left()

        // When
        val result = setNotificationsExtendedSetting(false)

        // Then
        assertEquals(SetExtendedNotificationsSetting.Error.UpdateFailed.left(), result)
    }
}
