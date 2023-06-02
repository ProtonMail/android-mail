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

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.AdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import timber.log.Timber

@AssistedFactory
interface MailboxItemRemoteMediatorFactory {

    fun create(mailboxPageKey: MailboxPageKey, type: MailboxItemType): MailboxItemRemoteMediator
}

@OptIn(ExperimentalPagingApi::class)
class MailboxItemRemoteMediator @AssistedInject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val getAdjacentPageKeys: GetAdjacentPageKeys,
    @Assisted private val mailboxPageKey: MailboxPageKey,
    @Assisted private val type: MailboxItemType
) : RemoteMediator<MailboxPageKey, MailboxItem>() {

    override suspend fun initialize() = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<MailboxPageKey, MailboxItem>): MediatorResult {
        val userId = mailboxPageKey.userIds.first()

        if (loadType == LoadType.REFRESH) {
            Timber.d("Paging: fetchItems -> markAsStale")
            when (type) {
                MailboxItemType.Message -> messageRepository.markAsStale(userId, mailboxPageKey.pageKey.filter.labelId)
                MailboxItemType.Conversation -> conversationRepository.markAsStale(
                    userId, mailboxPageKey.pageKey.filter.labelId
                )
            }
        }

        Timber.d("Paging: fetchItems: $loadType")
        val prevKey = state.pages.firstOrNull { it.data.isNotEmpty() }?.prevKey?.pageKey
        val nextKey = state.pages.lastOrNull { it.data.isNotEmpty() }?.nextKey?.pageKey
        Timber.d("Paging: fetchItems: prevKey: $prevKey")
        Timber.d("Paging: fetchItems: nextKey: $nextKey")

        val pageKey = when (loadType) {
            LoadType.REFRESH -> mailboxPageKey.pageKey.copy(size = state.config.initialLoadSize)
            LoadType.PREPEND -> prevKey ?: getAdjacentPageKeys(state).prev
            LoadType.APPEND -> nextKey ?: getAdjacentPageKeys(state).next
        }.let { mailboxPageKey.copy(pageKey = it) }

        Timber.d("Paging: fetchItems -> ${pageKey.pageKey}")

        return when (type) {
            MailboxItemType.Message -> fetchMessages(userId, pageKey)
            MailboxItemType.Conversation -> fetchConversations(userId, pageKey)
        }
    }

    private fun getAdjacentPageKeys(state: PagingState<MailboxPageKey, MailboxItem>): AdjacentPageKeys {
        val items = state.pages.flatMap { it.data }
        return getAdjacentPageKeys(items, mailboxPageKey.pageKey, state.config.pageSize)
    }

    private suspend fun fetchMessages(userId: UserId, pageKey: MailboxPageKey): MediatorResult {
        return messageRepository.getRemoteMessages(
            userId = userId,
            pageKey = pageKey.pageKey
        ).fold(
            ifRight = {
                Timber.d("Paging: endOfPaginationReached: ${it.size}")
                MediatorResult.Success(endOfPaginationReached = it.isEmpty())
            },
            ifLeft = {
                Timber.d("Paging: fetchItems: Mediator failed to load messages: $it")
                MediatorResult.Error(Exception(it.toString()))
            }
        )
    }

    private suspend fun fetchConversations(userId: UserId, pageKey: MailboxPageKey): MediatorResult {
        return conversationRepository.getRemoteConversations(
            userId = userId,
            pageKey = pageKey.pageKey
        ).fold(
            ifRight = {
                MediatorResult.Success(endOfPaginationReached = it.isEmpty())
            },
            ifLeft = {
                Timber.d("Paging: Mediator failed to load conversations: $it")
                MediatorResult.Error(Exception(it.toString()))
            }
        )
    }
}
