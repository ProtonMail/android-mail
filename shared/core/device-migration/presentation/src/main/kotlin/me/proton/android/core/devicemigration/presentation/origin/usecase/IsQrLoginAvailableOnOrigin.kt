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
import me.proton.core.biometric.data.StrongAuthenticatorsResolver
import me.proton.core.biometric.domain.CheckBiometricAuthAvailability
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionUserSettingsResult
import javax.inject.Inject

internal class IsQrLoginAvailableOnOrigin @Inject constructor(
    private val checkBiometricAuthAvailability: CheckBiometricAuthAvailability,
    private val strongAuthenticatorsResolver: StrongAuthenticatorsResolver,
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(): Boolean {
        val session = requireNotNull(userSessionRepository.getPrimarySession()) {
            "No primary session."
        }
        return hasBiometrics() && !hasOptedOut(session)
    }

    private fun hasBiometrics(): Boolean =
        checkBiometricAuthAvailability(authenticatorsResolver = strongAuthenticatorsResolver).canAttemptBiometricAuth()

    private suspend fun hasOptedOut(session: MailUserSession): Boolean = when (val result = session.userSettings()) {
        is MailUserSessionUserSettingsResult.Error -> true
        is MailUserSessionUserSettingsResult.Ok -> result.v1.flags.edmOptOut
    }
}
