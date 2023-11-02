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

package ch.protonmail.android.mailsettings.domain.usecase.privacy

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class UpdateShowRemoteContentSettingTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val mailSettingsRepository = mockk<MailSettingsRepository>()
    private val useCase = UpdateShowRemoteContentSetting(
        observePrimaryUserId,
        mailSettingsRepository
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when user id is null an error is returned`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)
        val expectedResult = DataError.Local.NoDataCached.left()

        // When
        val result = useCase(true)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when show images value is null, an error is returned`() = runTest {
        // Given
        val settingsMock = mockk<MailSettings>()
        every { observePrimaryUserId() } returns flowOf(UserId("123"))
        coEvery { mailSettingsRepository.getMailSettings(any(), false) } returns settingsMock
        coEvery { settingsMock.showImages } returns null

        val expectedResult = DataError.Local.NoDataCached.left()

        // When
        val result = useCase(true)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when the target remote content value is the same as the current one, the update is not triggered`() = runTest {
        // Given
        val userId = UserId("123")
        val settingsMock = mockk<MailSettings>()
        every { observePrimaryUserId() } returns flowOf(userId)
        coEvery { mailSettingsRepository.getMailSettings(any(), false) } returns settingsMock
        coEvery { settingsMock.showImages } returns IntEnum(0, ShowImage.None)

        val expectedResult = Unit.right()

        // When
        val result = useCase(false)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) {
            mailSettingsRepository.getMailSettings(userId, refresh = false)
        }
        coVerify(exactly = 0) {
            mailSettingsRepository.updateShowImages(any(), any())
        }
        confirmVerified(mailSettingsRepository)
    }

    @Test
    fun `when the target remote content value is different than the current one, the update is triggered`() = runTest {
        // Given
        val settingsMock = mockk<MailSettings>()
        val userId = UserId("123")
        val updateValue = slot<ShowImage>()
        val expectedResult = Unit.right()
        val expectedShowImagesValue = ShowImage.Both.value

        every { observePrimaryUserId() } returns flowOf(userId)
        coEvery { mailSettingsRepository.getMailSettings(any(), refresh = false) } returns settingsMock
        coEvery { mailSettingsRepository.updateShowImages(any(), any()) } returns settingsMock
        coEvery { settingsMock.showImages } returns IntEnum(2, ShowImage.Embedded)

        // When
        val result = useCase(true)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) {
            mailSettingsRepository.getMailSettings(userId, refresh = false)
            mailSettingsRepository.updateShowImages(userId, capture(updateValue))
        }
        confirmVerified(mailSettingsRepository)
        assertEquals(expectedShowImagesValue, updateValue.captured.value)
    }
}
