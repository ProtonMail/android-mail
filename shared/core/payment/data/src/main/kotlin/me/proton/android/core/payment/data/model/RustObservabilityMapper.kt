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

package me.proton.android.core.payment.data.model

import me.proton.android.core.payment.domain.model.PaymentObservabilityValue
import uniffi.mail_uniffi.PaymentObservabilityResponse

fun PaymentObservabilityValue.toExternal(): PaymentObservabilityResponse {
    return when (this) {
        PaymentObservabilityValue.HTTP4XX -> PaymentObservabilityResponse.HTTP4XX
        PaymentObservabilityValue.HTTP5XX -> PaymentObservabilityResponse.HTTP5XX
        PaymentObservabilityValue.SERIALIZATION_ERROR -> PaymentObservabilityResponse.SERIALIZATION_ERROR
        PaymentObservabilityValue.SUCCESS -> PaymentObservabilityResponse.SUCCESS
        PaymentObservabilityValue.UNKNOWN -> PaymentObservabilityResponse.UNKNOWN
    }
}
