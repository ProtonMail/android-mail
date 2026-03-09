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

package ch.protonmail.android.mailsession.domain.mapper

import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailsession.domain.model.EventLoopError
import uniffi.mail_uniffi.EventError
import uniffi.mail_uniffi.EventErrorReason

fun EventError.toEventLoopError(): EventLoopError = when (this) {
    is EventError.Other -> EventLoopError.Other(this.v1.toDataError())
    is EventError.Reason -> when (this.v1) {
        EventErrorReason.REFRESH -> EventLoopError.RefreshError
        EventErrorReason.SUBSCRIBER -> EventLoopError.SubscriberError
        EventErrorReason.CYCLIC_DEPENDENCY -> EventLoopError.CyclicDependencyError
    }
}
