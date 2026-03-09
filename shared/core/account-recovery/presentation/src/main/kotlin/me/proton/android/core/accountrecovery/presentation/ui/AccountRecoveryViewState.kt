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

package me.proton.android.core.accountrecovery.presentation.ui

import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.core.presentation.utils.StringBox
import uniffi.mail_uniffi.AccountRecoveryScreenId

sealed interface AccountRecoveryViewState {

    data object Loading : AccountRecoveryViewState
    data class Closed(val passwordResetCancelled: Boolean = false) : AccountRecoveryViewState
    data class Error(val message: String?) : AccountRecoveryViewState
    data class StartPasswordManager(val userId: CoreUserId) : AccountRecoveryViewState

    sealed interface Opened : AccountRecoveryViewState {

        data class GracePeriodStarted(
            val email: String,
            val remainingHours: Int
        ) : Opened

        sealed interface PasswordChangePeriodStarted : Opened {
            data class SelfInitiated(val endDate: String) : PasswordChangePeriodStarted
            data class OtherDeviceInitiated(val endDate: String) : PasswordChangePeriodStarted
        }

        data class RecoveryEnded(val email: String) : Opened

        data object CancellationHappened : Opened

        sealed interface Cancellation : Opened {
            data object Init : Cancellation

            data object Processing : Cancellation

            data object Success : Cancellation

            data class Error(
                val passwordError: StringBox? = null,
                val error: String? = null
            ) : Cancellation
        }
    }

    fun toScreenId(): AccountRecoveryScreenId? = when (this) {
        is Opened.CancellationHappened -> AccountRecoveryScreenId.RECOVERY_CANCELLED_INFO
        is Opened.GracePeriodStarted -> AccountRecoveryScreenId.GRACE_PERIOD_INFO
        is Opened.Cancellation -> AccountRecoveryScreenId.CANCEL_RESET_PASSWORD
        is Opened.PasswordChangePeriodStarted -> AccountRecoveryScreenId.PASSWORD_CHANGE_INFO
        is Opened.RecoveryEnded -> AccountRecoveryScreenId.RECOVERY_EXPIRED_INFO
        else -> null
    }
}
