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

package me.proton.android.core.auth.presentation.secondfactor.fido

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.flow.FlowManager.CurrentFlow
import me.proton.android.core.auth.presentation.login.getErrorMessage
import me.proton.android.core.auth.presentation.passmanagement.getErrorMessage
import me.proton.core.auth.fido.domain.entity.SecondFactorProof
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowSubmitFidoResult
import uniffi.mail_account_uniffi.PasswordFlow
import uniffi.mail_account_uniffi.PasswordFlowSubmitFidoResult
import uniffi.mail_uniffi.MailSessionToUserSessionResult
import javax.inject.Inject

class SubmitFido @Inject constructor(
    @ApplicationContext private val context: Context,
    private val flowManager: FlowManager
) {

    suspend fun execute(userId: CoreUserId, proof: SecondFactorProof.Fido2): SubmitFidoResult {
        return when (val submitFidoFlow = flowManager.getCurrentActiveFlow(userId)) {
            is CurrentFlow.LoggingIn -> handleLoginFlowSubmission(submitFidoFlow.flow, proof)
            is CurrentFlow.ChangingPassword -> handlePasswordFlowSubmission(submitFidoFlow.flow, proof)
        }
    }

    private suspend fun handleLoginFlowSubmission(
        loginFlow: LoginFlow,
        proof: SecondFactorProof.Fido2
    ): SubmitFidoResult {
        val fidoData = proof.toFido2Data()
        return when (val submit = loginFlow.submitFido(fidoData)) {
            is LoginFlowSubmitFidoResult.Error ->
                SubmitFidoResult.OtherError(submit.v1.getErrorMessage(context))

            is LoginFlowSubmitFidoResult.Ok ->
                SubmitFidoResult.Success(loginFlow)
        }
    }

    private suspend fun handlePasswordFlowSubmission(
        passwordFlow: PasswordFlow,
        proof: SecondFactorProof.Fido2
    ): SubmitFidoResult {
        val fidoData = proof.toFido2Data()
        return when (val submit = passwordFlow.submitFido(fidoData)) {
            is PasswordFlowSubmitFidoResult.Error -> SubmitFidoResult.OtherError(submit.v1.getErrorMessage(context))
            is PasswordFlowSubmitFidoResult.Ok -> SubmitFidoResult.Success()
        }
    }

    suspend fun convertToUserContext(loginFlow: LoginFlow): MailSessionToUserSessionResult =
        flowManager.convertToUserContext(loginFlow)
}
