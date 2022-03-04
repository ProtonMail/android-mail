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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.domain.ObservePrimaryUserSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.testdata.UserSettingsTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.usersettings.domain.entity.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccountSettingsViewModelTest {

    private val userSettingsFlow = MutableSharedFlow<UserSettings?>()
    private val observePrimaryUserSettings = mockk<ObservePrimaryUserSettings> {
        every { this@mockk.invoke() } returns userSettingsFlow
    }

    private lateinit var viewModel: AccountSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = AccountSettingsViewModel(
            observePrimaryUserSettings
        )
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has recovery email when use case returns a valid one`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            userSettingsFlow.emit(UserSettingsTestData.userSettings)

            // Then
            val actual = awaitItem() as Data
            val expected = UserSettingsTestData.recoverEmailRawValue
            assertEquals(expected, actual.recoveryEmail)
        }
    }

    @Test
    fun `state has null recovery email when use case returns an invalid one`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            userSettingsFlow.emit(UserSettingsTestData.emptyUserSettings)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.recoveryEmail)
        }
    }

    private suspend fun FlowTurbine<AccountSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }
}
