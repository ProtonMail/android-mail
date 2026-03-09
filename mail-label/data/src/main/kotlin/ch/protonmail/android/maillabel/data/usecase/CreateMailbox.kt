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

package ch.protonmail.android.maillabel.data.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.NewMailboxResult
import uniffi.mail_uniffi.newMailbox
import javax.inject.Inject

class CreateMailbox @Inject constructor() {

    operator fun invoke(mailUserSession: MailUserSessionWrapper, labelId: LocalLabelId) =
        when (val result = newMailbox(mailUserSession.getRustUserSession(), labelId)) {
            is NewMailboxResult.Error -> result.v1.toDataError().left()
            is NewMailboxResult.Ok -> MailboxWrapper(result.v1).right()
        }
}
