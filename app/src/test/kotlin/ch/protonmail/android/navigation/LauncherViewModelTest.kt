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

import androidx.lifecycle.Lifecycle
import app.cash.turbine.test
import ch.protonmail.android.legacymigration.domain.model.LegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.MigrateLegacyApplication
import ch.protonmail.android.legacymigration.domain.usecase.ObserveLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.SetLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.ShouldMigrateLegacyAccount
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionOrchestrator
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.SetPrimaryAccount
import ch.protonmail.android.mailsession.presentation.observe
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.core.auth.presentation.AuthOrchestrator
import me.proton.android.core.auth.presentation.login.LoginOutput
import me.proton.android.core.auth.presentation.onLoginResult
import me.proton.android.core.payment.presentation.PaymentOrchestrator
import org.junit.Assert
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class LauncherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authOrchestrator = mockk<AuthOrchestrator>()
    private val paymentOrchestrator = mockk<PaymentOrchestrator>()
    private val setPrimaryAccount = mockk<SetPrimaryAccount>(relaxUnitFun = true)
    private val userSessionRepository = mockk<UserSessionRepository>()
    private val notificationsPermissionOrchestrator = mockk<NotificationsPermissionOrchestrator>(relaxUnitFun = true)
    private val observeLegacyMigrationStatus = mockk<ObserveLegacyMigrationStatus>()
    private val setLegacyMigrationStatus = mockk<SetLegacyMigrationStatus>(relaxUnitFun = true)
    private val migrateLegacyApplication = mockk<MigrateLegacyApplication>(relaxUnitFun = true)
    private val shouldMigrateLegacyAccount = mockk<ShouldMigrateLegacyAccount>()

    private val isUpsellEnabled = mockk<FeatureFlag<Boolean>> {
        coEvery { this@mockk.get() } returns true
    }

    private val readyAccount = Account(
        userId = UserIdSample.Primary,
        name = "User",
        state = AccountState.Ready,
        primaryAddress = "address"
    )

    private fun viewModel() = LauncherViewModel(
        authOrchestrator,
        paymentOrchestrator,
        setPrimaryAccount,
        userSessionRepository,
        notificationsPermissionOrchestrator,
        observeLegacyMigrationStatus,
        setLegacyMigrationStatus,
        migrateLegacyApplication,
        shouldMigrateLegacyAccount,
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
    fun `migration is triggered when status is NotDone`() = runTest(mainDispatcherRule.testDispatcher) {
        val migrationStatusFlow = MutableSharedFlow<LegacyMigrationStatus>()
        every { observeLegacyMigrationStatus() } returns migrationStatusFlow
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
        coEvery { migrateLegacyApplication() } returns Unit

        viewModel().state.test {
            skipItems(1)
            migrationStatusFlow.emit(LegacyMigrationStatus.NotDone)

            assertEquals(LauncherState.MigrationInProgress, awaitItem())
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

    @Test
    fun `if onLoginResult is DuplicateAccount then emit duplicateDialogErrorEffect`() = runTest {
        // Given
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
        coEvery { shouldMigrateLegacyAccount.invoke() } returns false
        every { paymentOrchestrator.register(any()) } just runs
        every { paymentOrchestrator.setOnUpgradeResult(any()) } just runs
        mockkStatic(userSessionRepository::observe)
        every { userSessionRepository.observe(any(), Lifecycle.State.RESUMED) } returns mockk(relaxed = true)

        val blockSlot = slot<(result: LoginOutput?) -> Unit>()
        every { authOrchestrator.setOnSignUpResult(any()) } just runs
        every { authOrchestrator.register(any()) } just runs
        every { authOrchestrator.setOnAddAccountResult(any()) } just runs
        mockkStatic(authOrchestrator::onLoginResult)
        every { authOrchestrator.onLoginResult(capture(blockSlot)) } returns mockk()
        val sut = viewModel()
        // when

        sut.register(mockk(relaxed = true))
        val callback = blockSlot.captured
        callback(LoginOutput.DuplicateAccount(UserIdTestData.userId.id))

        // then
        Assert.assertEquals(sut.duplicateDialogErrorEffect.first(), Effect.of(Unit))
    }

    @Test
    fun `if onLoginResult is LoggedIn then setPrimaryAccount`() = runTest {
        // Given
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
        coEvery { shouldMigrateLegacyAccount.invoke() } returns false
        every { paymentOrchestrator.register(any()) } just runs
        every { paymentOrchestrator.setOnUpgradeResult(any()) } just runs
        mockkStatic(userSessionRepository::observe)
        every { userSessionRepository.observe(any(), Lifecycle.State.RESUMED) } returns mockk(relaxed = true)

        val blockSlot = slot<(result: LoginOutput?) -> Unit>()
        every { authOrchestrator.setOnSignUpResult(any()) } just runs
        every { authOrchestrator.register(any()) } just runs
        every { authOrchestrator.setOnAddAccountResult(any()) } just runs
        mockkStatic(authOrchestrator::onLoginResult)
        every { authOrchestrator.onLoginResult(capture(blockSlot)) } returns mockk()
        val sut = viewModel()
        // when

        sut.register(mockk(relaxed = true))
        val callback = blockSlot.captured
        callback(LoginOutput.LoggedIn(UserIdTestData.userId.id))

        // then
        coVerify { setPrimaryAccount(UserIdTestData.userId) }
    }
}
