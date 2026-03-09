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
import ch.protonmail.android.mailmessage.data.wrapper.DecryptedMessageWrapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import uniffi.mail_uniffi.GetMessageBodyResult
import uniffi.mail_uniffi.getMessageBody
import javax.inject.Inject

class CreateRustMessageBodyAccessor @Inject constructor() {

    private var cache: Pair<LocalMessageId, DecryptedMessageWrapper>? = null
    private val mutex = Mutex()

    suspend operator fun invoke(
        mailbox: MailboxWrapper,
        messageId: LocalMessageId
    ): Either<DataError, DecryptedMessageWrapper> = mutex.withLock {
        cache?.let {
            if (messageId == it.first) {
                Timber.d("RustMessage: cache hit, returning Decrypted Message Body...")
                return it.second.right()
            }
        }

        return when (val result = getMessageBody(mailbox.getRustMailbox(), messageId)) {
            is GetMessageBodyResult.Error -> result.v1.toDataError().left()
            is GetMessageBodyResult.Ok -> {
                val decryptedMessageWrapper = DecryptedMessageWrapper(result.v1)
                cache = Pair(messageId, decryptedMessageWrapper)
                decryptedMessageWrapper.right()
            }
        }
    }
}
