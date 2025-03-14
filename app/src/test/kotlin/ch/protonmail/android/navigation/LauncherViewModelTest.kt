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
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult.CHECKING
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult.DENIED
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult.GRANTED
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult.SHOW_RATIONALE
import ch.protonmail.android.mailnotifications.presentation.NewNotificationPermissionOrchestrator
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
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherViewModelTest {

    private val authOrchestrator = mockk<AuthOrchestrator>(relaxUnitFun = true)
    private val plansOrchestrator = mockk<PlansOrchestrator>(relaxUnitFun = true)
    private val reportOrchestrator = mockk<ReportOrchestrator>(relaxUnitFun = true)
    private val userSettingsOrchestrator = mockk<UserSettingsOrchestrator>(relaxUnitFun = true)
    private val notificationsPermissionsOrchestrator =
        mockk<NotificationsPermissionsOrchestrator>(relaxUnitFun = true) {
            every { permissionResult() } returns MutableStateFlow(GRANTED)
        }
    private val newNotificationPermissionOrchestrator = mockk<NewNotificationPermissionOrchestrator>(
        relaxUnitFun = true
    )

    private val accountListFlow = MutableStateFlow<List<Account>>(emptyList())
    private val accountManager = mockk<AccountManager>(relaxUnitFun = true) {
        every { getAccounts() } returns accountListFlow
    }

    private val context = mockk<AppCompatActivity> {
        every { lifecycle } returns mockk()
    }

    private val isNewNotificationPermissionFlowEnabled = mockk<Provider<Boolean>> {
        every { this@mockk.get() } returns false
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
    fun `when the accounts are ready, should check if the permission needs to be requested`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.readyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(CHECKING)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when there are no accounts, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(emptyList())
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(CHECKING)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the accounts are disabled, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.disabledAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(CHECKING)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the accounts are missing steps, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.notReadyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(CHECKING)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the permission is already granted, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.readyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(GRANTED)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the permission is already denied, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.readyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(DENIED)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the permission is in rationale state, should not check for notifications permissions`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.readyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(SHOW_RATIONALE)

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `when the FF is ON, should not check for notifications permission`() = runTest {
        // given
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountTestData.readyAccount))
        every { notificationsPermissionsOrchestrator.permissionResult() } returns flowOf(CHECKING)
        every { isNewNotificationPermissionFlowEnabled.get() } returns true

        // when
        val viewModel = buildViewModel()
        viewModel.state.test { awaitItem() }

        // then
        verify(exactly = 0) { notificationsPermissionsOrchestrator.requestPermissionIfRequired() }
    }

    @Test
    fun `should request notification permission when action is submitted`() {
        // When
        viewModel.submit(LauncherViewModel.Action.RequestNotificationPermission)

        // Then
        verify { newNotificationPermissionOrchestrator.requestPermissionIfRequired() }
    }

    private fun buildViewModel() = LauncherViewModel(
        accountManager,
        authOrchestrator,
        isNewNotificationPermissionFlowEnabled,
        newNotificationPermissionOrchestrator,
        plansOrchestrator,
        reportOrchestrator,
        userSettingsOrchestrator,
        notificationsPermissionsOrchestrator
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
