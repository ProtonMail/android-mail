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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.MessageRustCoroutineScope
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessageAccessor
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessageWatcher
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import uniffi.proton_mail_uniffi.LiveQueryCallback
import uniffi.proton_mail_uniffi.Message
import uniffi.proton_mail_uniffi.WatchedMessage
import javax.inject.Inject

class RustMessageQuery @Inject constructor(
    private val createRustMessageWatcher: CreateRustMessageWatcher,
    private val createRustMessageAccessor: CreateRustMessageAccessor,
    @MessageRustCoroutineScope private val coroutineScope: CoroutineScope
) {

    private var messageWatcher: WatchedMessage? = null
    private var currentUserSession: MailUserSessionWrapper? = null
    private var currentMessageId: LocalMessageId? = null
    private val mutex = Mutex()
    private val messageMutableStatusFlow = MutableStateFlow<Either<DataError, LocalMessageMetadata>?>(null)
    private val messageStatusFlow = messageMutableStatusFlow
        .asStateFlow()
        .filterNotNull()

    private val messageUpdatedCallback = object : LiveQueryCallback {

        override fun onUpdate() {
            coroutineScope.launch {
                mutex.withLock {
                    val messageId = currentMessageId ?: run {
                        Timber.w("Failed to update message, no message id!")
                        return@withLock
                    }
                    val session = currentUserSession ?: run {
                        Timber.w("Failed to update message, no session!")
                        return@withLock
                    }

                    messageMutableStatusFlow.value = createRustMessageAccessor(session, messageId)
                }
            }
        }
    }

    suspend fun observeMessage(
        session: MailUserSessionWrapper,
        messageId: LocalMessageId
    ): Flow<Either<DataError, Message>> {
        initialiseOrUpdateWatcher(session, messageId)

        return messageStatusFlow
    }

    private suspend fun initialiseOrUpdateWatcher(session: MailUserSessionWrapper, messageId: LocalMessageId) {
        mutex.withLock {
            if (currentMessageId != messageId || messageWatcher == null) {
                // If the messageId is different or there's no active watcher, destroy and create a new one
                destroy()

                currentUserSession = session
                currentMessageId = messageId
                val messageWatcherEither = createRustMessageWatcher(
                    session, messageId, messageUpdatedCallback
                ).onLeft {
                    Timber.w("Failed to observe message: $it")
                }.onRight {
                    messageWatcher = it
                }

                messageMutableStatusFlow.value = messageWatcherEither.map { it.message }
            }
        }
    }

    private fun destroy() {
        Timber.d("destroy watcher for $currentMessageId")
        messageWatcher?.handle?.destroy()
        messageWatcher = null
        currentMessageId = null
        messageMutableStatusFlow.value = null
    }

}

