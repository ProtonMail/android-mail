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

package ch.protonmail.android.api

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.mailsession.data.mapper.toLocalUserId
import ch.protonmail.android.mailsession.data.mapper.toUserId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.SetPrimaryAccount
import ch.protonmail.android.mailsession.presentation.observe
import ch.protonmail.android.mailsession.presentation.onAccountNewPasswordNeeded
import ch.protonmail.android.mailsession.presentation.onAccountTwoFactorNeeded
import ch.protonmail.android.mailsession.presentation.onAccountTwoPasswordNeeded
import kotlinx.coroutines.launch
import me.proton.android.core.auth.presentation.AuthOrchestrator
import me.proton.android.core.auth.presentation.login.LoginInput
import me.proton.android.core.auth.presentation.login.LoginOutput
import me.proton.android.core.auth.presentation.onAddAccountResult
import me.proton.android.core.auth.presentation.onLoginResult
import me.proton.android.core.auth.presentation.onSignUpResult
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class LegacyAccountApiProvider @Inject constructor(
    private val orchestrator: AuthOrchestrator,
    private val setPrimaryAccount: SetPrimaryAccount,
    private val userSessionRepository: UserSessionRepository
) : AccountApiProvider {

    override fun register(context: AppCompatActivity) {
        registerAuthOrchestrator(context)
    }

    override fun unregister() {
        orchestrator.unregister()
    }

    override fun startAddAccount() {
        orchestrator.startAddAccountWorkflow()
    }

    override fun startSignIn(username: String?) {
        orchestrator.startLoginWorkflow(LoginInput(username = username))
    }

    override fun startSignUp() {
        orchestrator.startSignUpWorkflow()
    }

    override fun startSettings(userId: UserId) {
        orchestrator.startPassManagement(userId.toLocalUserId())
    }

    override fun startSecurityKeys() {
        orchestrator.startSecurityKeys()
    }

    override suspend fun switchAccount(userId: UserId) {
        setPrimaryAccount(userId)
    }

    override suspend fun disableAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    private fun onSwitchAccount(context: AppCompatActivity, userId: UserId) = context.lifecycleScope.launch {
        switchAccount(userId)
    }

    private fun registerAuthOrchestrator(context: AppCompatActivity) {
        orchestrator.register(context)
        orchestrator.onAddAccountResult { result -> if (!result) context.finish() }
        orchestrator.onLoginResult { result ->
            when (result) {
                is LoginOutput.LoggedIn -> onSwitchAccount(context, result.userId.toUserId())
                // Not handled.
                is LoginOutput.DuplicateAccount -> Unit
                else -> Timber.e("Unknown login result $result")
            }
        }
        orchestrator.onSignUpResult { result ->
            if (result != null) {
                onSwitchAccount(context, result.userId.toUserId())
            }
        }
    }

    override suspend fun registerUserSessionObservers(context: AppCompatActivity) {
        userSessionRepository
            .observe(context.lifecycle, minActiveState = Lifecycle.State.RESUMED)
            .onAccountTwoFactorNeeded { orchestrator.startSecondFactorWorkflow(it.userId.toLocalUserId()) }
            .onAccountTwoPasswordNeeded { orchestrator.startTwoPassModeWorkflow(it.userId.toLocalUserId()) }
            .onAccountNewPasswordNeeded { orchestrator.startPassManagement(it.userId.toLocalUserId()) }
    }
}