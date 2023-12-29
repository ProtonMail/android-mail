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

package ch.protonmail.android.mailsettings.domain.model.autolock

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
sealed interface AutoLockPinContinuationAction {

    @Serializable
    data class NavigateToDeepLink(val destination: EncodedDestination) : AutoLockPinContinuationAction

    @Serializable
    object None : AutoLockPinContinuationAction

    @JvmInline
    @Serializable
    value class EncodedDestination private constructor(val value: String) {

        @OptIn(ExperimentalEncodingApi::class)
        fun toDecodedValue() = Base64.decode(value).decodeToString()

        companion object {

            @OptIn(ExperimentalEncodingApi::class)
            fun fromRawValue(rawValue: String) = EncodedDestination(Base64.encode(rawValue.toByteArray()))
        }
    }
}
