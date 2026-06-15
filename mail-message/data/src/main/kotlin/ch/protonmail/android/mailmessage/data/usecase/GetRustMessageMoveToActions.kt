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

package ch.protonmail.android.mailmessage.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import uniffi.mail_uniffi.AvailableMoveToDestinationsForMessagesResult
import uniffi.mail_uniffi.MoveDestination
import uniffi.mail_uniffi.SystemFolderDestination
import uniffi.mail_uniffi.availableMoveToDestinationsForMessages
import javax.inject.Inject

class GetRustMessageMoveToActions @Inject constructor() {

    suspend operator fun invoke(
        mailbox: MailboxWrapper,
        messageIds: List<LocalMessageId>
    ): Either<DataError, List<MoveDestination>> =
        when (val result = availableMoveToDestinationsForMessages(mailbox.getRustMailbox(), messageIds)) {
            is AvailableMoveToDestinationsForMessagesResult.Error -> result.v1.toDataError().left()
            is AvailableMoveToDestinationsForMessagesResult.Ok ->
                result.v1.map { it.asSystemFolderOrSelf() }.right()
        }

    /**
     * Hotfix: the downstream move-to flow only keeps SystemFolder destinations and
     * discards the Inbox location. Convert the Rust Inbox destination into the equivalent
     * SystemFolder
     */
    private fun MoveDestination.asSystemFolderOrSelf(): MoveDestination = when (this) {
        is MoveDestination.Inbox -> MoveDestination.SystemFolder(SystemFolderDestination(v1.localId, v1.name))
        else -> this
    }
}
