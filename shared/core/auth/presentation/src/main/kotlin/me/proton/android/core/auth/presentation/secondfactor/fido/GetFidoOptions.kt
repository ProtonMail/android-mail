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

import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.flow.FlowManager.CurrentFlow
import uniffi.proton_account_uniffi.Fido2ResponseFfi
import uniffi.proton_account_uniffi.LoginFlowGetFidoDetailsResult
import uniffi.proton_account_uniffi.PasswordFlowFidoDetailsResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFidoOptions @Inject constructor(
    private val flowManager: FlowManager
) {

    suspend operator fun invoke(userId: CoreUserId): Fido2ResponseFfi? {
        return when (val fidoFlowResult = flowManager.getCurrentActiveFlow(userId)) {
            is CurrentFlow.LoggingIn -> when (val fidoDetailsResult = fidoFlowResult.flow.getFidoDetails()) {
                is LoginFlowGetFidoDetailsResult.Error -> null
                is LoginFlowGetFidoDetailsResult.Ok -> fidoDetailsResult.v1
            }
            is CurrentFlow.ChangingPassword -> {
                when (val fidoDetailsResult = fidoFlowResult.flow.fidoDetails()) {
                    is PasswordFlowFidoDetailsResult.Error -> null
                    is PasswordFlowFidoDetailsResult.Ok -> fidoDetailsResult.v1
                }
            }
        }
    }
}
