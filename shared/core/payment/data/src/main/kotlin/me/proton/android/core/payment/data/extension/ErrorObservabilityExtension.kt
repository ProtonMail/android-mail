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
import uniffi.mail_uniffi.OtherErrorReason
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.SessionReason
import uniffi.mail_uniffi.UserSessionError
import uniffi.mail_uniffi_common.UserApiServiceError
import uniffi.mail_uniffi_common.UserApiServiceError.BadGateway
import uniffi.mail_uniffi_common.UserApiServiceError.BadRequest
import uniffi.mail_uniffi_common.UserApiServiceError.Forbidden
import uniffi.mail_uniffi_common.UserApiServiceError.Internal
import uniffi.mail_uniffi_common.UserApiServiceError.InternalServerError
import uniffi.mail_uniffi_common.UserApiServiceError.NetworkFailure
import uniffi.mail_uniffi_common.UserApiServiceError.NotFound
import uniffi.mail_uniffi_common.UserApiServiceError.NotImplemented
import uniffi.mail_uniffi_common.UserApiServiceError.OtherHttpError
import uniffi.mail_uniffi_common.UserApiServiceError.ServiceUnavailable
import uniffi.mail_uniffi_common.UserApiServiceError.TooManyRequests
import uniffi.mail_uniffi_common.UserApiServiceError.Unauthorized
import uniffi.mail_uniffi_common.UserApiServiceError.UnprocessableEntity

fun UserSessionError.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is UserSessionError.Other -> v1.toObservabilityValue()
        is UserSessionError.Reason -> v1.toObservabilityValue()
    }
}

fun SessionReason.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is SessionReason.DuplicateSession,
        is SessionReason.MethodCalledInWrongOrigin,
        is SessionReason.UserSessionNotInitialized -> PaymentObservabilityValue.HTTP4XX
        is SessionReason.UnknownLabel -> PaymentObservabilityValue.UNKNOWN
    }
}

fun ProtonError.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is ProtonError.OtherReason -> v1.toObservabilityValue()
        is ProtonError.ServerError -> v1.toObservabilityValue()
        is ProtonError.Network,
        is ProtonError.Unexpected,
        is ProtonError.NonProcessableActions -> PaymentObservabilityValue.UNKNOWN
    }
}

fun OtherErrorReason.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is OtherErrorReason.InvalidParameter -> PaymentObservabilityValue.HTTP4XX
        else -> PaymentObservabilityValue.UNKNOWN
    }
}

fun UserApiServiceError.toObservabilityValue(): PaymentObservabilityValue {
    return when (this) {
        is TooManyRequests,
        is Unauthorized,
        is UnprocessableEntity,
        is NotFound,
        is BadRequest,
        is Forbidden -> PaymentObservabilityValue.HTTP4XX
        is BadGateway,
        is Internal,
        is InternalServerError,
        is NotImplemented,
        is ServiceUnavailable -> PaymentObservabilityValue.HTTP5XX
        is OtherHttpError,
        is NetworkFailure -> PaymentObservabilityValue.UNKNOWN
    }
}
