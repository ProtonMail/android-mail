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

package ch.protonmail.android.navigation

import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.test
import ch.protonmail.android.mailnotifications.presentation.NotificationPermissionOrchestrator
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.testdata.AccountTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.AccountManagerObserver
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDeviceSecretNeeded
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionForceLogout
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.MissingScopeObserver
import me.proton.core.humanverification.presentation.HumanVerificationManagerObserver
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.report.presentation.ReportOrchestrator
import me.proton.core.usersettings.presentation.UserSettingsOrchestrator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherViewModelTest {

    private val authOrchestrator = mockk<AuthOrchestrator>(relaxUnitFun = true)
    private val plansOrchestrator = mockk<PlansOrchestrator>(relaxUnitFun = true)
    private val reportOrchestrator = mockk<ReportOrchestrator>(relaxUnitFun = true)
    private val userSettingsOrchestrator = mockk<UserSettingsOrchestrator>(relaxUnitFun = true)
    private val notificationPermissionOrchestrator = mockk<NotificationPermissionOrchestrator>(
        relaxUnitFun = true
    )

    private val accountListFlow = MutableStateFlow<List<Account>>(emptyList())
    private val accountManager = mockk<AccountManager>(relaxUnitFun = true) {
        every { getAccounts() } returns accountListFlow
    }

    private val context = mockk<AppCompatActivity> {
        every { lifecycle } returns mockk()
    }

    private val user1Username = "username"

    private lateinit var viewModel: LauncherViewModel

    @BeforeTest
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = buildViewModel()
    }

    @AfterTest
    fun teardown() {
        unmockkStatic(
            AccountManagerObserver::class,
            HumanVerificationManagerObserver::class,
            MissingScopeObserver::class
        )
    }

    @Test
    fun `when no account then AccountNeeded`() = runTest {
        // GIVEN
        accountListFlow.emit(emptyList())
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherState.AccountNeeded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when all accounts are disabled then AccountNeeded`() = runTest {
        // GIVEN
        accountListFlow.emit(listOf(AccountTestData.disabledAccount))
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherState.AccountNeeded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when one ready account then PrimaryExist`() = runTest {
        // GIVEN
        accountListFlow.emit(listOf(AccountTestData.readyAccount))
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherState.PrimaryExist, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when adding first account`() = runTest {
        // GIVEN
        accountListFlow.emit(emptyList())
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherState.AccountNeeded, awaitItem())

            accountListFlow.emit(listOf(AccountTestData.notReadyAccount))
            assertEquals(LauncherState.StepNeeded, awaitItem())

            accountListFlow.emit(listOf(AccountTestData.readyAccount))
            assertEquals(LauncherState.PrimaryExist, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when adding a second account PrimaryExist state do not change`() = runTest {
        // GIVEN
        accountListFlow.emit(listOf(AccountTestData.readyAccount))
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherState.PrimaryExist, awaitItem())

            accountListFlow.emit(
                listOf(AccountTestData.readyAccount, AccountTestData.notReadyAccount)
            )
            accountListFlow.emit(
                listOf(AccountTestData.readyAccount, AccountTestData.readyAccount)
            )

            val events = cancelAndConsumeRemainingEvents()
            assertEquals(0, events.size)
        }
    }

    @Test
    fun `when addAccount is called, startAddAccountWorkflow`() = runTest {
        // WHEN
        viewModel.submit(LauncherViewModel.Action.AddAccount)
        // THEN
        verify {
            authOrchestrator.startAddAccountWorkflow()
        }
    }

    @Test
    fun `when signIn is called, startLoginWorkflow`() = runTest {
        // WHEN
        viewModel.submit(LauncherViewModel.Action.SignIn(userId = null))
        // THEN
        verify { authOrchestrator.startLoginWorkflow(any()) }
    }

    @Test
    fun `when signIn with userId is called, startLoginWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getAccount(userId) } returns flowOf(AccountTestData.readyAccount)
        // WHEN
        viewModel.submit(LauncherViewModel.Action.SignIn(userId))
        // THEN
        verify { authOrchestrator.startLoginWorkflow(user1Username) }
    }

    @Test
    fun `when switch is called on disabled account, startLoginWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getAccount(userId) } returns flowOf(AccountTestData.disabledAccount)
        // WHEN
        viewModel.submit(LauncherViewModel.Action.Switch(userId))
        // THEN
        verify { authOrchestrator.startLoginWorkflow(user1Username) }
    }

    @Test
    fun `when switch is called on ready account, setPrimary`() = runTest {
        // GIVEN
        every { accountManager.getAccount(userId) } returns flowOf(AccountTestData.readyAccount)
        // WHEN
        viewModel.submit(LauncherViewModel.Action.Switch(userId))
        // THEN
        coVerify { accountManager.setAsPrimary(userId) }
    }

    @Test
    fun `when register is called, verify AccountManagerObserver subscriptions`() = runTest {
        // GIVEN

        // AccountManager
        mockkStatic(AccountManager::observe)
        val amObserver = mockAccountManagerObserver()
        every { accountManager.observe(any(), any()) } returns amObserver

        // WHEN
        viewModel.register(context)

        // THEN
        // AccountManager
        verify(exactly = 1) { amObserver.onAccountCreateAddressFailed(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountCreateAddressNeeded(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountTwoPassModeFailed(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountDeviceSecretNeeded(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountTwoPassModeNeeded(any(), any()) }
        verify(exactly = 1) { amObserver.onSessionSecondFactorNeeded(any(), any()) }
    }

    @Test
    fun `when register is called userSettingsOrchestrator is registered`() = runTest {
        // GIVEN
        mockkStatic(AccountManager::observe)
        val amObserver = mockAccountManagerObserver()
        every { accountManager.observe(any(), any()) } returns amObserver

        // WHEN
        viewModel.register(context)

        // THEN
        verify { userSettingsOrchestrator.register(context) }
    }

    @Test
    fun `when passwordManagement is called then startPasswordManagementWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)

        // WHEN
        viewModel.submit(LauncherViewModel.Action.OpenPasswordManagement)

        // THEN
        verify { userSettingsOrchestrator.startPasswordManagementWorkflow(userId) }
    }

    @Test
    fun `when change recovery email is called, correct workflow is launched`() = runTest {
        // given
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)

        // when
        viewModel.submit(LauncherViewModel.Action.OpenRecoveryEmail)

        // then
        verify { userSettingsOrchestrator.startUpdateRecoveryEmailWorkflow(userId) }
    }

    @Test
    fun `should request notification permission when action is submitted`() {
        // When
        viewModel.submit(LauncherViewModel.Action.RequestNotificationPermission)

        // Then
        verify { notificationPermissionOrchestrator.requestPermissionIfRequired() }
    }

    private fun buildViewModel() = LauncherViewModel(
        accountManager,
        authOrchestrator,
        notificationPermissionOrchestrator,
        plansOrchestrator,
        reportOrchestrator,
        userSettingsOrchestrator
    )

    private fun mockAccountManagerObserver(): AccountManagerObserver {
        mockkStatic(AccountManagerObserver::onAccountCreateAddressFailed)
        mockkStatic(AccountManagerObserver::onAccountCreateAddressNeeded)
        mockkStatic(AccountManagerObserver::onAccountTwoPassModeFailed)
        mockkStatic(AccountManagerObserver::onAccountTwoPassModeNeeded)
        mockkStatic(AccountManagerObserver::onAccountDeviceSecretNeeded)
        mockkStatic(AccountManagerObserver::onSessionForceLogout)
        mockkStatic(AccountManagerObserver::onSessionSecondFactorNeeded)
        return mockk {
            every { onAccountCreateAddressFailed(any(), any()) } returns this
            every { onAccountCreateAddressNeeded(any(), any()) } returns this
            every { onAccountTwoPassModeFailed(any(), any()) } returns this
            every { onAccountTwoPassModeNeeded(any(), any()) } returns this
            every { onAccountDeviceSecretNeeded(any(), any()) } returns this
            every { onSessionForceLogout(any(), any()) } returns this
            every { onSessionSecondFactorNeeded(any(), any()) } returns this
        }
    }
}
