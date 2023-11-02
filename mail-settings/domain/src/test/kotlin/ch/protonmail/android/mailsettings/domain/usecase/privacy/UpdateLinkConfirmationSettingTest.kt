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
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class UpdateLinkConfirmationSettingTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val mailSettingsRepository = mockk<MailSettingsRepository>()
    private val useCase = UpdateLinkConfirmationSetting(
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
    fun `when link confirmation update is triggered, the action is propagated to the repository`() = runTest {
        // Given
        val userId = UserId("123")
        val settingsMock = mockk<MailSettings>()
        every { observePrimaryUserId() } returns flowOf(userId)
        coEvery { mailSettingsRepository.updateConfirmLink(any(), true) } returns settingsMock
        coEvery { settingsMock.confirmLink } returns false

        val expectedResult = Unit.right()

        // When
        val result = useCase(true)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) {
            mailSettingsRepository.updateConfirmLink(userId, true)
        }
        confirmVerified(mailSettingsRepository)
    }
}
