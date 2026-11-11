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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.AccountApiProvider
import ch.protonmail.android.legacymigration.domain.model.LegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.MigrateLegacyApplication
import ch.protonmail.android.legacymigration.domain.usecase.ObserveLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.SetLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.ShouldMigrateLegacyAccount
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionOrchestrator
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.RefreshFeatureFlags
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.navigation.model.LauncherState.AccountNeeded
import ch.protonmail.android.navigation.model.LauncherState.MigrationInProgress
import ch.protonmail.android.navigation.model.LauncherState.PrimaryExist
import ch.protonmail.android.navigation.model.LauncherState.Processing
import ch.protonmail.android.navigation.model.LauncherState.ProcessingAfterMigration
import ch.protonmail.android.navigation.model.LauncherState.StepNeeded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.proton.android.core.payment.presentation.PaymentOrchestrator
import me.proton.android.core.payment.presentation.onUpgradeResult
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("NotImplementedDeclaration", "UnusedPrivateMember")
class LauncherViewModel @Inject constructor(
    private val accountApiProvider: AccountApiProvider,
    private val paymentOrchestrator: PaymentOrchestrator,
    private val userSessionRepository: UserSessionRepository,
    private val notificationsPermissionOrchestrator: NotificationsPermissionOrchestrator,
    private val observeLegacyMigrationStatus: ObserveLegacyMigrationStatus,
    private val setLegacyMigrationStatus: SetLegacyMigrationStatus,
    private val migrateLegacyApplication: MigrateLegacyApplication,
    private val shouldMigrateLegacyAccount: ShouldMigrateLegacyAccount,
    private val refreshFeatureFlags: RefreshFeatureFlags,
    @IsUpsellEnabled private val isUpsellEnabled: FeatureFlag<Boolean>
) : ViewModel() {

    private val _duplicateDialogErrorEffect = MutableStateFlow<Effect<Unit>>(Effect.empty())
    val duplicateDialogErrorEffect = _duplicateDialogErrorEffect.asStateFlow()

    private val mutableState = MutableStateFlow(Processing)
    val state: StateFlow<LauncherState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            when (observeLegacyMigrationStatus().first()) {
                LegacyMigrationStatus.NotDone -> {
                    mutableState.value = MigrationInProgress

                    migrateLegacyApplication()

                    setLegacyMigrationStatus(LegacyMigrationStatus.Done)
                    observeStoredAccounts(afterMigration = true)
                }

                LegacyMigrationStatus.Done -> {
                    observeStoredAccounts()
                }
            }
        }
    }

    private suspend fun observeStoredAccounts(afterMigration: Boolean = false) {
        // A cold start already has the flags of the last session, so only a sign-in, sign-up or migration waits.
        // The accounts ready at launch are read from the store: the accounts flow can start with an empty list while
        // the stored accounts load. After that, an account that becomes ready, or leaving PrimaryExist (a sign-out or
        // a login step), arms the wait again.
        val storedReadyUserIds = userSessionRepository.getReadyUserIds()
        var waitForFeatureFlags = afterMigration || storedReadyUserIds.isEmpty()
        var previousState: LauncherState? = null
        var previousReadyUserIds = storedReadyUserIds
        userSessionRepository.observeAccounts()
            // A repeated value would cancel and restart a running wait.
            .distinctUntilChanged()
            .mapLatest { accounts ->
                val state = accounts.toLauncherState(afterMigration)
                val readyUserIds = accounts.filter { it.state == AccountState.Ready }.map { it.userId }.toSet()
                val signedInUserId = (readyUserIds - previousReadyUserIds).firstOrNull()
                // An empty list is the store loading, or every account removed, so the known accounts stay.
                if (accounts.isNotEmpty()) previousReadyUserIds = readyUserIds
                // An account added while another one is ready leaves the launcher in PrimaryExist.
                if (signedInUserId != null) waitForFeatureFlags = true
                if (previousState == PrimaryExist && state != PrimaryExist) waitForFeatureFlags = true
                previousState = state
                if (state == PrimaryExist && waitForFeatureFlags) {
                    mutableState.value = if (afterMigration) ProcessingAfterMigration else Processing
                    // The account that just became ready is the one signing in. An added account becomes the
                    // primary one only seconds later, so the primary account is only the fallback.
                    (signedInUserId ?: userSessionRepository.observePrimaryUserId().first())
                        ?.let { refreshFeatureFlags(it) }
                    waitForFeatureFlags = false
                }
                state
            }
            .collect { mutableState.value = it }
    }

    private fun List<Account>.toLauncherState(afterMigration: Boolean): LauncherState = when {
        isEmpty() || all { it.state == AccountState.Disabled } -> AccountNeeded

        any { it.state == AccountState.TwoPasswordNeeded } ||
            any { it.state == AccountState.TwoFactorNeeded } ||
            any { it.state == AccountState.NewPassNeeded } -> StepNeeded

        any { it.state == AccountState.Ready } -> PrimaryExist

        afterMigration -> ProcessingAfterMigration

        else -> Processing
    }

    override fun onCleared() {
        accountApiProvider.unregister()
        notificationsPermissionOrchestrator.unregister()
        paymentOrchestrator.unregister()
        super.onCleared()
    }

    fun register(context: AppCompatActivity) {
        // Activity result launchers must be registered before the Activity is STARTED, so this cannot be deferred.
        accountApiProvider.register(context)

        viewModelScope.launch {
            if (shouldMigrateLegacyAccount()) {
                // Wait for the legacy migration to complete before registering observers.
                observeLegacyMigrationStatus().first { it == LegacyMigrationStatus.Done }

                if (context.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    Timber.d("Legacy migration: Activity is still alive. Registering user session observers.")
                    accountApiProvider.registerUserSessionObservers(context)
                } else {
                    Timber.w("Legacy migration: Activity no longer alive. Skipping registration.")
                }
            } else {
                accountApiProvider.registerUserSessionObservers(context)
            }
        }

        notificationsPermissionOrchestrator.register(context)
        with(paymentOrchestrator) {
            register(context)
            onUpgradeResult { }
        }
    }

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                is Action.AddAccount -> onAddAccount()
                is Action.OpenPasswordManagement -> onOpenPasswordManagement(action.userId)
                is Action.OpenRecoveryEmail -> onOpenRecoveryEmail()
                is Action.OpenReport -> onOpenReport()
                is Action.OpenSecurityKeys -> onOpenSecurityKeys()
                is Action.OpenSubscription -> onOpenSubscription()
                is Action.RequestNotificationPermission -> onRequestNotificationPermission()
                is Action.SignIn -> onSignIn(action.userId)
                is Action.SignUp -> onSignUp()
                is Action.SwitchToAccount -> onSwitchToAccount(action.userId)
            }
        }
    }

    private fun onAddAccount() {
        accountApiProvider.startAddAccount()
    }

    private fun onOpenPasswordManagement(userId: UserId?) {
        accountApiProvider.startSettings(requireNotNull(userId))
    }

    private fun onOpenRecoveryEmail() {
        TODO("ET - Not yet implemented")
    }

    private fun onOpenReport() {
        TODO("ET - Not yet implemented")
    }

    private fun onOpenSubscription() = viewModelScope.launch {
        paymentOrchestrator.startSubscriptionWorkflow(isUpsellEnabled.get())
    }

    private fun onOpenSecurityKeys() {
        accountApiProvider.startSecurityKeys()
    }

    private fun onSignIn(userId: UserId?) = viewModelScope.launch {
        val address = userId?.let {
            userSessionRepository.getAccount(it)?.primaryAddress
        }
        accountApiProvider.startSignIn(address)
    }

    private fun onSignUp() = viewModelScope.launch {
        accountApiProvider.startSignUp()
    }

    private fun onSwitchToAccount(userId: UserId) = viewModelScope.launch {
        accountApiProvider.switchAccount(userId)
    }

    private fun onDuplicateAccountError() = _duplicateDialogErrorEffect.tryEmit(Effect.of(Unit))

    private fun onRequestNotificationPermission() {
        notificationsPermissionOrchestrator.requestPermissionIfRequired()
    }

    sealed interface Action {

        data object AddAccount : Action
        data class OpenPasswordManagement(val userId: UserId?) : Action
        data object OpenRecoveryEmail : Action
        data object OpenReport : Action
        data object OpenSecurityKeys : Action
        data object OpenSubscription : Action
        data object RequestNotificationPermission : Action
        data class SignIn(val userId: UserId?) : Action
        data object SignUp : Action
        data class SwitchToAccount(val userId: UserId) : Action
    }
}
