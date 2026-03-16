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
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.MessageRustCoroutineScope
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessagesPaginator
import ch.protonmail.android.mailmessage.data.usecase.CreateRustSearchPaginator
import ch.protonmail.android.mailmessage.data.util.awaitWithTimeout
import ch.protonmail.android.mailmessage.data.wrapper.MailMessageCursorWrapper
import ch.protonmail.android.mailmessage.data.wrapper.MessagePaginatorWrapper
import ch.protonmail.android.mailpagination.data.model.scroller.PendingRequest
import ch.protonmail.android.mailpagination.data.model.scroller.RequestType
import ch.protonmail.android.mailpagination.data.model.scroller.isCompleted
import ch.protonmail.android.mailpagination.data.scroller.ScrollerCache
import ch.protonmail.android.mailpagination.data.scroller.ScrollerOnUpdateHandler
import ch.protonmail.android.mailpagination.data.scroller.ScrollerUpdate
import ch.protonmail.android.mailpagination.data.scroller.itemCount
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PageToLoad
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.mailpagination.domain.repository.PageInvalidationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.MessageScrollerListUpdate
import uniffi.mail_uniffi.MessageScrollerLiveQueryCallback
import uniffi.mail_uniffi.MessageScrollerStatusUpdate
import uniffi.mail_uniffi.MessageScrollerUpdate
import javax.inject.Inject

class RustMessageListQueryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val createRustMessagesPaginator: CreateRustMessagesPaginator,
    private val createRustSearchPaginator: CreateRustSearchPaginator,
    @MessageRustCoroutineScope private val coroutineScope: CoroutineScope,
    private val invalidationRepository: PageInvalidationRepository
) : RustMessageListQuery {

    private var paginatorState: PaginatorState? = null
    private val paginatorMutex = Mutex()

    private val scrollerFetchNewStatusFlow = MutableStateFlow<MessageScrollerStatusUpdate?>(null)

    override suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<PaginationError, List<Message>> {

        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message-query: trying to load messages with a null mailbox")
            return PaginationError.Other(DataError.Local.IllegalStateError).left()
        }

        val pageDescriptor = pageKey.toPageDescriptor(userId)

        val newPaginatorInitialized = paginatorMutex.withLock {
            if (shouldInitPaginator(pageDescriptor, pageKey)) {
                initPaginator(pageDescriptor, mailbox)
                true
            } else {
                false
            }
        }

        Timber.d("rust-message-query: Paging: querying ${pageKey.pageToLoad.name} page for messages")

        return when (pageKey.pageToLoad) {
            PageToLoad.First,
            PageToLoad.Next -> loadNextPage()

            PageToLoad.All -> {
                if (newPaginatorInitialized) {
                    Timber.d(
                        "rust-message-query: paginator newly created, calling nextPage() " +
                            "instead of reload() for pageKey: %s",
                        pageKey
                    )
                    loadNextPage()
                } else {
                    Timber.d("rust-message-query: calling reload() for pageKey: %s", pageKey)
                    reloadMessages()
                }
            }
        }
    }

    private suspend fun loadNextPage(): Either<PaginationError, List<Message>> {
        val deferred = setPendingRequest(RequestType.Append)
        paginatorState?.paginatorWrapper?.nextPage()

        return deferred.await().let { firstResponse ->
            val followUp = paginatorState?.pendingRequest?.followUpResponse
            followUp?.awaitWithTimeout(NONE_FOLLOWUP_GRACE_MS, firstResponse) {
                Timber.d("rust-message-query: Follow-up response timed out.")
                clearPendingRequest()
            } ?: firstResponse
        }
    }

    private suspend fun reloadMessages(): Either<PaginationError, List<Message>> {
        val deferred = setPendingRequest(RequestType.Refresh)
        paginatorState?.paginatorWrapper?.reload()
        return deferred.await()
    }

    override suspend fun getCursor(
        userId: UserId,
        labelId: LabelId,
        conversationId: LocalConversationId
    ): Either<PaginationError, MailMessageCursorWrapper>? {
        val pageDescriptor = PageDescriptor.Default(userId = userId, labelId = labelId)

        val initError = paginatorMutex.withLock {
            if (shouldInitPaginatorForCursor(pageDescriptor)) {
                Timber.d(
                    "rust-message-query: Initializing paginator for getCursor with page desc=%s",
                    pageDescriptor
                )
                val mailbox = rustMailboxFactory.create(userId, labelId.toLocalLabelId()).getOrNull()
                if (mailbox == null) {
                    Timber.e(
                        "rust-message-query: Unable to create mailbox for userId=%s and labelId=%s",
                        userId,
                        labelId
                    )
                    PaginationError.Other(DataError.Local.IllegalStateError).left()
                } else {
                    initPaginator(pageDescriptor, mailbox)
                    null
                }
            } else {
                Timber.d(
                    "rust-message-query: Reusing existing paginator for getCursor with page desc=%s",
                    pageDescriptor
                )
                null
            }
        }

        if (initError != null) return initError

        return paginatorState?.paginatorWrapper?.getCursor(conversationId)
    }

    override fun observeScrollerFetchNewStatus(): Flow<MessageScrollerStatusUpdate> =
        scrollerFetchNewStatusFlow.filterNotNull()

    override suspend fun terminatePaginator(userId: UserId) {
        if (paginatorState?.pageDescriptor?.userId == userId) {
            paginatorMutex.withLock {
                destroy()
            }
        } else {
            Timber.d("rust-message-query: Not terminating paginator, userId does not match")
        }
    }

    override suspend fun supportsIncludeFilter() = paginatorState?.paginatorWrapper?.supportsIncludeFilter() == true

    override suspend fun updateUnreadFilter(filterUnread: Boolean) {
        paginatorState?.paginatorWrapper?.filterUnread(filterUnread)
            ?: Timber.w("rust-message-query: No paginator to update unread filter")
    }

    override suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean) {
        paginatorState?.paginatorWrapper?.showSpamAndTrash(showSpamTrash)
            ?: Timber.w("rust-message-query: No paginator to update show spam/trash filter")
    }

    private suspend fun initPaginator(pageDescriptor: PageDescriptor, mailbox: MailboxWrapper) {
        Timber.d("rust-message-query: [destroy and] initialize paginator instance...")
        destroy()

        val scrollerOnUpdateHandler = ScrollerOnUpdateHandler<Message>(
            tag = "rust-message-query",
            invalidate = { invalidateLoadedItems() }
        )


        when (pageDescriptor) {
            is PageDescriptor.Default -> createRustMessagesPaginator(
                mailbox = mailbox,
                callback = messagesUpdatedCallback(scrollerOnUpdateHandler)
            )

            is PageDescriptor.Search -> createRustSearchPaginator(
                mailbox = mailbox,
                keyword = pageDescriptor.keyword,
                callback = messagesUpdatedCallback(scrollerOnUpdateHandler)
            )
        }.onRight { wrapper ->
            Timber.d("rust-message-query: Paginator instance created, id=${wrapper.getScrollerId()}")
            paginatorState = PaginatorState(
                paginatorWrapper = wrapper,
                pageDescriptor = pageDescriptor,
                scrollerCache = ScrollerCache()
            )
        }
    }

    private fun messagesUpdatedCallback(onUpdateHandler: ScrollerOnUpdateHandler<Message>) =
        object : MessageScrollerLiveQueryCallback {
            override fun onUpdate(update: MessageScrollerUpdate) {
                coroutineScope.launch {
                    paginatorMutex.withLock {
                        val scrollerUpdate = when (update) {
                            is MessageScrollerUpdate.Status -> {
                                Timber.d("rust-message-query: Scroller fetch new status update: ${update.v1}")
                                scrollerFetchNewStatusFlow.value = update.v1
                                return@withLock
                            }

                            is MessageScrollerUpdate.List -> update.toScrollerUpdate()

                            is MessageScrollerUpdate.Error -> update.toScrollerUpdate()
                        }

                        Timber.d(
                            "rust-message-query: Received paginator update: %s with %d items, " +
                                "current cache: %d scrollerId=%s",
                            update.debugTypeName(),
                            scrollerUpdate.itemCount(),
                            paginatorState?.scrollerCache?.itemCount() ?: 0,
                            scrollerUpdate.scrollerId
                        )

                        val snapshot = paginatorState?.scrollerCache?.applyUpdate(scrollerUpdate) ?: emptyList()
                        val pending = paginatorState?.pendingRequest

                        onUpdateHandler.handleUpdate(pending, scrollerUpdate, snapshot) {
                            // We need to wait for the follow-up response
                            if (pending?.type == RequestType.Append) {
                                Timber.d("rust-message-query: Triggering follow-up after immediate Append None")
                                paginatorState = paginatorState?.withFollowUpResponse()
                            }
                        }

                        if (paginatorState?.pendingRequest == null) {
                            Timber.d("rust-message-query: No pending request")
                        } else if (paginatorState?.pendingRequest?.isCompleted() == true) {
                            Timber.d("rust-message-query: Clearing completed pending request")
                            paginatorState = paginatorState?.copy(pendingRequest = null)
                        } else {
                            Timber.d("rust-message-query: Keeping pending request, waiting for more data")
                        }
                    }
                }
            }
        }

    private fun shouldInitPaginator(pageDescriptor: PageDescriptor, pageKey: PageKey) = paginatorState == null ||
        paginatorState?.pageDescriptor != pageDescriptor ||
        pageKey.pageToLoad == PageToLoad.First

    private fun shouldInitPaginatorForCursor(pageDescriptor: PageDescriptor): Boolean =
        paginatorState == null || paginatorState?.pageDescriptor != pageDescriptor

    private fun destroy() {
        if (paginatorState == null) {
            Timber.d("rust-message-query: no paginator to destroy")
        } else {
            Timber.d(
                "rust-message-query: disconnecting and destroying paginator with id=%s",
                paginatorState?.paginatorWrapper?.getScrollerId()
            )
            paginatorState?.paginatorWrapper?.disconnect()
            paginatorState = null
        }
    }

    private fun invalidateLoadedItems() {
        coroutineScope.launch {
            invalidationRepository.submit(PageInvalidationEvent.MessagesInvalidated())
        }
    }

    private suspend fun setPendingRequest(
        type: RequestType
    ): CompletableDeferred<Either<PaginationError, List<Message>>> {
        paginatorMutex.withLock {
            val deferred = CompletableDeferred<Either<PaginationError, List<Message>>>()
            paginatorState = paginatorState?.copy(
                pendingRequest = PendingRequest(
                    type = type,
                    response = deferred
                )
            )
            return deferred
        }
    }

    private fun clearPendingRequest() {
        coroutineScope.launch {
            paginatorMutex.withLock {
                paginatorState = paginatorState?.copy(pendingRequest = null)
                Timber.d("rust-message-query: Cleared pending request")
            }
        }
    }

    private data class PaginatorState(
        val paginatorWrapper: MessagePaginatorWrapper,
        val pageDescriptor: PageDescriptor,
        val scrollerCache: ScrollerCache<Message>,
        val pendingRequest: PendingRequest<Message>? = null
    )

    private fun PaginatorState.withFollowUpResponse(): PaginatorState {
        val currentPending = this.pendingRequest
            ?: return this

        val newPending = currentPending.copy(followUpResponse = CompletableDeferred())

        return this.copy(pendingRequest = newPending)
    }

    private sealed interface PageDescriptor {

        val userId: UserId

        data class Default(override val userId: UserId, val labelId: LabelId) : PageDescriptor

        data class Search(override val userId: UserId, val keyword: String) : PageDescriptor
    }

    private fun PageKey.toPageDescriptor(userId: UserId): PageDescriptor = when (this) {
        is PageKey.DefaultPageKey -> PageDescriptor.Default(
            userId = userId,
            labelId = this.labelId
        )

        is PageKey.PageKeyForSearch -> PageDescriptor.Search(
            userId = userId,
            keyword = keyword
        )
    }

    companion object {

        const val NONE_FOLLOWUP_GRACE_MS = 250L
    }
}

