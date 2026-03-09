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

package me.proton.android.core.auth.presentation.secondfactor

import me.proton.android.core.auth.presentation.LogTag
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAccountResult
import uniffi.mail_uniffi.MailSessionGetAccountSessionsResult
import uniffi.mail_uniffi.StoredAccount
import uniffi.mail_uniffi.StoredSession

suspend fun MailSession.getSessionsForAccount(account: StoredAccount?): List<StoredSession>? {
    if (account == null) {
        return null
    }

    return when (val result = getAccountSessions(account)) {
        is MailSessionGetAccountSessionsResult.Error -> {
            CoreLogger.e(LogTag.LOGIN, result.v1.toString())
            null
        }

        is MailSessionGetAccountSessionsResult.Ok -> result.v1
    }
}

suspend fun MailSession.getAccountById(userId: String): StoredAccount? {
    return when (val result = getAccount(userId)) {
        is MailSessionGetAccountResult.Error -> {
            CoreLogger.e(LogTag.LOGIN, result.v1.toString())
            null
        }

        is MailSessionGetAccountResult.Ok -> result.v1
    }
}
