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

package ch.protonmail.android.mailsession.data.usecase

import arrow.core.raise.either
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import uniffi.mail_uniffi.MailSessionSignOutAllResult
import javax.inject.Inject

class SignOutAllAccounts @Inject constructor(
    private val mailSessionRepository: MailSessionRepository
) {

    suspend operator fun invoke() = either {
        val mailSession = mailSessionRepository.getMailSession().getRustMailSession()
        when (val result = mailSession.signOutAll()) {
            is MailSessionSignOutAllResult.Error -> raise(result.v1.toDataError())
            is MailSessionSignOutAllResult.Ok -> Unit
        }
    }
}
