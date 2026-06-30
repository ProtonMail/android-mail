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
import ch.protonmail.android.legacymigration.domain.model.LegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.MigrateLegacyApplication
import ch.protonmail.android.legacymigration.domain.usecase.ObserveLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.SetLegacyMigrationStatus
import ch.protonmail.android.legacymigration.domain.usecase.ShouldMigrateLegacyAccount
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionOrchestrator
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.android.account.api.ProtonAccountApi
import me.proton.android.account.types.ProtonUserId
import me.proton.android.core.payment.presentation.PaymentOrchestrator
import me.proton.android.core.payment.presentation.onUpgradeResult
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("NotImplementedDeclaration", "UnusedPrivateMember")
class LauncherViewModel @Inject constructor(
    private val accountApi: ProtonAccountApi,
    private val paymentOrchestrator: PaymentOrchestrator,
    private val userSessionRepository: UserSessionRepository,
    private val notificationsPermissionOrchestrator: NotificationsPermissionOrchestrator,
    private val observeLegacyMigrationStatus: ObserveLegacyMigrationStatus,
    private val setLegacyMigrationStatus: SetLegacyMigrationStatus,
    private val migrateLegacyApplication: MigrateLegacyApplication,
    private val shouldMigrateLegacyAccount: ShouldMigrateLegacyAccount,
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

    private fun observeStoredAccounts(afterMigration: Boolean = false) {
        userSessionRepository.observeAccounts()
            .mapLatest { accounts ->
                when {
                    accounts.isEmpty() || accounts.all { it.state == AccountState.Disabled } ->
                        AccountNeeded

                    accounts.any { it.state == AccountState.TwoPasswordNeeded } ||
                        accounts.any { it.state == AccountState.TwoFactorNeeded } ||
                        accounts.any { it.state == AccountState.NewPassNeeded } ->
                        StepNeeded

                    accounts.any { it.state == AccountState.Ready } ->
                        PrimaryExist

                    else -> {
                        if (afterMigration) {
                            ProcessingAfterMigration
                        } else {
                            Processing
                        }
                    }
                }
            }
            .onEach { mutableState.value = it }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        notificationsPermissionOrchestrator.unregister()
        paymentOrchestrator.unregister()
        super.onCleared()
    }

    fun register(context: AppCompatActivity) {
        viewModelScope.launch {
            if (shouldMigrateLegacyAccount()) {
                // Wait for the legacy migration to complete before registering observers.
                observeLegacyMigrationStatus().first { it == LegacyMigrationStatus.Done }

                if (context.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    Timber.d("Legacy migration: Activity is still alive. Registering user session observers.")
                } else {
                    Timber.w("Legacy migration: Activity no longer alive. Skipping registration.")
                }
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
        accountApi.launchAddAccount()
    }

    private fun onOpenPasswordManagement(userId: UserId?) {
        TODO("accountApi.launchSettings()")
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
        TODO("accountApi.launchSettings()")
    }

    private fun onSignIn(userId: UserId?) = viewModelScope.launch {
        val address = userId?.let {
            userSessionRepository.getAccount(it)?.primaryAddress
        }
        accountApi.launchSignIn(/*address*/)
    }

    private fun onSignUp() = viewModelScope.launch {
        accountApi.launchSignUp()
    }

    private fun onSwitchToAccount(userId: UserId) = viewModelScope.launch {
        accountApi.setCurrentAccount(ProtonUserId(userId.id))
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
