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
import ch.protonmail.android.mailnotifications.presentation.NotificationPermissionOrchestrator
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.navigation.model.LauncherState.AccountNeeded
import ch.protonmail.android.navigation.model.LauncherState.PrimaryExist
import ch.protonmail.android.navigation.model.LauncherState.Processing
import ch.protonmail.android.navigation.model.LauncherState.StepNeeded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDeviceSecretNeeded
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.report.presentation.ReportOrchestrator
import me.proton.core.usersettings.presentation.UserSettingsOrchestrator
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val authOrchestrator: AuthOrchestrator,
    private val notificationPermissionOrchestrator: NotificationPermissionOrchestrator,
    private val plansOrchestrator: PlansOrchestrator,
    private val reportOrchestrator: ReportOrchestrator,
    private val userSettingsOrchestrator: UserSettingsOrchestrator
) : ViewModel() {

    val state: StateFlow<LauncherState> = accountManager.getAccounts().map { accounts ->
        when {
            accounts.isEmpty() || accounts.all { it.isDisabled() } -> AccountNeeded
            accounts.any { it.isReady() } -> PrimaryExist
            accounts.any { it.isStepNeeded() } -> StepNeeded
            else -> Processing
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = Processing
    )

    fun register(context: AppCompatActivity) {
        authOrchestrator.register(context)
        plansOrchestrator.register(context)
        reportOrchestrator.register(context)
        userSettingsOrchestrator.register(context)
        notificationPermissionOrchestrator.register(context)

        authOrchestrator.onAddAccountResult { result ->
            viewModelScope.launch {
                if (result == null && getPrimaryUserIdOrNull() == null) {
                    context.finish()
                }
            }
        }

        accountManager.observe(context.lifecycle, Lifecycle.State.CREATED)
            .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
            .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
            .onSessionSecondFactorNeeded { authOrchestrator.startSecondFactorWorkflow(it) }
            .onAccountTwoPassModeNeeded { authOrchestrator.startTwoPassModeWorkflow(it) }
            .onAccountCreateAddressNeeded { authOrchestrator.startChooseAddressWorkflow(it) }
            .onAccountDeviceSecretNeeded { authOrchestrator.startDeviceSecretWorkflow(it) }
    }

    fun unregister() {
        authOrchestrator.unregister()
        plansOrchestrator.unregister()
        reportOrchestrator.unregister()
        userSettingsOrchestrator.unregister()
        notificationPermissionOrchestrator.unregister()
    }

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                Action.AddAccount -> onAddAccount()
                Action.OpenPasswordManagement -> onOpenPasswordManagement()
                Action.OpenRecoveryEmail -> onOpenRecoveryEmail()
                Action.OpenSecurityKeys -> onOpenSecurityKeys()
                Action.OpenReport -> onOpenReport()
                Action.OpenSubscription -> onOpenSubscription()
                Action.RequestNotificationPermission -> onRequestNotificationPermission()
                is Action.SignIn -> onSignIn(action.userId)
                is Action.Switch -> onSwitch(action.userId)
            }
        }
    }

    private fun onAddAccount() {
        authOrchestrator.startAddAccountWorkflow()
    }

    private suspend fun onOpenPasswordManagement() {
        getPrimaryUserIdOrNull()?.let {
            userSettingsOrchestrator.startPasswordManagementWorkflow(it)
        }
    }

    private suspend fun onOpenRecoveryEmail() {
        getPrimaryUserIdOrNull()?.let {
            userSettingsOrchestrator.startUpdateRecoveryEmailWorkflow(it)
        }
    }

    private suspend fun onOpenSecurityKeys() {
        getPrimaryUserIdOrNull()?.let {
            userSettingsOrchestrator.startSecurityKeysWorkflow(it)
        }
    }

    private suspend fun onOpenReport() = viewModelScope.launch {
        reportOrchestrator.startBugReport()
    }

    private suspend fun onOpenSubscription() {
        getPrimaryUserIdOrNull()?.let {
            plansOrchestrator.showCurrentPlanWorkflow(it)
        }
    }

    private fun onRequestNotificationPermission() {
        notificationPermissionOrchestrator.requestPermissionIfRequired()
    }

    private suspend fun onSignIn(userId: UserId?) {
        val account = userId?.let { getAccountOrNull(it) }
        authOrchestrator.startLoginWorkflow(account?.username)
    }

    private suspend fun onSwitch(userId: UserId) {
        val account = getAccountOrNull(userId) ?: return
        when {
            account.isDisabled() -> onSignIn(userId)
            account.isReady() -> accountManager.setAsPrimary(userId)
        }
    }

    private suspend fun getAccountOrNull(it: UserId) = accountManager.getAccount(it).firstOrNull()
    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    sealed interface Action {

        data object AddAccount : Action
        data object OpenPasswordManagement : Action
        data object OpenRecoveryEmail : Action
        data object OpenSecurityKeys : Action
        data object OpenReport : Action
        data object OpenSubscription : Action
        data object RequestNotificationPermission : Action
        data class SignIn(val userId: UserId?) : Action
        data class Switch(val userId: UserId) : Action
    }
}
