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

package ch.protonmail.android.mailpagination.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalMailScrollerError
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import uniffi.mail_uniffi.MailScrollerError
import uniffi.mail_uniffi.MailScrollerErrorReason
import uniffi.mail_uniffi.ProtonError

fun LocalMailScrollerError.toPaginationError(): PaginationError = when (this) {
    is MailScrollerError.Other -> when (this.v1) {
        is ProtonError.Network -> PaginationError.Offline
        is ProtonError.NonProcessableActions -> PaginationError.NonProcessableActions
        is ProtonError.OtherReason,
        is ProtonError.ServerError,
        is ProtonError.Unexpected -> PaginationError.Other(this.v1.toDataError())
    }
    is MailScrollerError.Reason -> when (this.v1) {
        MailScrollerErrorReason.NOT_SYNCED -> PaginationError.PaginationDataNotSynced
    }
}
