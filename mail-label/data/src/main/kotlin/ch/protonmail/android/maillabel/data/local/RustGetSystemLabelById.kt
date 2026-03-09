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

package ch.protonmail.android.maillabel.data.local

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.ResolveSystemLabelByIdResult
import uniffi.mail_uniffi.resolveSystemLabelById
import javax.inject.Inject

class RustGetSystemLabelById @Inject constructor() {

    suspend operator fun invoke(
        mailUserSession: MailUserSessionWrapper,
        labelId: LocalLabelId
    ): Either<DataError, LocalSystemLabel> = either {
        when (val result = resolveSystemLabelById(mailUserSession.getRustUserSession(), labelId)) {
            is ResolveSystemLabelByIdResult.Error -> raise(result.v1.toDataError())
            is ResolveSystemLabelByIdResult.Ok -> result.v1 ?: raise(DataError.Local.NoDataCached)
        }
    }
}
