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

package me.proton.android.core.humanverification.presentation

sealed interface HumanVerificationOperation

sealed interface HumanVerificationAction : HumanVerificationOperation {
    data object NoOp : HumanVerificationAction
    data class Load(
        val url: String,
        val defaultCountry: String?,
        val recoveryPhone: String?,
        val locale: String?,
        val headers: List<Pair<String, String>>?,

        // For legacy purpose only; if not null, then alt routing is active.
        val originalHost: String?,
        val alternativeHost: String?
    ) : HumanVerificationAction

    data class Cancel(val unused: Long = System.currentTimeMillis()) : HumanVerificationAction
    data class HVMessage(val message: HV3ResponseMessage) : HumanVerificationAction
    data class WebError(val error: WebResponseError?) : HumanVerificationAction
}
