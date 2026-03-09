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

package ch.protonmail.android.legacymigration.data.mapper

import ch.protonmail.android.legacymigration.domain.model.AccountMigrationInfo
import ch.protonmail.android.legacymigration.domain.model.AccountPasswordMode
import ch.protonmail.android.legacymigration.domain.model.LegacyMobileSignaturePreference
import ch.protonmail.android.legacymigration.domain.model.LegacySessionInfo
import ch.protonmail.android.legacymigration.domain.model.LegacySignaturePreference
import ch.protonmail.android.legacymigration.domain.model.LegacyUserAddressInfo
import ch.protonmail.android.legacymigration.domain.model.LegacyUserInfo
import uniffi.mail_account_uniffi.MigrationData
import uniffi.mail_account_uniffi.PasswordMode
import javax.inject.Inject

@Suppress("LongParameterList")
class MigrationInfoMapper @Inject constructor() {
    fun mapToAccountMigrationInfo(
        sessionInfo: LegacySessionInfo,
        user: LegacyUserInfo,
        userAddress: LegacyUserAddressInfo,
        isPrimaryUser: Boolean,
        signaturePreference: LegacySignaturePreference,
        mobileSignaturePreference: LegacyMobileSignaturePreference
    ): AccountMigrationInfo {
        val userName = user.name ?: userAddress.email
        val displayName = user.displayName ?: user.name ?: userAddress.email

        return AccountMigrationInfo(
            userId = sessionInfo.userId,
            username = userName,
            primaryAddr = userAddress.email,
            displayName = displayName,
            sessionId = sessionInfo.sessionId,
            refreshToken = sessionInfo.refreshToken,
            keySecret = user.passPhrase,
            passwordMode = if (sessionInfo.twoPassModeEnabled)
                AccountPasswordMode.TWO
            else
                AccountPasswordMode.ONE,
            isPrimaryUser = isPrimaryUser,
            addressSignatureEnabled = signaturePreference.isEnabled,
            mobileSignatureEnabled = mobileSignaturePreference.enabled,
            mobileSignature = mobileSignaturePreference.value
        )
    }
}

fun AccountMigrationInfo.toMigrationData(): MigrationData {
    return MigrationData(
        userId = userId.id,
        username = username,
        primaryAddr = primaryAddr,
        displayName = displayName,
        sessionId = sessionId.id,
        refreshToken = refreshToken,
        keySecret = keySecret,
        passwordMode = if (passwordMode == AccountPasswordMode.ONE)
            PasswordMode.ONE
        else
            PasswordMode.TWO,
        addressSignatureEnabled = addressSignatureEnabled,
        mobileSignature = mobileSignature,
        mobileSignatureEnabled = mobileSignatureEnabled
    )
}
