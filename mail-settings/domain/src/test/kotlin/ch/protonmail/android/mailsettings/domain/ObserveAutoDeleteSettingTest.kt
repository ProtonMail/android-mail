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

package ch.protonmail.android.mailsettings.domain

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData.buildMailSettings
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class ObserveAutoDeleteSettingTest {

    private val mutableMailSettings = MutableSharedFlow<DataResult<MailSettings>>(replay = 1)
    private val mailSettingsRepository = mockk<MailSettingsRepository> {
        coEvery { getMailSettingsFlow(any()) } returns mutableMailSettings
    }
    private val isPaidMailUser = mockk<IsPaidMailUser> {
        coEvery { this@mockk(UserIdTestData.userId) } returns false.right()
    }
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every { this@mockk(UpsellingEntryPoint.Feature.AutoDelete) } returns flowOf(false)
    }
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns flowOf(UserIdTestData.userId)
    }

    private lateinit var usecase: ObserveAutoDeleteSetting

    @Before
    fun setUp() {
        usecase = ObserveAutoDeleteSetting(
            mailSettingsRepository,
            isPaidMailUser,
            observeUpsellingVisibility,
            observePrimaryUserId
        )
    }

    @Test
    fun `mail settings returns 'NotSet-FreeUser' when setting is null and user is free`() = runTest {
        // Given
        emitAutoDeleteSetting(null)
        expectUserIsPaid(false)

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertTrue(actual is AutoDeleteSetting.NotSet.FreeUser)
        }
    }

    @Test
    fun `mail settings returns 'NotSet-PaidUser' when setting is null and user is paid`() = runTest {
        // Given
        emitAutoDeleteSetting(null)
        expectUserIsPaid(true)

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertTrue(actual is AutoDeleteSetting.NotSet.PaidUser)
        }
    }

    @Test
    fun `mail settings returns 'NotSet-FreeUser-UpsellingOn' when setting is null and user is free and upselling is on`() =
        runTest {
            // Given
            emitAutoDeleteSetting(null)
            expectUserIsPaid(false)
            expectShouldShowUpselling(true)

            // When
            usecase.invoke().test {
                // Then
                val actual = awaitItem()
                assertTrue(actual is AutoDeleteSetting.NotSet.FreeUser.UpsellingOn)
            }
        }

    @Test
    fun `mail settings returns 'NotSet-FreeUser-UpsellingOff' when setting is null and user is free and upselling is off`() =
        runTest {
            // Given
            emitAutoDeleteSetting(null)
            expectUserIsPaid(false)
            expectShouldShowUpselling(false)

            // When
            usecase.invoke().test {
                // Then
                val actual = awaitItem()
                assertTrue(actual is AutoDeleteSetting.NotSet.FreeUser.UpsellingOff)
            }
        }

    @Test
    fun `mail settings returns 'Enabled' when setting is enabled`() = runTest {
        // Given
        emitAutoDeleteSetting(30)

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertEquals(AutoDeleteSetting.Enabled, actual)
        }
    }

    @Test
    fun `mail settings returns 'Disabled' when setting is disabled`() = runTest {
        // Given
        emitAutoDeleteSetting(0)

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertEquals(AutoDeleteSetting.Disabled, actual)
        }
    }

    @Test
    fun `mail settings returns 'NotSet' when setting is null`() = runTest {
        // Given
        emitAutoDeleteSetting(null)

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertTrue(actual is AutoDeleteSetting.NotSet)
        }
    }

    @Test
    fun `mail settings returns 'disabled' on error getting settings from repo`() = runTest {
        // Given
        emitAutoDeleteSettingError()

        // When
        usecase.invoke().test {
            // Then
            val actual = awaitItem()
            assertTrue(actual is AutoDeleteSetting.Disabled)
        }
    }

    private fun expectShouldShowUpselling(value: Boolean) {
        every { observeUpsellingVisibility(UpsellingEntryPoint.Feature.AutoDelete) } returns flowOf(value)
    }

    private fun expectUserIsPaid(value: Boolean) {
        coEvery { isPaidMailUser(UserIdTestData.userId) } returns value.right()
    }

    private suspend fun emitAutoDeleteSettingError() {
        mutableMailSettings.emit(DataResult.Error.Local(message = "Error", cause = null))
    }

    private suspend fun emitAutoDeleteSetting(value: Int?) {
        mutableMailSettings.emit(
            DataResult.Success(
                source = ResponseSource.Local,
                value = buildMailSettings(
                    autoDeleteSpamAndTrashDays = value
                )
            )
        )
    }
}
