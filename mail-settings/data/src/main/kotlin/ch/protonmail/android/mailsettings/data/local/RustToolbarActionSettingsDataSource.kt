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

package ch.protonmail.android.mailsettings.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMobileAction
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MobileActionsResult
import uniffi.mail_uniffi.VoidActionResult
import uniffi.mail_uniffi.getAllMobileConversationActions
import uniffi.mail_uniffi.getAllMobileListActions
import uniffi.mail_uniffi.getAllMobileMessageActions
import uniffi.mail_uniffi.getMobileConversationToolbarActions
import uniffi.mail_uniffi.getMobileListToolbarActions
import uniffi.mail_uniffi.getMobileMessageToolbarActions
import uniffi.mail_uniffi.updateMobileConversationToolbarActions
import uniffi.mail_uniffi.updateMobileListToolbarActions
import uniffi.mail_uniffi.updateMobileMessageToolbarActions
import javax.inject.Inject

class RustToolbarActionSettingsDataSource @Inject constructor(
    private val userSessionRepository: UserSessionRepository
) : ToolbarActionSettingsDataSource {

    override suspend fun getListActions(userId: UserId): Either<DataError, List<LocalMobileAction>> {
        return withValidUserSession(userId) { session ->
            when (val result = getMobileListToolbarActions(session)) {
                is MobileActionsResult.Error -> result.v1.toDataError().left()
                is MobileActionsResult.Ok -> result.v1.right()
            }
        }
    }

    override suspend fun getAllListActions(): List<LocalMobileAction> = getAllMobileListActions()

    override suspend fun updateListActions(userId: UserId, actions: List<LocalMobileAction>): Either<DataError, Unit> {
        return withValidUserSession(userId) { session ->
            when (val result = updateMobileListToolbarActions(session, actions)) {
                is VoidActionResult.Error -> result.v1.toDataError().left()
                VoidActionResult.Ok -> Unit.right()
            }
        }
    }

    override suspend fun getConversationActions(userId: UserId): Either<DataError, List<LocalMobileAction>> {
        return withValidUserSession(userId) { session ->
            when (val result = getMobileConversationToolbarActions(session)) {
                is MobileActionsResult.Error -> result.v1.toDataError().left()
                is MobileActionsResult.Ok -> result.v1.right()
            }
        }
    }

    override suspend fun getAllConversationActions(): List<LocalMobileAction> = getAllMobileConversationActions()

    override suspend fun updateConversationActions(
        userId: UserId,
        actions: List<LocalMobileAction>
    ): Either<DataError, Unit> {
        return withValidUserSession(userId) { session ->
            when (val result = updateMobileConversationToolbarActions(session, actions)) {
                is VoidActionResult.Error -> result.v1.toDataError().left()
                VoidActionResult.Ok -> Unit.right()
            }
        }
    }

    override suspend fun getMessageActions(userId: UserId): Either<DataError, List<LocalMobileAction>> {
        return withValidUserSession(userId) { session ->
            when (val result = getMobileMessageToolbarActions(session)) {
                is MobileActionsResult.Error -> result.v1.toDataError().left()
                is MobileActionsResult.Ok -> result.v1.right()
            }
        }
    }

    override suspend fun getAllMessageActions(): List<LocalMobileAction> = getAllMobileMessageActions()

    override suspend fun updateMessageActions(
        userId: UserId,
        actions: List<LocalMobileAction>
    ): Either<DataError, Unit> {
        return withValidUserSession(userId) { session ->
            when (val result = updateMobileMessageToolbarActions(session, actions)) {
                is VoidActionResult.Error -> result.v1.toDataError().left()
                VoidActionResult.Ok -> Unit.right()
            }
        }
    }

    private suspend fun <T> withValidUserSession(
        userId: UserId,
        block: suspend (MailUserSession) -> Either<DataError, T>
    ): Either<DataError, T> {
        val session = userSessionRepository.getUserSession(userId)
            ?: return DataError.Local.NoUserSession.left()
        return block(session.getRustUserSession())
    }
}