fun MessageScrollerUpdate.List.toScrollerUpdate(): ScrollerUpdate<Message> = when (val listResult = this.v1) {
    is MessageScrollerListUpdate.Append -> ScrollerUpdate.Append(
        scrollerId = listResult.scrollerId,
        items = listResult.items
    )

    is MessageScrollerListUpdate.ReplaceFrom -> ScrollerUpdate.ReplaceFrom(
        scrollerId = listResult.scrollerId,
        idx = listResult.idx.toInt(),
        items = listResult.items
    )

    is MessageScrollerListUpdate.ReplaceBefore -> ScrollerUpdate.ReplaceBefore(
        scrollerId = listResult.scrollerId,
        idx = listResult.idx.toInt(),
        items = listResult.items
    )

    is MessageScrollerListUpdate.ReplaceRange -> ScrollerUpdate.ReplaceRange(
        scrollerId = listResult.scrollerId,
        fromIdx = listResult.from.toInt(),
        toIdx = listResult.to.toInt(),
        items = listResult.items
    )

    is MessageScrollerListUpdate.None -> ScrollerUpdate.None(
        scrollerId = listResult.scrollerId
    )
}

fun MessageScrollerUpdate.Error.toScrollerUpdate(): ScrollerUpdate<Message> = ScrollerUpdate.Error(
    error = this.error
)

fun MessageScrollerUpdate.debugTypeName(): String = when (this) {
    is MessageScrollerUpdate.List -> this.v1.debugTypeName()
    is MessageScrollerUpdate.Status -> this.v1.debugTypeName()
    is MessageScrollerUpdate.Error -> "Error"
}

fun MessageScrollerListUpdate.debugTypeName(): String = when (this) {
    is MessageScrollerListUpdate.None -> "None"
    is MessageScrollerListUpdate.Append -> "Append"
    is MessageScrollerListUpdate.ReplaceFrom -> "ReplaceFrom"
    is MessageScrollerListUpdate.ReplaceBefore -> "ReplaceBefore"
    is MessageScrollerListUpdate.ReplaceRange -> "ReplaceRange"
}

fun MessageScrollerStatusUpdate.debugTypeName(): String = when (this) {
    MessageScrollerStatusUpdate.FETCH_NEW_START -> "FETCH_NEW_START"
    MessageScrollerStatusUpdate.FETCH_NEW_END -> "FETCH_NEW_END"
}
