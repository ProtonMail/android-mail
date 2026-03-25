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
import uniffi.mail_account_uniffi.LoginFlowTfaMethodsResult
import uniffi.mail_account_uniffi.PasswordFlowHasFidoResult
import uniffi.mail_account_uniffi.PasswordFlowHasTotpResult
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.mail_account_uniffi.TfaMethods as UniffiTfaMethods

@Singleton
class GetTfaMethods @Inject constructor(
    private val flowManager: FlowManager
) {

    suspend operator fun invoke(userId: CoreUserId): TfaMethods? {
        return when (val flow = flowManager.getCurrentActiveFlow(userId)) {
            is CurrentFlow.LoggingIn -> when (val result = flow.flow.tfaMethods()) {
                is LoginFlowTfaMethodsResult.Error -> null
                is LoginFlowTfaMethodsResult.Ok -> result.v1.toLocalTfaMethods()
            }
            is CurrentFlow.ChangingPassword -> {
                val hasFido = when (val result = flow.flow.hasFido()) {
                    is PasswordFlowHasFidoResult.Error -> return null
                    is PasswordFlowHasFidoResult.Ok -> result.v1
                }
                val hasTotp = when (val result = flow.flow.hasTotp()) {
                    is PasswordFlowHasTotpResult.Error -> return null
                    is PasswordFlowHasTotpResult.Ok -> result.v1
                }
                when {
                    hasFido && hasTotp -> TfaMethods.TotpAndFido2
                    hasFido -> TfaMethods.Fido2
                    hasTotp -> TfaMethods.Totp
                    else -> null
                }
            }
        }
    }
}

private fun UniffiTfaMethods.toLocalTfaMethods(): TfaMethods = when (this) {
    UniffiTfaMethods.TOTP -> TfaMethods.Totp
    UniffiTfaMethods.FIDO2 -> TfaMethods.Fido2
    UniffiTfaMethods.TOTP_AND_FIDO2 -> TfaMethods.TotpAndFido2
}

