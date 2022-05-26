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

package ch.protonmail.android.navigation

import androidx.appcompat.app.AppCompatActivity
import app.cash.turbine.test
import ch.protonmail.android.testdata.AccountTestData
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.AccountType.Internal
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.AccountManagerObserver
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionForceLogout
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.MissingScopeObserver
import me.proton.core.auth.presentation.observe
import me.proton.core.auth.presentation.onConfirmPasswordNeeded
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.Product.Mail
import me.proton.core.domain.entity.UserId
import me.proton.core.humanverification.domain.HumanVerificationManager
import me.proton.core.humanverification.presentation.HumanVerificationManagerObserver
import me.proton.core.humanverification.presentation.HumanVerificationOrchestrator
import me.proton.core.humanverification.presentation.observe
import me.proton.core.humanverification.presentation.onHumanVerificationNeeded
import me.proton.core.network.domain.scopes.MissingScopeListener
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.report.presentation.ReportOrchestrator
import me.proton.core.user.domain.UserManager
import me.proton.core.usersettings.presentation.UserSettingsOrchestrator
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LauncherViewModelTest {

    private val authOrchestrator = mockk<AuthOrchestrator>(relaxUnitFun = true)
    private val hvOrchestrator = mockk<HumanVerificationOrchestrator>(relaxUnitFun = true)
    private val plansOrchestrator = mockk<PlansOrchestrator>(relaxUnitFun = true)
    private val reportOrchestrator = mockk<ReportOrchestrator>(relaxUnitFun = true)
    private val userSettingsOrchestrator = mockk<UserSettingsOrchestrator>(relaxUnitFun = true)
    private val missingScopeListener = mockk<MissingScopeListener>(relaxUnitFun = true)

    private val userManager = mockk<UserManager>()
    private val humanVerificationManager = mockk<HumanVerificationManager>()

    private val accountListFlow = MutableStateFlow<List<Account>>(emptyList())
    private val accountManager = mockk<AccountManager> {
        every { getAccounts() } returns accountListFlow
    }

    private val context = mockk<AppCompatActivity> {
        every { lifecycle } returns mockk()
    }

    private val user1UserId = UserId("test")
    private val user1Username = "username"

    private lateinit var viewModel: LauncherViewModel

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = LauncherViewModel(
            Mail,
            Internal,
            accountManager,
            userManager,
            humanVerificationManager,
            authOrchestrator,
            hvOrchestrator,
            plansOrchestrator,
            reportOrchestrator,
            userSettingsOrchestrator,
            missingScopeListener
        )
    }

    @Test
    fun `when no account then AccountNeeded`() = runTest {
        // GIVEN
        accountListFlow.emit(emptyList())
        // WHEN
        viewModel.state.test {
            // THEN
            assertEquals(LauncherViewModel.State.AccountNeeded, awaitItem())
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
            assertEquals(LauncherViewModel.State.AccountNeeded, awaitItem())
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
            assertEquals(LauncherViewModel.State.PrimaryExist, awaitItem())
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
            assertEquals(LauncherViewModel.State.AccountNeeded, awaitItem())

            accountListFlow.emit(listOf(AccountTestData.notReadyAccount))
            assertEquals(LauncherViewModel.State.StepNeeded, awaitItem())

            accountListFlow.emit(listOf(AccountTestData.readyAccount))
            assertEquals(LauncherViewModel.State.PrimaryExist, awaitItem())

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
            assertEquals(LauncherViewModel.State.PrimaryExist, awaitItem())

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
        viewModel.addAccount()
        // THEN
        verify { authOrchestrator.startAddAccountWorkflow(AccountType.Internal, Product.Mail) }
    }

    @Test
    fun `when signIn is called, startLoginWorkflow`() = runTest {
        // WHEN
        viewModel.signIn()
        // THEN
        verify { authOrchestrator.startLoginWorkflow(AccountType.Internal, any(), any()) }
    }

    @Test
    fun `when signIn with userId is called, startLoginWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getAccount(user1UserId) } returns flowOf(AccountTestData.readyAccount)
        // WHEN
        viewModel.signIn(user1UserId)
        // THEN
        verify { authOrchestrator.startLoginWorkflow(AccountType.Internal, user1Username) }
    }

    @Test
    fun `when signOut is called, disableAccount`() = runTest {
        // GIVEN
        every { accountManager.getPrimaryUserId() } returns flowOf(user1UserId)
        // WHEN
        viewModel.signOut()
        // THEN
        coVerify { accountManager.disableAccount(user1UserId) }
        verify(exactly = 1) { accountManager.getPrimaryUserId() }
    }

    @Test
    fun `when signOut with userId is called, disableAccount`() = runTest {
        // WHEN
        viewModel.signOut(user1UserId)
        // THEN
        coVerify { accountManager.disableAccount(user1UserId) }
        verify(exactly = 0) { accountManager.getPrimaryUserId() }
    }

    @Test
    fun `when switch is called on disabled account, startLoginWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getAccount(user1UserId) } returns flowOf(AccountTestData.disabledAccount)
        // WHEN
        viewModel.switch(user1UserId)
        // THEN
        verify { authOrchestrator.startLoginWorkflow(AccountType.Internal, user1Username) }
    }

    @Test
    fun `when switch is called on ready account, setPrimary`() = runTest {
        // GIVEN
        every { accountManager.getAccount(user1UserId) } returns flowOf(AccountTestData.readyAccount)
        // WHEN
        viewModel.switch(user1UserId)
        // THEN
        coVerify { accountManager.setAsPrimary(user1UserId) }
    }

    @Test
    fun `when remove is called, removeAccount`() = runTest {
        // WHEN
        viewModel.remove(user1UserId)
        // THEN
        coVerify { accountManager.removeAccount(user1UserId) }
    }

    @Test
    fun `when register is called, verify AccountManagerObserver subscriptions`() = runTest {
        // GIVEN

        // AccountManager
        mockkStatic(AccountManager::observe)
        val amObserver = mockAccountManagerObserver()
        every { accountManager.observe(any(), any()) } returns amObserver

        // HumanVerificationManager
        mockkStatic(HumanVerificationManager::observe)
        val hvObserver = mockHumanVerificationManagerObserver()
        every { humanVerificationManager.observe(any(), any()) } returns hvObserver

        // MissingScopeListener
        mockkStatic(MissingScopeListener::observe)
        val missingScopeObserver = mockMissingScopeObserver()
        every { missingScopeListener.observe(any(), any()) } returns missingScopeObserver

        // WHEN
        viewModel.register(context)

        // THEN
        // AccountManager
        verify(exactly = 1) { amObserver.onAccountCreateAddressFailed(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountCreateAddressNeeded(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountTwoPassModeFailed(any(), any()) }
        verify(exactly = 1) { amObserver.onAccountTwoPassModeNeeded(any(), any()) }
        verify(exactly = 1) { amObserver.onSessionForceLogout(any(), any()) }
        verify(exactly = 1) { amObserver.onSessionSecondFactorNeeded(any(), any()) }
        // HumanVerificationManager
        verify(exactly = 1) { hvObserver.onHumanVerificationNeeded(any(), any()) }
        // MissingScopeListener
        verify(exactly = 1) { missingScopeObserver.onConfirmPasswordNeeded(any()) }
    }

    @Test
    fun `when register is called userSettingsOrchestrator is registered`() = runTest {
        // GIVEN
        mockkStatic(AccountManager::observe)
        val amObserver = mockAccountManagerObserver()
        every { accountManager.observe(any(), any()) } returns amObserver

        mockkStatic(HumanVerificationManager::observe)
        val hvObserver = mockHumanVerificationManagerObserver()
        every { humanVerificationManager.observe(any(), any()) } returns hvObserver

        mockkStatic(MissingScopeListener::observe)
        val missingScopeObserver = mockMissingScopeObserver()
        every { missingScopeListener.observe(any(), any()) } returns missingScopeObserver

        // WHEN
        viewModel.register(context)

        // THEN
        verify { userSettingsOrchestrator.register(context) }
    }

    @Test
    fun `when passwordManagement is called then startPasswordManagementWorkflow`() = runTest {
        // GIVEN
        every { accountManager.getPrimaryUserId() } returns flowOf(user1UserId)

        // WHEN
        viewModel.passwordManagement()

        // THEN
        verify { userSettingsOrchestrator.startPasswordManagementWorkflow(user1UserId) }
    }

    private fun mockMissingScopeObserver(): MissingScopeObserver {
        mockkStatic(MissingScopeObserver::onConfirmPasswordNeeded)
        val missingScopeObserver = mockk<MissingScopeObserver> {
            every { onConfirmPasswordNeeded(any()) } returns this
        }
        return missingScopeObserver
    }

    private fun mockHumanVerificationManagerObserver(): HumanVerificationManagerObserver {
        mockkStatic(HumanVerificationManagerObserver::onHumanVerificationNeeded)
        val hvObserver = mockk<HumanVerificationManagerObserver> {
            every { onHumanVerificationNeeded(any(), any()) } returns this
        }
        return hvObserver
    }

    private fun mockAccountManagerObserver(): AccountManagerObserver {
        mockkStatic(AccountManagerObserver::onAccountCreateAddressFailed)
        mockkStatic(AccountManagerObserver::onAccountCreateAddressNeeded)
        mockkStatic(AccountManagerObserver::onAccountTwoPassModeFailed)
        mockkStatic(AccountManagerObserver::onAccountTwoPassModeNeeded)
        mockkStatic(AccountManagerObserver::onSessionForceLogout)
        mockkStatic(AccountManagerObserver::onSessionSecondFactorNeeded)
        return mockk {
            every { onAccountCreateAddressFailed(any(), any()) } returns this
            every { onAccountCreateAddressNeeded(any(), any()) } returns this
            every { onAccountTwoPassModeFailed(any(), any()) } returns this
            every { onAccountTwoPassModeNeeded(any(), any()) } returns this
            every { onSessionForceLogout(any(), any()) } returns this
            every { onSessionSecondFactorNeeded(any(), any()) } returns this
        }
    }
}
