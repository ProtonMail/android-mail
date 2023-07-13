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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ShouldShowEmbeddedImagesTest {

    private val mailSettingsRepository = mockk<MailSettingsRepository>()

    private val shouldShowEmbeddedImages = ShouldShowEmbeddedImages(mailSettingsRepository)

    @Test
    fun `should not show embedded images when repository returns None as the setting value`() = runTest {
        // Given
        coEvery {
            mailSettingsRepository.getMailSettings(UserIdTestData.userId)
        } returns MailSettingsTestData.buildMailSettings(showImages = IntEnum(0, ShowImage.None))
        val expected = false

        // When
        val actual = shouldShowEmbeddedImages(UserIdTestData.userId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should not show embedded images when repository returns Remote as the setting value`() = runTest {
        // Given
        coEvery {
            mailSettingsRepository.getMailSettings(UserIdTestData.userId)
        } returns MailSettingsTestData.buildMailSettings(showImages = IntEnum(1, ShowImage.Remote))
        val expected = false

        // When
        val actual = shouldShowEmbeddedImages(UserIdTestData.userId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should show embedded images when repository returns Embedded as the setting value`() = runTest {
        // Given
        coEvery {
            mailSettingsRepository.getMailSettings(UserIdTestData.userId)
        } returns MailSettingsTestData.buildMailSettings(showImages = IntEnum(2, ShowImage.Embedded))
        val expected = true

        // When
        val actual = shouldShowEmbeddedImages(UserIdTestData.userId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should show embedded images when repository returns Both as the setting value`() = runTest {
        // Given
        coEvery {
            mailSettingsRepository.getMailSettings(UserIdTestData.userId)
        } returns MailSettingsTestData.buildMailSettings(showImages = IntEnum(3, ShowImage.Both))
        val expected = true

        // When
        val actual = shouldShowEmbeddedImages(UserIdTestData.userId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should not show embedded images when repository returns null as the setting value`() = runTest {
        // Given
        coEvery {
            mailSettingsRepository.getMailSettings(UserIdTestData.userId)
        } returns MailSettingsTestData.buildMailSettings(showImages = null)
        val expected = false

        // When
        val actual = shouldShowEmbeddedImages(UserIdTestData.userId)

        // Then
        assertEquals(expected, actual)
    }
}
