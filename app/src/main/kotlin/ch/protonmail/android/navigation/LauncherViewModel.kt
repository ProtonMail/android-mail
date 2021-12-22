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

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.navigation.LauncherViewModel.State.AccountNeeded
import ch.protonmail.android.navigation.LauncherViewModel.State.PrimaryExist
import ch.protonmail.android.navigation.LauncherViewModel.State.Processing
import ch.protonmail.android.navigation.LauncherViewModel.State.StepNeeded
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.account.domain.entity.isStepNeeded
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionForceLogout
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onAddAccountResult
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.UserId
import me.proton.core.humanverification.domain.HumanVerificationManager
import me.proton.core.humanverification.presentation.HumanVerificationOrchestrator
import me.proton.core.humanverification.presentation.observe
import me.proton.core.humanverification.presentation.onHumanVerificationNeeded
import me.proton.core.user.domain.UserManager
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val product: Product,
    private val requiredAccountType: AccountType,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val humanVerificationManager: HumanVerificationManager,
    private val authOrchestrator: AuthOrchestrator,
    private val hvOrchestrator: HumanVerificationOrchestrator
) : ViewModel() {

    val state: StateFlow<State> = accountManager.getAccounts()
        .map { accounts ->
            when {
                accounts.isEmpty() || accounts.all { it.isDisabled() } -> AccountNeeded
                accounts.any { it.isReady() } -> PrimaryExist
                accounts.any { it.isStepNeeded() } -> StepNeeded
                else -> Processing
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Processing
        )

    fun register(context: FragmentActivity) {
        authOrchestrator.register(context)
        hvOrchestrator.register(context)

        authOrchestrator.onAddAccountResult { result ->
            viewModelScope.launch {
                if (result == null && getPrimaryUserIdOrNull() == null) {
                    context.finish()
                }
            }
        }

        accountManager.observe(context.lifecycle, Lifecycle.State.CREATED)
            .onSessionForceLogout { userManager.lock(it.userId) }
            .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
            .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
            .onSessionSecondFactorNeeded { authOrchestrator.startSecondFactorWorkflow(it) }
            .onAccountTwoPassModeNeeded { authOrchestrator.startTwoPassModeWorkflow(it) }
            .onAccountCreateAddressNeeded { authOrchestrator.startChooseAddressWorkflow(it) }

        humanVerificationManager.observe(context.lifecycle, Lifecycle.State.RESUMED)
            .onHumanVerificationNeeded { hvOrchestrator.startHumanVerificationWorkflow(it) }
    }

    fun addAccount() = viewModelScope.launch {
        authOrchestrator.startAddAccountWorkflow(requiredAccountType, product)
    }

    fun signIn(userId: UserId? = null) = viewModelScope.launch {
        val account = userId?.let { getAccountOrNull(it) }
        authOrchestrator.startLoginWorkflow(requiredAccountType, username = account?.username)
    }

    fun signOut(userId: UserId? = null) = viewModelScope.launch {
        accountManager.disableAccount(requireNotNull(userId ?: getPrimaryUserIdOrNull()))
    }

    fun switch(userId: UserId) = viewModelScope.launch {
        val account = getAccountOrNull(userId) ?: return@launch
        when {
            account.isDisabled() -> signIn(userId)
            account.isReady() -> accountManager.setAsPrimary(userId)
        }
    }

    fun remove(userId: UserId) = viewModelScope.launch {
        accountManager.removeAccount(userId)
    }

    private suspend fun getAccountOrNull(it: UserId) = accountManager.getAccount(it).firstOrNull()
    private suspend fun getPrimaryUserIdOrNull() = accountManager.getPrimaryUserId().firstOrNull()

    enum class State { Processing, AccountNeeded, PrimaryExist, StepNeeded }
}
