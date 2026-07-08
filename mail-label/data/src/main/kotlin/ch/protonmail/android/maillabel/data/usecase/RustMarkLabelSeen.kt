/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.maillabel.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.VoidActionResult
import uniffi.mail_uniffi.markLabelSeen
import javax.inject.Inject

class RustMarkLabelSeen @Inject constructor() {

    suspend operator fun invoke(
        mailUserSession: MailUserSessionWrapper,
        localId: LocalCategoryLabelId
    ): Either<DataError, Unit> {
        return when (val result = markLabelSeen(mailUserSession.getRustUserSession(), localId)) {
            is VoidActionResult.Error -> result.v1.toDataError().left()
            VoidActionResult.Ok -> Unit.right()
        }
    }
}
