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

package ch.protonmail.android.mailconversation.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import uniffi.mail_uniffi.AllAvailableListActionsForConversationsResult
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.allAvailableListActionsForConversations
import javax.inject.Inject

class GetRustConversationListBottomBarActions @Inject constructor() {

    suspend operator fun invoke(
        mailbox: MailboxWrapper,
        messageIds: List<LocalConversationId>
    ): Either<DataError, AllListActions> =
        when (val result = allAvailableListActionsForConversations(mailbox.getRustMailbox(), messageIds)) {
            is AllAvailableListActionsForConversationsResult.Error -> result.v1.toDataError().left()
            is AllAvailableListActionsForConversationsResult.Ok -> result.v1.right()
        }
}
