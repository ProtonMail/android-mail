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

package ch.protonmail.android.mailbugreport.data.local

import arrow.core.raise.either
import ch.protonmail.android.mailcommon.data.mapper.LocalIssueReport
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.VoidSessionResult
import uniffi.mail_uniffi.reportAnIssue
import javax.inject.Inject

class RustBugReportDataSourceImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : RustBugReportDataSource {

    override suspend fun submitBugReport(userId: UserId, report: LocalIssueReport) = with(ioDispatcher) {
        either {
            val session = userSessionRepository.getUserSession(userId)?.getRustUserSession()
                ?: raise(DataError.Local.NoUserSession)

            val issue = reportAnIssue(session, report)

            when (issue) {
                is VoidSessionResult.Error -> raise(issue.v1.toDataError())
                VoidSessionResult.Ok -> Unit
            }
        }
    }
}
