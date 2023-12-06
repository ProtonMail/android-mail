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

package ch.protonmail.android.initializer.outbox

import ch.protonmail.android.mailmessage.data.usecase.DeleteSentMessagesFromOutbox
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxObserver @Inject constructor(
    private val scopeProvider: CoroutineScopeProvider,
    private val accountManager: AccountManager,
    private val messageRepository: MessageRepository,
    private val outboxRepository: OutboxRepository,
    private val deleteSentMessagesFromOutbox: DeleteSentMessagesFromOutbox
) {

    fun start() = accountManager.getPrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            // Observe the outbox items
            outboxRepository.observeAll(userId)
                .flatMapLatest { outboxMessages ->

                    // Observe the corresponding messages from the MessageRepository
                    messageRepository.observeCachedMessages(userId, outboxMessages)
                        .map { either ->
                            either.fold(
                                // Ignore local data error
                                {},
                                { messages ->
                                    deleteSentMessagesFromOutbox(userId, messages)
                                }
                            )
                        }
                }
        }
        .launchIn(scopeProvider.GlobalDefaultSupervisedScope)
}
