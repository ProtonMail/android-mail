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

package me.proton.android.core.devicemigration.presentation.origin.usecase

import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.repository.getPrimarySession
import uniffi.mail_account_uniffi.ProcessTargetDeviceQrError
import uniffi.mail_uniffi.MailUserSessionProcessTargetDeviceQrCodeResult
import javax.inject.Inject

internal class ForkSessionIntoTargetDevice @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(qrCode: String): Result {
        val session = requireNotNull(userSessionRepository.getPrimarySession()) {
            "No primary session."
        }

        return when (val result = session.processTargetDeviceQrCode(qrCode = qrCode)) {
            is MailUserSessionProcessTargetDeviceQrCodeResult.Error -> Result.Error(result.v1.getErrorMessage())
            is MailUserSessionProcessTargetDeviceQrCodeResult.Ok -> Result.Success
        }
    }

    sealed interface Result {
        data class Error(val message: String) : Result
        data object Success : Result
    }
}

private fun ProcessTargetDeviceQrError.getErrorMessage(): String = when (this) {
    is ProcessTargetDeviceQrError.Api -> v1
    is ProcessTargetDeviceQrError.EncryptionFailed -> v1
    is ProcessTargetDeviceQrError.Other -> v1
    is ProcessTargetDeviceQrError.ParseError -> v1
    is ProcessTargetDeviceQrError.PassphraseAcquire -> v1
}
