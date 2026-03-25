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

package me.proton.android.core.auth.presentation.secondfactor

import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.flow.FlowManager.CurrentFlow
import me.proton.android.core.auth.presentation.secondfactor.getAccountById
import me.proton.android.core.auth.presentation.secondfactor.getSessionsForAccount
import uniffi.mail_account_uniffi.LoginFlowTfaMethodsResult
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailUserSessionUserSettingsResult
import uniffi.mail_uniffi.TfaStatus
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.mail_account_uniffi.TfaMethods as UniffiTfaMethods

@Singleton
class GetTfaMethods @Inject constructor(
    private val flowManager: FlowManager,
    private val sessionInterface: MailSession
) {

    suspend operator fun invoke(userId: CoreUserId): TfaMethods? {
        return when (val flow = flowManager.getCurrentActiveFlow(userId)) {
            is CurrentFlow.LoggingIn -> when (val result = flow.flow.tfaMethods()) {
                is LoginFlowTfaMethodsResult.Error -> null
                is LoginFlowTfaMethodsResult.Ok -> result.v1.toLocalTfaMethods()
            }
            is CurrentFlow.ChangingPassword -> {
                val session = sessionInterface.getSessionsForAccount(
                    sessionInterface.getAccountById(userId.id)
                )?.firstOrNull() ?: return null

                val userSession = when (val result = sessionInterface.userSessionFromStoredSession(session)) {
                    is MailSessionUserSessionFromStoredSessionResult.Error -> return null
                    is MailSessionUserSessionFromStoredSessionResult.Ok -> result.v1
                }

                when (val settingsResult = userSession.userSettings()) {
                    is MailUserSessionUserSettingsResult.Error -> null
                    is MailUserSessionUserSettingsResult.Ok ->
                        settingsResult.v1.twoFactorAuth.enabled.toLocalTfaMethods()
                }
            }
        }
    }
}

private fun UniffiTfaMethods.toLocalTfaMethods(): TfaMethods = when (this) {
    UniffiTfaMethods.Totp -> TfaMethods.Totp
    UniffiTfaMethods.Fido2 -> TfaMethods.Fido2
    UniffiTfaMethods.TotpAndFido2 -> TfaMethods.TotpAndFido2
}

private fun TfaStatus.toLocalTfaMethods(): TfaMethods? = when (this) {
    TfaStatus.TOTP -> TfaMethods.Totp
    TfaStatus.FIDO2 -> TfaMethods.Fido2
    TfaStatus.TOTP_OR_FIDO2 -> TfaMethods.TotpAndFido2
    TfaStatus.NONE -> null
}
