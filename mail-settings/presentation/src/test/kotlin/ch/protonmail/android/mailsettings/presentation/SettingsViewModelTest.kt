/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.ObserveAppSettings
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.settings.AccountInfo
import ch.protonmail.android.mailsettings.presentation.settings.AppInformation
import ch.protonmail.android.mailsettings.presentation.settings.GetAppInformation
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.SettingsViewModel
import ch.protonmail.android.mailsettings.presentation.testdata.AppSettingsTestData
import ch.protonmail.android.mailsettings.presentation.testdata.UserIdTestData
import ch.protonmail.android.mailsettings.presentation.testdata.UserTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource.Local
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SettingsViewModelTest {

    private val userIdFlow = MutableSharedFlow<UserId?>()
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns userIdFlow
    }

    private val userFlow = MutableSharedFlow<DataResult<User?>>()
    private val userManager = mockk<UserManager> {
        every { this@mockk.getUserFlow(UserIdTestData.userId) } returns userFlow
    }

    private val appSettingsFlow = MutableSharedFlow<AppSettings>()
    private val observeAppSettings = mockk<ObserveAppSettings> {
        every { this@mockk.invoke() } returns appSettingsFlow
    }

    private val getAppInformation = mockk<GetAppInformation> {
        every { this@mockk.invoke() } returns AppInformation("6.0.0-alpha")
    }

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = SettingsViewModel(
            accountManager,
            userManager,
            observeAppSettings,
            getAppInformation
        )
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has account info when user manager returns valid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // When
            primaryUserIdIs(UserIdTestData.userId)
            userManagerSuccessfullyReturns(UserTestData.user)

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
    fun `state has null account info when user manager returns an error`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // When
            primaryUserIdIs(UserIdTestData.userId)
            userManagerReturnsError()

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.account)
        }
    }

    @Test
    fun `state has app settings info when get app settings use case returns valid data`() =
        runTest {
            viewModel.state.test {
                // Given
                initialStateEmitted()
                primaryUserIdIs(UserIdTestData.userId)
                userManagerSuccessfullyReturns(UserTestData.user)

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

    @Test
    fun `state has app version info when get app info returns them`() = runTest {
        viewModel.state.test {
            // Given
            every { getAppInformation() } returns AppInformation("6.0.0-alpha-01")
            initialStateEmitted()
            primaryUserIdIs(UserIdTestData.userId)
            userManagerSuccessfullyReturns(UserTestData.user)
            appSettingsFlow.emit(AppSettingsTestData.appSettings)

            // Then
            val actual = awaitItem() as Data
            assertEquals(AppInformation("6.0.0-alpha-01"), actual.appInformation)
        }
    }

    private suspend fun userManagerReturnsError() {
        userFlow.emit(Error.Local("Test-IOException", IOException("Test")))
    }

    private suspend fun userManagerSuccessfullyReturns(user: User) {
        userFlow.emit(Success(Local, user))
    }

    private suspend fun primaryUserIdIs(userId: UserId) {
        userIdFlow.emit(userId)
    }

    private suspend fun FlowTurbine<SettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
