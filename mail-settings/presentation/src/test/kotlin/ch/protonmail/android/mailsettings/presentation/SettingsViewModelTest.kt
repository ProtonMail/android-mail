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

import app.cash.turbine.test
import ch.protonmail.android.mailsettings.presentation.State.Data
import ch.protonmail.android.mailsettings.presentation.State.Loading
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

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = SettingsViewModel(
            accountManager,
            userManager
        )
    }

    @Test
    fun emitsLoadingStateWhenInitialised() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun stateHasAccountDataWhenUserManagerReturnsValidUser() = runTest {
        viewModel.state.test {
            awaitItem() as Loading // Initial state

            // When
            userIdFlow.emit(UserIdTestData.userId)
            userFlow.emit(DataResult.Success(Local, UserTestData.user))

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
    fun stateHasNullAccountDataWhenUserManagerReturnsAnError() = runTest {
        viewModel.state.test {
            awaitItem() as Loading // Initial state

            // When
            userIdFlow.emit(UserIdTestData.userId)
            userFlow.emit(DataResult.Error.Local("Test-IOException", IOException("Test")))

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.account)
        }
    }
}
