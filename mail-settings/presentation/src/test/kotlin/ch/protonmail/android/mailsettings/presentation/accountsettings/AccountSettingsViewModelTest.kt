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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.NotLoggedIn
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserTestData
import ch.protonmail.android.testdata.usersettings.UserSettingsTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.user.domain.entity.User
import me.proton.core.usersettings.domain.entity.UserSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountSettingsViewModelTest {

    private val userFlow = MutableSharedFlow<User?>()
    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(userId)
    }
    private val observeUser = mockk<ObserveUser> {
        every { this@mockk(userId) } returns userFlow
    }

    private val userSettingsFlow = MutableSharedFlow<UserSettings?>()
    private val observeUserSettings = mockk<ObserveUserSettings> {
        every { this@mockk(userId) } returns userSettingsFlow
    }

    private val mailSettingsFlow = MutableSharedFlow<MailSettings?>()
    private val observeMailSettings = mockk<ObserveMailSettings> {
        every { this@mockk(userId) } returns mailSettingsFlow
    }

    private val viewModel by lazy {
        AccountSettingsViewModel(
            accountManager = accountManager,
            observeUser = observeUser,
            observeUserSettings = observeUserSettings,
            observeMailSettings = observeMailSettings
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `emits loading state when initialised`() = runTest {
        viewModel.state.test {
            assertEquals(Loading, awaitItem())
        }
    }

    @Test
    fun `state has recovery email when use case returns valid user settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            mailSettingsExist()

            // When
            userSettingsFlow.emit(UserSettingsTestData.userSettings)

            // Then
            val actual = awaitItem() as Data
            val expected = UserSettingsTestData.RECOVERY_EMAIL_RAW
            assertEquals(expected, actual.recoveryEmail)
        }
    }

    @Test
    fun `state has null recovery email when use case returns invalid user settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            mailSettingsExist()

            // When
            userSettingsFlow.emit(UserSettingsTestData.emptyUserSettings)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.recoveryEmail)
        }
    }

    @Test
    fun `state has default email when use case returns a valid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Data
            val expected = UserTestData.USER_EMAIL_RAW
            assertEquals(expected, actual.defaultEmail)
        }
    }

    @Test
    fun `state has null default email when use case returns an invalid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.defaultEmail)
        }
    }

    @Test
    fun `state has mailbox sizes when use case returns a valid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Data
            assertEquals(UserTestData.MAX_SPACE_RAW, actual.mailboxSize)
            assertEquals(UserTestData.USED_SPACE_RAW, actual.mailboxUsedSpace)
        }
    }

    @Test
    fun `state has null mailbox sizes when use case returns an invalid user`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            userSettingsExist()
            mailSettingsExist()

            // When
            userFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.mailboxSize)
            assertNull(actual.mailboxUsedSpace)
        }
    }

    @Test
    fun `state has conversation mode flag when use case returns a valid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()

            // When
            mailSettingsFlow.emit(MailSettingsTestData.mailSettings)

            // Then
            val actual = awaitItem() as Data
            assertEquals(false, actual.isConversationMode)
        }
    }

    @Test
    fun `state has null conversation mode when use case returns invalid mail settings`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()
            primaryUserExists()
            userSettingsExist()

            // When
            mailSettingsFlow.emit(null)

            // Then
            val actual = awaitItem() as Data
            assertNull(actual.isConversationMode)
        }
    }

    @Test
    fun `emits right state when no user is logged in`() = runTest {
        // given
        givenNoLoggedInUser()

        // when
        viewModel.state.test {

            // then
            assertEquals(NotLoggedIn, awaitItem())
        }
    }

    private fun givenNoLoggedInUser() {
        every { accountManager.getPrimaryUserId() } returns flowOf(null)
    }

    private suspend fun ReceiveTurbine<AccountSettingsState>.initialStateEmitted() {
        awaitItem() as Loading
    }

    private suspend fun primaryUserExists() {
        userFlow.emit(UserTestData.Primary)
    }

    private suspend fun userSettingsExist() {
        userSettingsFlow.emit(UserSettingsTestData.userSettings)
    }

    private suspend fun mailSettingsExist() {
        mailSettingsFlow.emit(MailSettingsTestData.mailSettings)
    }
}
