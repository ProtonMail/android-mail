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

package ch.protonmail.android.mailmailbox.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.AutoDeleteBanner
import uniffi.mail_uniffi.GetAutoDeleteBannerResult
import uniffi.mail_uniffi.getAutoDeleteBanner
import javax.inject.Inject

class RustGetAutoDeleteBanner @Inject constructor() {

    suspend operator fun invoke(
        mailSession: MailUserSessionWrapper,
        localLabelId: LocalLabelId
    ): Either<DataError, AutoDeleteBanner?> =
        when (val result = getAutoDeleteBanner(mailSession.getRustUserSession(), localLabelId)) {
            is GetAutoDeleteBannerResult.Error -> result.v1.toDataError().left()
            is GetAutoDeleteBannerResult.Ok -> result.v1.right()
        }
}
