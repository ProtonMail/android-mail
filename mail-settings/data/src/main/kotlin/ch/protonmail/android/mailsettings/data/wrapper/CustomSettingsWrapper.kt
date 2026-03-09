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

package ch.protonmail.android.mailsettings.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMobileSignature
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.CustomSettings
import uniffi.mail_uniffi.CustomSettingsMobileSignatureResult
import uniffi.mail_uniffi.CustomSettingsSetMobileSignatureEnabledResult
import uniffi.mail_uniffi.CustomSettingsSetMobileSignatureResult
import uniffi.mail_uniffi.CustomSettingsSetSwipeToAdjacentConversationResult
import uniffi.mail_uniffi.CustomSettingsSwipeToAdjacentConversationResult

class CustomSettingsWrapper(
    private val customSettings: CustomSettings
) {

    suspend fun getMobileSignature(): Either<DataError, LocalMobileSignature> {
        return when (val result = customSettings.mobileSignature()) {
            is CustomSettingsMobileSignatureResult.Ok -> result.v1.right()
            is CustomSettingsMobileSignatureResult.Error -> result.v1.toDataError().left()
        }
    }

    suspend fun setMobileSignature(signature: String): Either<DataError, Unit> {
        return when (val result = customSettings.setMobileSignature(signature)) {
            is CustomSettingsSetMobileSignatureResult.Ok -> Unit.right()
            is CustomSettingsSetMobileSignatureResult.Error -> result.v1.toDataError().left()
        }
    }

    suspend fun setMobileSignatureEnabled(enabled: Boolean): Either<DataError, Unit> {
        return when (val result = customSettings.setMobileSignatureEnabled(enabled)) {
            is CustomSettingsSetMobileSignatureEnabledResult.Ok -> Unit.right()
            is CustomSettingsSetMobileSignatureEnabledResult.Error -> result.v1.toDataError().left()
        }
    }

    suspend fun getSwipeToAdjacentConversation(): Either<DataError, Boolean> {
        return when (val result = customSettings.swipeToAdjacentConversation()) {
            is CustomSettingsSwipeToAdjacentConversationResult.Ok -> result.v1.right()
            is CustomSettingsSwipeToAdjacentConversationResult.Error -> result.v1.toDataError().left()
        }
    }

    suspend fun setSwipeToAdjacentConversation(enabled: Boolean): Either<DataError, Unit> {
        return when (val result = customSettings.setSwipeToAdjacentConversation(enabled)) {
            is CustomSettingsSetSwipeToAdjacentConversationResult.Error -> result.v1.toDataError().left()
            CustomSettingsSetSwipeToAdjacentConversationResult.Ok -> Unit.right()
        }
    }
}
