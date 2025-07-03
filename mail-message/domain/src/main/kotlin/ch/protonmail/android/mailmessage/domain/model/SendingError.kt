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

package ch.protonmail.android.mailmessage.domain.model

import kotlinx.serialization.Serializable
import me.proton.core.mailmessage.domain.entity.Email

@Serializable
sealed interface SendingError {

    @Serializable
    object Other : SendingError

    @Serializable
    data class ExternalAddressSendDisabled(val apiMessage: String?) : SendingError

    @Serializable
    data class GenericLocalized(val apiMessage: String) : SendingError

    @Serializable
    object MessageAlreadySent : SendingError

    @Serializable
    data class SendPreferences(
        val errors: Map<Email, SendPreferencesError>
    ) : SendingError

    enum class SendPreferencesError {
        AddressDisabled,
        GettingContactPreferences,
        TrustedKeysInvalid,
        NoCorrectlySignedTrustedKeys,
        PublicKeysInvalid
    }
}
