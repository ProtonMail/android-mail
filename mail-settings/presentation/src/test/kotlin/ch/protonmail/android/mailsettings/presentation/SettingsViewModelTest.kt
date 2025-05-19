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

package ch.protonmail.android.mailsettings.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.usecase.ClearLocalStorage
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAppSettings
import ch.protonmail.android.mailsettings.presentation.settings.AccountInfo
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.SettingsViewModel
import ch.protonmail.android.mailsettings.presentation.testdata.AppSettingsTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.BeforeTest

class SettingsViewModelTest {

    private val userFlow = MutableSharedFlow<User?>()
    private val observePrimaryUser = mockk<ObservePrimaryUser> {
        every { this@mockk.invoke() } returns userFlow
    }

    private val appSettingsFlow = MutableSharedFlow<AppSettings>()
    private val observeAppSettings = mockk<ObserveAppSettings> {
        every { this@mockk.invoke() } returns appSettingsFlow
    }

    private val clearLocalStorage = mockk<ClearLocalStorage>()

    private val appInformation = AppInformation(appVersionName = "6.0.0-alpha")
    private val logsExportFeatureSetting = LogsExportFeatureSetting(enabled = false, internalEnabled = false)

    private val viewModel by lazy {
        SettingsViewModel(
            appInformation,
            observeAppSettings,
            observePrimaryUser,
            clearLocalStorage,
            logsExportFeatureSetting
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        MockKAnnotations.init(this)
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has account info when there is a valid primary user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // When
            userFlow.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Data
            val expected = AccountInfo(
                UserTestData.USER_DISPLAY_NAME_RAW,
                UserTestData.USER_EMAIL_RAW
            )
            assertEquals(expected, actual.account)
        }
    }

    @Test
    fun `state has user name info when primary user display name is empty`() = runTest {
        viewModel.state.test {
            // Given
            val user = UserSample.Primary
            initialStateEmitted()
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // When
            userFlow.emit(user.copy(displayName = ""))

            // Then
            val actual = awaitItem() as Data
            val expected = AccountInfo(
                name = requireNotNull(user.displayName),
                email = requireNotNull(user.email)
            )
            assertEquals(expected, actual.account)
        }
    }

    @Test
    fun `state has null account info when there is no valid primary user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // When
            userFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.account)
        }
    }

    @Test
    fun `state has app settings info when get app settings use case returns valid data`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userFlow.emit(UserTestData.Primary)

            // When
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // Then
            val actual = awaitItem() as Data
            val expected = AppSettings(
                hasAutoLock = false,
                hasAlternativeRouting = true,
                customAppLanguage = null,
                hasCombinedContacts = true
            )
            assertEquals(expected, actual.appSettings)
        }
    }

    private suspend fun ReceiveTurbine<SettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
