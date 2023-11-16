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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
import ch.protonmail.android.mailsettings.domain.repository.NotificationsSettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetExtendedNotificationsSettingTest {

    private val notificationsSettingsRepository = mockk<NotificationsSettingsRepository>()
    private val getExtendedNotificationsSetting = GetExtendedNotificationsSetting(notificationsSettingsRepository)

    @Test
    fun `should return the correct value when fetched from the data store`() = runTest {
        // Given
        coEvery {
            notificationsSettingsRepository.observeExtendedNotificationsSetting()
        } returns flowOf(BasePreference.right())

        // When
        val result = getExtendedNotificationsSetting()

        // Then
        assertEquals(BasePreference.right(), result)
    }

    @Test
    fun `should return an error when unable to fetch from the data store`() = runTest {
        // Given
        coEvery {
            notificationsSettingsRepository.observeExtendedNotificationsSetting()
        } returns flowOf(PreferencesError.left())

        // When
        val result = getExtendedNotificationsSetting()

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    private companion object {

        val BasePreference = ExtendedNotificationPreference(true)
    }
}
