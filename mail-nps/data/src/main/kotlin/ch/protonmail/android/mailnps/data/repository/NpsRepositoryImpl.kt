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

package ch.protonmail.android.mailnps.data.repository

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailnps.domain.repository.NpsRepository
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSessionDismissNpsResult
import uniffi.mail_uniffi.MailUserSessionRecordNpsScoreResult
import javax.inject.Inject

class NpsRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : NpsRepository {

    override suspend fun recordScore(
        userId: UserId,
        score: NpsScore,
        comment: String?
    ): Either<DataError, Unit> = executeWithUserSession(userId) { session ->
        when (val result = session.getRustUserSession().recordNpsScore(score.value.toLong(), comment)) {
            is MailUserSessionRecordNpsScoreResult.Ok -> Unit.right()
            is MailUserSessionRecordNpsScoreResult.Error -> result.v1.toDataError().left()
        }
    }.flatten()

    override suspend fun dismiss(userId: UserId): Either<DataError, Unit> = executeWithUserSession(userId) { session ->
        when (val result = session.getRustUserSession().dismissNps()) {
            is MailUserSessionDismissNpsResult.Ok -> Unit.right()
            is MailUserSessionDismissNpsResult.Error -> result.v1.toDataError().left()
        }
    }.flatten()
}
