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

package me.proton.android.core.payment.data.extension

import me.proton.android.core.payment.domain.model.PaymentObservabilityValue
import uniffi.mail_uniffi.MailUserSessionGetPaymentsPlansResult
import uniffi.mail_uniffi.MailUserSessionGetPaymentsStatusResult
import uniffi.mail_uniffi.MailUserSessionGetPaymentsSubscriptionResult
import uniffi.mail_uniffi.MailUserSessionPostPaymentsSubscriptionResult
import uniffi.mail_uniffi.MailUserSessionPostPaymentsTokensResult

fun MailUserSessionPostPaymentsTokensResult.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is MailUserSessionPostPaymentsTokensResult.Error -> v1.toObservabilityValue()
        is MailUserSessionPostPaymentsTokensResult.Ok -> PaymentObservabilityValue.SUCCESS
    }
}

fun MailUserSessionPostPaymentsSubscriptionResult.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is MailUserSessionPostPaymentsSubscriptionResult.Error -> v1.toObservabilityValue()
        is MailUserSessionPostPaymentsSubscriptionResult.Ok -> PaymentObservabilityValue.SUCCESS
    }
}

fun MailUserSessionGetPaymentsSubscriptionResult.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is MailUserSessionGetPaymentsSubscriptionResult.Error -> v1.toObservabilityValue()
        is MailUserSessionGetPaymentsSubscriptionResult.Ok -> PaymentObservabilityValue.SUCCESS
    }
}

fun MailUserSessionGetPaymentsPlansResult.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is MailUserSessionGetPaymentsPlansResult.Error -> v1.toObservabilityValue()
        is MailUserSessionGetPaymentsPlansResult.Ok -> PaymentObservabilityValue.SUCCESS
    }
}

fun MailUserSessionGetPaymentsStatusResult.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is MailUserSessionGetPaymentsStatusResult.Error -> v1.toObservabilityValue()
        is MailUserSessionGetPaymentsStatusResult.Ok -> PaymentObservabilityValue.SUCCESS
    }
}
