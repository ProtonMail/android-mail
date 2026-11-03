/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.debug.environment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsession.data.usecase.SignOutAllAccounts
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Progress of an Apply or Reset from the dialog. */
internal enum class EnvironmentSwitchState { Idle, Switching, Done, Failed }

/**
 * The environment chosen in the debug menu. The app keeps using [EnvironmentDebugViewModel.runningHost] until it is
 * restarted, so a choice that differs from it is pending.
 */
internal data class EnvironmentSelection(
    val host: String,
    val isOverridden: Boolean,
    val recentHosts: List<String>
)

/**
 * The Environment section of the debug menu: which proton.black environment the app uses, and switching it.
 *
 * An account only exists on the environment it signed in to, so switching signs out every account first, while the
 * app still talks to that environment.
 */
@HiltViewModel
internal class EnvironmentDebugViewModel @Inject constructor(
    private val store: DebugEnvironmentStore,
    private val userSessionRepository: UserSessionRepository,
    private val signOutAllAccounts: SignOutAllAccounts
) : ViewModel() {

    /** Read at app start, like the API client, so it is the environment in use until the restart. */
    val runningHost: String = store.effectiveHost()

    /** The build's own environment, which the app uses without an override. */
    val defaultHost: String = store.defaultHost()

    private val mutableSelection = MutableStateFlow(
        EnvironmentSelection(
            host = runningHost,
            isOverridden = store.host() != null,
            recentHosts = store.recentHosts()
        )
    )
    val selection: StateFlow<EnvironmentSelection> = mutableSelection.asStateFlow()

    /** Whether switching now would sign out an account. */
    val hasSignedInAccounts: Flow<Boolean> = userSessionRepository.observeAccounts()
        .map { accounts -> accounts.any(Account::isSignedIn) }
        .distinctUntilChanged()

    private val mutableSwitchState = MutableStateFlow(EnvironmentSwitchState.Idle)
    val switchState: StateFlow<EnvironmentSwitchState> = mutableSwitchState.asStateFlow()

    /**
     * Stores the environment for [input] for the next app start; invalid input or the current choice is ignored.
     *
     * Runs in [viewModelScope] so closing the dialog can't stop it between the sign-out and storing the host.
     */
    fun save(input: String) = switch {
        val current = mutableSelection.value
        when (val parsed = EnvironmentHost.parse(input, defaultHost)) {
            EnvironmentHost.Input.Invalid -> true
            EnvironmentHost.Input.Default -> !current.isOverridden || resetToDefault()
            is EnvironmentHost.Input.Valid -> parsed.host == current.host || saveHost(parsed.host)
        }
    }

    fun reset() = switch { resetToDefault() }

    /** Clears a finished switch when the dialog closes, so the next one starts afresh. */
    fun onDialogClosed() {
        mutableSwitchState.update { if (it == EnvironmentSwitchState.Switching) it else EnvironmentSwitchState.Idle }
    }

    private fun switch(block: suspend () -> Boolean) {
        if (mutableSwitchState.getAndUpdate { EnvironmentSwitchState.Switching } == EnvironmentSwitchState.Switching) {
            return
        }
        viewModelScope.launch {
            mutableSwitchState.value = if (block()) EnvironmentSwitchState.Done else EnvironmentSwitchState.Failed
        }
    }

    private suspend fun saveHost(host: String): Boolean {
        if (!signOutAccounts()) return false
        val recentHosts = store.save(host)
        mutableSelection.value = EnvironmentSelection(host, isOverridden = true, recentHosts)
        return true
    }

    private suspend fun resetToDefault(): Boolean {
        if (!signOutAccounts()) return false
        store.clear()
        mutableSelection.update { it.copy(host = store.defaultHost(), isOverridden = false) }
        return true
    }

    /** Returns false when an account is still signed in, so the switch must not happen. */
    private suspend fun signOutAccounts(): Boolean {
        if (!hasSignedInAccounts.first()) return true
        return signOutAllAccounts()
            .onLeft { Timber.e("debug-environment: unable to sign out before switching: $it") }
            .onRight { Timber.d("debug-environment: signed out every account before switching") }
            .isRight()
    }
}

private fun Account.isSignedIn() = state != AccountState.Disabled
