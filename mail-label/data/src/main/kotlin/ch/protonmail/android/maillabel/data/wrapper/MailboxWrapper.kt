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

package ch.protonmail.android.maillabel.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalViewMode
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.Mailbox
import uniffi.mail_uniffi.MailboxUnreadCountResult
import uniffi.mail_uniffi.MailboxWatchUnreadCountResult
import uniffi.mail_uniffi.UnreadLiveQueryCallback
import uniffi.mail_uniffi.WatchHandle

/**
 * Wraps rust Mailbox object, which is used to keep track of the currently selected label
 */
class MailboxWrapper(private val rustMailbox: Mailbox) {

    fun getRustMailbox() = rustMailbox

    fun labelId(): LocalLabelId = rustMailbox.labelId()

    fun viewMode(): LocalViewMode = rustMailbox.viewMode()

    suspend fun unreadCount(): Either<DataError, ULong> = when (val result = rustMailbox.unreadCount()) {
        is MailboxUnreadCountResult.Error -> result.v1.toDataError().left()
        is MailboxUnreadCountResult.Ok -> result.v1.right()
    }

    suspend fun watchUnreadCount(
        callback: UnreadLiveQueryCallback,
        category: LocalCategoryLabelId?
    ): Either<DataError, WatchHandle> = when (val result = rustMailbox.watchUnreadCount(callback, category)) {
        is MailboxWatchUnreadCountResult.Error -> result.v1.toDataError().left()
        is MailboxWatchUnreadCountResult.Ok -> result.v1.right()
    }

    fun destroy() {
        rustMailbox.destroy()
    }
}
