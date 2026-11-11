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

import app.cash.turbine.test
import ch.protonmail.android.api.AccountApiProvider
import ch.protonmail.android.legacymigration.domain.model.LegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.MigrateLegacyApplication
import ch.protonmail.android.legacymigration.domain.usecase.ObserveLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.SetLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.ShouldMigrateLegacyAccount
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionOrchestrator
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.RefreshFeatureFlags
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.presentation.PaymentOrchestrator
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class LauncherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountApiProvider = mockk<AccountApiProvider>(relaxUnitFun = true)
    private val paymentOrchestrator = mockk<PaymentOrchestrator>()
    private val userSessionRepository = mockk<UserSessionRepository> {
        // A launch with the ready account signed in, unless a test signs in.
        coEvery { getReadyUserIds() } returns setOf(UserIdSample.Primary)
    }
    private val notificationsPermissionOrchestrator = mockk<NotificationsPermissionOrchestrator>(relaxUnitFun = true)
    private val observeLegacyMigrationStatus = mockk<ObserveLegacyMigrationStatus>()
    private val setLegacyMigrationStatus = mockk<SetLegacyMigrationStatus>(relaxUnitFun = true)
    private val migrateLegacyApplication = mockk<MigrateLegacyApplication>(relaxUnitFun = true)
    private val shouldMigrateLegacyAccount = mockk<ShouldMigrateLegacyAccount>()
    private val refreshFeatureFlags = mockk<RefreshFeatureFlags> {
        coEvery { this@mockk(any()) } just runs
    }

    private val isUpsellEnabled = mockk<FeatureFlag<Boolean>> {
        coEvery { this@mockk.get() } returns true
    }

    private val readyAccount = Account(
        userId = UserIdSample.Primary,
        name = "User",
        state = AccountState.Ready,
        primaryAddress = "address"
    )

    private val addedAccount = Account(
        userId = UserId("added"),
        name = "Added",
        state = AccountState.Ready,
        primaryAddress = "added-address"
    )

    private fun viewModel() = LauncherViewModel(
        accountApiProvider,
        paymentOrchestrator,
        userSessionRepository,
        notificationsPermissionOrchestrator,
        observeLegacyMigrationStatus,
        setLegacyMigrationStatus,
        migrateLegacyApplication,
        shouldMigrateLegacyAccount,
        refreshFeatureFlags,
        isUpsellEnabled
    )

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `state should be AccountNeeded when userSession is not available`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns flowOf(emptyList())

            viewModel().state.test {
                assertEquals(LauncherState.AccountNeeded, awaitItem())
            }
        }

    @Test
    fun `state should be PrimaryExist when userSession is available`() = runTest(mainDispatcherRule.testDispatcher) {
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))

        viewModel().state.test {
            assertEquals(LauncherState.PrimaryExist, awaitItem())
        }
    }

    @Test
    fun `does not wait for feature flags when the account is already ready on launch`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))

            viewModel().state.test {
                assertEquals(LauncherState.PrimaryExist, awaitItem())
            }

            coVerify(exactly = 0) { refreshFeatureFlags(any()) }
        }

    @Test
    fun `does not wait for feature flags when the accounts load after an empty first value`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts

            viewModel().state.test {
                assertEquals(LauncherState.Processing, awaitItem())
                accounts.emit(emptyList())
                assertEquals(LauncherState.AccountNeeded, awaitItem())

                accounts.emit(listOf(readyAccount))
                assertEquals(LauncherState.PrimaryExist, awaitItem())
            }

            coVerify(exactly = 0) { refreshFeatureFlags(any()) }
        }

    @Test
    fun `waits for feature flags when signing in again after a sign-out`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)

            viewModel().state.test {
                assertEquals(LauncherState.Processing, awaitItem())
                accounts.emit(listOf(readyAccount))
                assertEquals(LauncherState.PrimaryExist, awaitItem())
                accounts.emit(emptyList())
                assertEquals(LauncherState.AccountNeeded, awaitItem())

                accounts.emit(listOf(readyAccount))
                assertEquals(LauncherState.Processing, awaitItem())
                assertEquals(LauncherState.PrimaryExist, awaitItem())
            }

            coVerify(exactly = 1) { refreshFeatureFlags(readyAccount.userId) }
        }

    @Test
    fun `waits for feature flags before PrimaryExist after a sign-in`() = runTest(mainDispatcherRule.testDispatcher) {
        val accounts = MutableSharedFlow<List<Account>>()
        val refreshDone = CompletableDeferred<Unit>()
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns accounts
        coEvery { userSessionRepository.getReadyUserIds() } returns emptySet()
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)
        coEvery { refreshFeatureFlags(readyAccount.userId) } coAnswers { refreshDone.await() }

        viewModel().state.test {
            assertEquals(LauncherState.Processing, awaitItem())
            accounts.emit(emptyList())
            assertEquals(LauncherState.AccountNeeded, awaitItem())

            accounts.emit(listOf(readyAccount))
            assertEquals(LauncherState.Processing, awaitItem())
            expectNoEvents()

            refreshDone.complete(Unit)
            assertEquals(LauncherState.PrimaryExist, awaitItem())
        }

        coVerify(exactly = 1) { refreshFeatureFlags(readyAccount.userId) }
    }

    @Test
    fun `keeps waiting for feature flags when the same accounts are emitted again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val accounts = MutableSharedFlow<List<Account>>()
            val refreshDone = CompletableDeferred<Unit>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.getReadyUserIds() } returns emptySet()
            every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)
            coEvery { refreshFeatureFlags(readyAccount.userId) } coAnswers { refreshDone.await() }

            viewModel().state.test {
                assertEquals(LauncherState.Processing, awaitItem())
                accounts.emit(listOf(readyAccount))
                accounts.emit(listOf(readyAccount))
                expectNoEvents()

                refreshDone.complete(Unit)
                assertEquals(LauncherState.PrimaryExist, awaitItem())
            }

            coVerify(exactly = 1) { refreshFeatureFlags(readyAccount.userId) }
        }

    @Test
    fun `waits for the flags of an account added without a login step`() = runTest(mainDispatcherRule.testDispatcher) {
        val accounts = MutableSharedFlow<List<Account>>()
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns accounts

        viewModel().state.test {
            assertEquals(LauncherState.Processing, awaitItem())
            accounts.emit(listOf(readyAccount))
            assertEquals(LauncherState.PrimaryExist, awaitItem())
            accounts.emit(listOf(readyAccount, addedAccount.copy(state = AccountState.NotReady)))
            expectNoEvents()

            accounts.emit(listOf(readyAccount, addedAccount))
            assertEquals(LauncherState.Processing, awaitItem())
            assertEquals(LauncherState.PrimaryExist, awaitItem())
        }

        coVerify(exactly = 1) { refreshFeatureFlags(addedAccount.userId) }
        coVerify(exactly = 0) { refreshFeatureFlags(readyAccount.userId) }
    }

    @Test
    fun `waits for the flags of an added account, not the primary one`() = runTest(mainDispatcherRule.testDispatcher) {
        val accounts = MutableSharedFlow<List<Account>>()
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns accounts
        // The added account becomes the primary one only after its wait.
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)

        viewModel().state.test {
            assertEquals(LauncherState.Processing, awaitItem())
            accounts.emit(listOf(readyAccount))
            assertEquals(LauncherState.PrimaryExist, awaitItem())
            accounts.emit(listOf(readyAccount, addedAccount.copy(state = AccountState.TwoPasswordNeeded)))
            assertEquals(LauncherState.StepNeeded, awaitItem())

            accounts.emit(listOf(readyAccount, addedAccount))
            assertEquals(LauncherState.Processing, awaitItem())
            assertEquals(LauncherState.PrimaryExist, awaitItem())
        }

        coVerify(exactly = 1) { refreshFeatureFlags(addedAccount.userId) }
        coVerify(exactly = 0) { refreshFeatureFlags(readyAccount.userId) }
    }

    @Test
    fun `stops waiting for feature flags when the accounts change`() = runTest(mainDispatcherRule.testDispatcher) {
        val accounts = MutableSharedFlow<List<Account>>()
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns accounts
        coEvery { userSessionRepository.getReadyUserIds() } returns emptySet()
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)
        coEvery { refreshFeatureFlags(readyAccount.userId) } coAnswers { awaitCancellation() }

        viewModel().state.test {
            assertEquals(LauncherState.Processing, awaitItem())
            accounts.emit(emptyList())
            assertEquals(LauncherState.AccountNeeded, awaitItem())
            accounts.emit(listOf(readyAccount))
            assertEquals(LauncherState.Processing, awaitItem())

            accounts.emit(emptyList())
            assertEquals(LauncherState.AccountNeeded, awaitItem())
        }
    }

    @Test
    fun `waits for feature flags after the legacy migration`() = runTest(mainDispatcherRule.testDispatcher) {
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.NotDone)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(LauncherState.PrimaryExist, viewModel.state.value)
        coVerify(exactly = 1) { refreshFeatureFlags(readyAccount.userId) }
    }

    @Test
    fun `migration is triggered when status is NotDone`() = runTest(mainDispatcherRule.testDispatcher) {
        val migrationStatusFlow = MutableSharedFlow<LegacyMigrationStatus>()
        every { observeLegacyMigrationStatus() } returns migrationStatusFlow
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(readyAccount.userId)
        coEvery { migrateLegacyApplication() } returns Unit

        viewModel().state.test {
            skipItems(1)
            migrationStatusFlow.emit(LegacyMigrationStatus.NotDone)

            assertEquals(LauncherState.MigrationInProgress, awaitItem())
            assertEquals(LauncherState.ProcessingAfterMigration, awaitItem())
            assertEquals(LauncherState.PrimaryExist, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { migrateLegacyApplication() }
        coVerify(exactly = 1) { setLegacyMigrationStatus(LegacyMigrationStatus.Done) }
    }

    @Test
    fun `should start subscription flow with upselling disabled when FF is off`() = runTest {
        // Given
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))

        coEvery { isUpsellEnabled.get() } returns false
        coEvery { paymentOrchestrator.startSubscriptionWorkflow(false) } just runs

        // When
        viewModel().submit(LauncherViewModel.Action.OpenSubscription)

        // Then
        coVerify(exactly = 1) { paymentOrchestrator.startSubscriptionWorkflow(false) }
        confirmVerified(paymentOrchestrator)
    }

    @Test
    fun `should start subscription flow with upselling enabled when FF is on`() = runTest {
        // Given
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))

        coEvery { isUpsellEnabled.get() } returns true
        coEvery { paymentOrchestrator.startSubscriptionWorkflow(true) } just runs

        // When
        viewModel().submit(LauncherViewModel.Action.OpenSubscription)

        // Then
        coVerify(exactly = 1) { paymentOrchestrator.startSubscriptionWorkflow(true) }
        confirmVerified(paymentOrchestrator)
    }
}
