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

package ch.protonmail.android.composer.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.RecipientsExpirationSupport
import uniffi.mail_uniffi.DraftExpirationError
import uniffi.mail_uniffi.DraftExpirationErrorReason
import uniffi.mail_uniffi.DraftExpirationTime
import uniffi.mail_uniffi.DraftRecipientExpirationFeatureReport
import kotlin.time.Instant

fun MessageExpirationTime.toLocalExpirationTime() = when (this) {
    is MessageExpirationTime.Custom -> DraftExpirationTime.Custom(this.expiresAt.epochSeconds.toULong())
    is MessageExpirationTime.Never -> DraftExpirationTime.Never
    is MessageExpirationTime.OneDay -> DraftExpirationTime.OneDay
    is MessageExpirationTime.OneHour -> DraftExpirationTime.OneHour
    is MessageExpirationTime.ThreeDays -> DraftExpirationTime.ThreeDays
}

fun DraftExpirationError.toMessageExpirationError() = when (this) {
    is DraftExpirationError.Other -> MessageExpirationError.Other(this.v1.toDataError())
    is DraftExpirationError.Reason -> when (this.v1) {
        DraftExpirationErrorReason.EXPIRATION_TIME_IN_THE_PAST -> MessageExpirationError.ExpirationTimeInThePast
        DraftExpirationErrorReason.EXPIRATION_TIME_EXCEEDS30_DAYS -> MessageExpirationError.ExpirationTimeTooFarAhead
        DraftExpirationErrorReason.EXPIRATION_TIME_LESS_THAN15_MIN -> MessageExpirationError.ExpirationTimeLessThan15Min
    }
}

fun DraftExpirationTime.toMessageExpiration() = when (this) {
    is DraftExpirationTime.Custom -> MessageExpirationTime.Custom(Instant.fromEpochSeconds(this.v1.toLong()))
    is DraftExpirationTime.Never -> MessageExpirationTime.Never
    is DraftExpirationTime.OneDay -> MessageExpirationTime.OneDay
    is DraftExpirationTime.OneHour -> MessageExpirationTime.OneHour
    is DraftExpirationTime.ThreeDays -> MessageExpirationTime.ThreeDays
}

fun DraftRecipientExpirationFeatureReport.toRecipientsNotSupportingExpiration() = RecipientsExpirationSupport(
    supported = this.supported,
    unsupported = this.unsupported,
    unknown = this.unknown
)
