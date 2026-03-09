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

package ch.protonmail.android.mailconversation.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailconversation.data.ConversationRustCoroutineScope
import ch.protonmail.android.mailconversation.data.mapper.toOrigin
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationWatcher
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import ch.protonmail.android.mailmessage.data.model.LocalConversationWithMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.WatchedConversation
import javax.inject.Inject

@SuppressWarnings("MagicNumber")
class RustConversationDetailQueryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val createRustConversationWatcher: CreateRustConversationWatcher,
    private val getRustConversation: GetRustConversation,
    @ConversationRustCoroutineScope private val coroutineScope: CoroutineScope
) : RustConversationDetailQuery {

    private var conversationWatcher: WatchedConversation? = null
    private var currentConversationId: LocalConversationId? = null
    private var currentUserId: UserId? = null
    private var currentLabelId: LocalLabelId? = null
    private var currentShowAllMessages: Boolean = false

    private val mutex = Mutex()

    private val conversationWithMessagesMutableFlow =
        MutableStateFlow<Either<ConversationError, LocalConversationWithMessages>?>(null)
    private val conversationWithMessagesFlow = conversationWithMessagesMutableFlow
        .asStateFlow()
        .filterNotNull()

    override suspend fun observeConversationWithMessages(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, LocalConversationWithMessages>> = callbackFlow {
        initialiseOrUpdateWatcher(userId, conversationId, labelId, entryPoint, showAllMessages)

        val job = launch {
            conversationWithMessagesFlow.collect { value ->
                send(value)
            }
        }

        awaitClose {
            job.cancel()

            // runBlocking here is fine, it's run on a non-main thread.
            runBlocking {
                mutex.withLock {
                    if (currentConversationId == conversationId) {
                        Timber.d("conversation called destroy on $conversationId")
                        destroy()
                    }
                }
            }
        }
    }

    @Suppress("ComplexCondition")
    private suspend fun initialiseOrUpdateWatcher(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ) {
        mutex.withLock {
            if (
                currentConversationId != conversationId || conversationWatcher == null ||
                userId != currentUserId || showAllMessages != currentShowAllMessages
            ) {
                // If the conversationId is different or there's no active watcher, destroy and create a new one
                destroy()

                val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
                if (mailbox == null) {
                    Timber.e("Failed to observe conversation, null mailbox")
                    return@withLock
                }

                currentUserId = userId
                currentLabelId = labelId
                currentShowAllMessages = showAllMessages
                val conversationEither = createRustConversationWatcher(
                    mailbox, conversationId, conversationUpdatedCallback(), entryPoint.toOrigin(), showAllMessages
                ).onLeft {
                    Timber.w("Failed to create watcher for conversation: $it")
                }.onRight {
                    conversationWatcher = it
                }

                conversationWithMessagesMutableFlow.value = conversationEither.map {
                    LocalConversationWithMessages(
                        conversation = it.conversation,
                        messages = LocalConversationMessages(it.focusedMessageId, it.messages)
                    )
                }
                currentConversationId = conversationId
            }
        }
    }

    private fun conversationUpdatedCallback() = object : LiveQueryCallback {
        override fun onUpdate() {
            coroutineScope.launch {
                mutex.withLock {
                    val userId = currentUserId ?: run {
                        Timber.w("Failed to update convo, no user id!")
                        return@withLock
                    }
                    val labelId = currentLabelId ?: run {
                        Timber.w("Failed to update convo, no label id!")
                        return@withLock
                    }
                    val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()

                    if (mailbox == null) {
                        Timber.w("Failed to update conversation messages, null mailbox")
                        return@withLock
                    }
                    if (currentConversationId == null) {
                        Timber.w("Failed to update conversation messages, null conversationId")
                        return@withLock
                    }

                    val conversationEither = getRustConversation(
                        mailbox, currentConversationId!!, currentShowAllMessages
                    ).onLeft {
                        Timber.w("Failed to update conversation messages, $it")
                    }

                    conversationWithMessagesMutableFlow.value = conversationEither.map {
                        LocalConversationWithMessages(
                            conversation = it.conversation,
                            messages = LocalConversationMessages(it.focusedMessageId, it.messages)
                        )
                    }

                    Timber.d("Conversation updated: $currentConversationId")
                }
            }
        }
    }

    private fun destroy() {
        Timber.d("destroy watcher for conversation $currentConversationId")

        conversationWatcher?.handle?.disconnect()

        conversationWatcher = null
        currentConversationId = null
        conversationWithMessagesMutableFlow.value = null
    }
}
