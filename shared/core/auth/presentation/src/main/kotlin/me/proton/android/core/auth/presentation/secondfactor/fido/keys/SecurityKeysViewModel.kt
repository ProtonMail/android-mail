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

package me.proton.android.core.auth.presentation.secondfactor.fido.keys

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import me.proton.android.core.auth.presentation.LogTag
import me.proton.android.core.auth.presentation.secondfactor.fido.keys.SecurityKeysAction.Load
import me.proton.android.core.auth.presentation.secondfactor.fido.keys.SecurityKeysState.Error
import me.proton.android.core.auth.presentation.secondfactor.fido.keys.SecurityKeysState.Loading
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_uniffi.FidoKey
import javax.inject.Inject

@HiltViewModel
class SecurityKeysViewModel @Inject constructor(
    private val observeSecurityKeys: ObserveSecurityKeys
) : BaseViewModel<SecurityKeysAction, SecurityKeysState>(
    initialAction = Load(),
    initialState = Loading
) {

    override suspend fun FlowCollector<SecurityKeysState>.onError(throwable: Throwable) {
        CoreLogger.e(LogTag.SECURITY_KEYS, throwable)
        emit(Error.General(throwable))
    }

    override fun onAction(action: SecurityKeysAction): Flow<SecurityKeysState> {
        return when (action) {
            is Load -> observeState()
        }
    }

    private fun observeState(): Flow<SecurityKeysState> = flow {
        emit(Loading)

        observeSecurityKeys().collect { keys ->
            emit(SecurityKeysState.Success(keys))
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
data class Fido2RegisteredKey(
    val attestationFormat: String,
    val credentialID: UByteArray,
    val name: String
)

fun List<FidoKey>.toFido2SecurityKeys(): List<Fido2RegisteredKey> = map { it.toFido2RegisteredKey() }

@OptIn(ExperimentalUnsignedTypes::class)
fun FidoKey.toFido2RegisteredKey() = Fido2RegisteredKey(
    attestationFormat = this.attestationFormat,
    credentialID = this.credentialId.map { it.toUByte() }.toUByteArray(),
    name = this.name
)
