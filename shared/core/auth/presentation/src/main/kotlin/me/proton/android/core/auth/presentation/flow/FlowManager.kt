/*
 * Copyright (c) 2025 Proton Technologies AG
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

package me.proton.android.core.auth.presentation.flow

import me.proton.android.core.account.domain.model.CoreAccount
import me.proton.android.core.account.domain.model.CoreAccountState
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.account.domain.model.toCoreAccount
import me.proton.android.core.auth.presentation.secondfactor.getAccountById
import me.proton.android.core.auth.presentation.secondfactor.getSessionsForAccount
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.PasswordFlow
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionResumeLoginFlowResult
import uniffi.mail_uniffi.MailSessionToUserSessionResult
import uniffi.mail_uniffi.MailSessionUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionNewPasswordChangeFlowResult
import uniffi.mail_uniffi.StoredSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowManager @Inject constructor(
    private val sessionInterface: MailSession,
    private val flowCache: FlowCache
) {

    sealed class CurrentFlow {
        data class LoggingIn(val flow: LoginFlow) : CurrentFlow()
        data class ChangingPassword(val flow: PasswordFlow) : CurrentFlow()
    }

    suspend fun getCurrentActiveFlow(userId: CoreUserId, clear: Boolean = false): CurrentFlow {
        val account = sessionInterface.getAccountById(userId.id)?.toCoreAccount()
        flowCache.clearIfUserChanged(userId.id)
        val activeFlow = getActiveFlow(account)
        if (activeFlow != null && !clear) {
            return activeFlow
        }
        return when (account?.state) {
            CoreAccountState.Ready -> tryCreatePasswordFlow(userId.id)
            CoreAccountState.TwoFactorNeeded,
            CoreAccountState.NewPasswordNeeded -> tryResumeLoginFlow(userId.id)

            else -> throw SessionException("Failed to get flow")
        }
    }

    private fun getActiveFlow(coreAccount: CoreAccount?): CurrentFlow? {
        val activeFlow = flowCache.getActiveFlow()
        if (coreAccount == null) {
            throw SessionException("Failed to get flow")
        }
        return if (activeFlow != null) {
            when (activeFlow) {
                is CurrentFlow.ChangingPassword ->
                    if (coreAccount.state != CoreAccountState.Ready)
                        null
                    else activeFlow

                is CurrentFlow.LoggingIn ->
                    if (coreAccount.state == CoreAccountState.Ready) null
                    else activeFlow
            }
        } else null
    }

    private suspend fun tryResumeLoginFlow(userId: String): CurrentFlow {
        val loginFlow = run {
            val session = getActiveSession(userId) ?: throw SessionException("Failed to get user context 3")
            when (val result = sessionInterface.resumeLoginFlow(userId, session.sessionId())) {
                is MailSessionResumeLoginFlowResult.Error -> null
                is MailSessionResumeLoginFlowResult.Ok -> result.v1
            }
        } ?: throw SessionException("Failed to get user context 4")

        return CurrentFlow.LoggingIn(loginFlow).also {
            flowCache.setActiveFlow(it)
        }
    }

    private suspend fun tryCreatePasswordFlow(userId: String): CurrentFlow {
        val passwordFlow = run {
            val session = getActiveSession(userId) ?: throw SessionException("Failed to get user context 2")
            val userSession = getUserSession(session)
            createPasswordChangeFlow(userSession)
        }

        return CurrentFlow.ChangingPassword(passwordFlow).also {
            flowCache.setActiveFlow(it)
        }
    }

    suspend fun convertToUserContext(loginFlow: LoginFlow): MailSessionToUserSessionResult =
        sessionInterface.toUserSession(loginFlow)

    suspend fun clearCache(userId: CoreUserId): Boolean {
        val activeFlow = flowCache.getActiveFlow()
        val account = sessionInterface.getAccountById(userId.id)?.toCoreAccount()

        deleteAccountIfNeeded(account?.state, activeFlow)

        return when (activeFlow) {
            is CurrentFlow.ChangingPassword ->
                if (account?.state != CoreAccountState.Ready) {
                    flowCache.clear()
                } else false

            is CurrentFlow.LoggingIn -> when (account?.state) {
                CoreAccountState.TwoPasswordNeeded,
                CoreAccountState.TwoFactorNeeded,
                CoreAccountState.NotReady,
                CoreAccountState.Ready -> flowCache.clear()

                else -> false
            }

            null -> false
        }
    }

    private suspend fun deleteAccountIfNeeded(state: CoreAccountState?, activeFlow: CurrentFlow?) {
        when (activeFlow) {
            is CurrentFlow.ChangingPassword ->
                if (state != CoreAccountState.Ready) {
                    flowCache.deleteAccount()
                }

            is CurrentFlow.LoggingIn ->
                if (state != CoreAccountState.Ready && state != CoreAccountState.TwoPasswordNeeded) {
                    flowCache.deleteAccount()
                }

            null -> Unit
        }
    }

    private suspend fun getActiveSession(userId: String): StoredSession? {
        return flowCache.getCachedSession() ?: run {
            val account = sessionInterface.getAccountById(userId)
            sessionInterface.getSessionsForAccount(account)
                ?.firstOrNull()
                ?.also { session ->
                    flowCache.setCachedSession(session, userId)
                }
        }
    }

    private suspend fun getUserSession(session: StoredSession): MailUserSession {
        return when (val result = sessionInterface.userSessionFromStoredSession(session)) {
            is MailSessionUserSessionFromStoredSessionResult.Error ->
                throw SessionException("Failed to get user context")

            is MailSessionUserSessionFromStoredSessionResult.Ok -> result.v1
        }
    }

    private suspend fun createPasswordChangeFlow(userSession: MailUserSession): PasswordFlow {
        return when (val result = userSession.newPasswordChangeFlow()) {
            is MailUserSessionNewPasswordChangeFlowResult.Error ->
                throw SessionException("Failed to create password change flow")

            is MailUserSessionNewPasswordChangeFlowResult.Ok -> result.v1
        }
    }

    class SessionException(message: String) : Exception(message)
}
