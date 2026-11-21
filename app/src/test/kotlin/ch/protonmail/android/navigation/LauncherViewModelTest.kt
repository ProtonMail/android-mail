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

import android.content.Intent
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
import ch.protonmail.android.navigation.share.NewIntentObserver
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.presentation.PaymentOrchestrator
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.assertNull
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

    private val newIntentObserver = NewIntentObserver()

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
        isUpsellEnabled,
        newIntentObserver
    )

    private fun linkIntent(link: String) = mockk<Intent> {
        every { action } returns Intent.ACTION_VIEW
        every { dataString } returns link
    }

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

    @Test
    fun `closes the app after opening a connect Gmail web link that started it signed out`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.isSignedOut() } returns true
            val viewModel = viewModel()

            // When
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            advanceUntilIdle()
            accounts.emit(emptyList())
            advanceUntilIdle()

            // Then
            assertEquals(
                LauncherViewModel.OpenInBrowser(WebLink, closeApp = true),
                viewModel.openInBrowserEffect.value.consume()
            )
            // The welcome screen would start the sign-in on top of the browser.
            assertEquals(LauncherState.Processing, viewModel.state.value)
        }

    @Test
    fun `leaves a connect Gmail web link that started the app to Home when signed in`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.isSignedOut() } returns false
            val viewModel = viewModel()
            val intent = linkIntent(WebLink)

            // When
            newIntentObserver.onNewIntent(intent)
            advanceUntilIdle()
            // The accounts flow can start with an empty list while the stored accounts load.
            accounts.emit(emptyList())
            accounts.emit(listOf(readyAccount))
            advanceUntilIdle()

            // Then
            assertNull(viewModel.openInBrowserEffect.value.consume())
            assertEquals(LauncherState.PrimaryExist, viewModel.state.value)
            newIntentObserver().test { assertEquals(intent, awaitItem()) }
        }

    @Test
    fun `shows the launcher once the connect Gmail web link that started the app is left to Home`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val isSignedOut = CompletableDeferred<Boolean>()
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.isSignedOut() } coAnswers { isSignedOut.await() }
            val viewModel = viewModel()
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            runCurrent()
            accounts.emit(listOf(readyAccount))
            runCurrent()
            assertEquals(LauncherState.Processing, viewModel.state.value)

            // When
            isSignedOut.complete(false)
            runCurrent()

            // Then
            assertEquals(LauncherState.PrimaryExist, viewModel.state.value)
        }

    @Test
    fun `opens a connect Gmail web link in the browser over the welcome screen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns flowOf(emptyList())
            coEvery { userSessionRepository.isSignedOut() } returns true
            val viewModel = viewModel()
            advanceUntilIdle()

            // When
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            advanceUntilIdle()

            // Then
            assertEquals(
                LauncherViewModel.OpenInBrowser(WebLink, closeApp = false),
                viewModel.openInBrowserEffect.value.consume()
            )
            assertEquals(LauncherState.AccountNeeded, viewModel.state.value)
        }

    @Test
    fun `drops the connect Gmail web link it opened in the browser, so a sign-in does not open it again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns flowOf(emptyList())
            coEvery { userSessionRepository.isSignedOut() } returns true
            viewModel()
            advanceUntilIdle()

            // When
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            advanceUntilIdle()

            // Then
            newIntentObserver().test { expectNoEvents() }
        }

    @Test
    fun `shows the welcome screen when no browser can open the link that started the app`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.isSignedOut() } returns true
            val viewModel = viewModel()
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            advanceUntilIdle()
            accounts.emit(emptyList())
            advanceUntilIdle()
            assertEquals(LauncherState.Processing, viewModel.state.value)

            // When
            viewModel.submit(LauncherViewModel.Action.BrowserUnavailable)
            advanceUntilIdle()

            // Then
            assertEquals(LauncherState.AccountNeeded, viewModel.state.value)
        }

    @Test
    fun `a newer intent cancels the check of a connect Gmail web link`() = runTest(mainDispatcherRule.testDispatcher) {
        // Given
        val accounts = MutableSharedFlow<List<Account>>()
        every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
        every { userSessionRepository.observeAccounts() } returns accounts
        coEvery { userSessionRepository.isSignedOut() } coAnswers { awaitCancellation() }
        val viewModel = viewModel()
        newIntentObserver.onNewIntent(linkIntent(WebLink))
        runCurrent()
        accounts.emit(emptyList())
        runCurrent()
        assertEquals(LauncherState.Processing, viewModel.state.value)

        // When
        newIntentObserver.onNewIntent(mockk { every { action } returns Intent.ACTION_SEND })
        runCurrent()

        // Then
        assertNull(viewModel.openInBrowserEffect.value.consume())
        assertEquals(LauncherState.AccountNeeded, viewModel.state.value)
    }

    @Test
    fun `leaves the connect Gmail web link to Home when the accounts cannot be checked in time`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            val accounts = MutableSharedFlow<List<Account>>()
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns accounts
            coEvery { userSessionRepository.isSignedOut() } coAnswers { awaitCancellation() }
            val viewModel = viewModel()
            newIntentObserver.onNewIntent(linkIntent(WebLink))
            advanceUntilIdle()

            // When
            accounts.emit(emptyList())
            advanceUntilIdle()

            // Then
            assertNull(viewModel.openInBrowserEffect.value.consume())
            assertEquals(LauncherState.AccountNeeded, viewModel.state.value)
        }

    @Test
    fun `does not check the accounts for a launch without a connect Gmail web link`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Given
            every { observeLegacyMigrationStatus() } returns flowOf(LegacyMigrationStatus.Done)
            every { userSessionRepository.observeAccounts() } returns flowOf(listOf(readyAccount))
            val viewModel = viewModel()

            // When
            newIntentObserver.onNewIntent(mockk { every { action } returns Intent.ACTION_MAIN })
            newIntentObserver.onNewIntent(linkIntent("protonmail://connect-gmail/start"))
            advanceUntilIdle()

            // Then
            assertEquals(LauncherState.PrimaryExist, viewModel.state.value)
            assertNull(viewModel.openInBrowserEffect.value.consume())
            coVerify(exactly = 0) { userSessionRepository.isSignedOut() }
        }

    private companion object {

        const val WebLink = "https://account.proton.me/mail/easy-switch"
    }
}
