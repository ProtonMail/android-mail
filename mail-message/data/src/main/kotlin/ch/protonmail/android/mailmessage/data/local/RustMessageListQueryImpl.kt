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
import arrow.core.right
import ch.protonmail.android.mailcategory.data.mapper.toCategoryViewStatus
import ch.protonmail.android.mailcategory.domain.model.CategoryViewStatus
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalCategoryLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
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
    private val categoryViewStatusFlow = MutableStateFlow<CategoryViewStatus?>(null)

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
                    Timber.d("rust-message-query: refreshing for pageKey: %s", pageKey)
                    reloadMessages()
                }
            }
        }
    }

    private suspend fun loadNextPage(): Either<PaginationError, List<Message>> {
        val deferred = setPendingRequest(RequestType.Append)
        paginatorState?.paginatorWrapper?.nextPage()
            ?.onLeft { completeRejectedRequest(deferred, RequestType.Append, it) }

        return deferred.await().let { firstResponse ->
            val followUp = paginatorState?.pendingRequest?.followUpResponse
            followUp?.awaitWithTimeout(NONE_FOLLOWUP_GRACE_MS, firstResponse) {
                Timber.d("rust-message-query: Follow-up response timed out.")
                clearPendingRequest()
            } ?: firstResponse
        }
    }

    /**
     * Serves a Paging refresh — "all the items loaded so far" — from [ScrollerCache], which mirrors the
     * scroller's list because both are built from the same stream of updates.
     *
     * Rust processes scroller calls serially, so getItems() can sit behind an in-flight fetch for
     * seconds (a switch to a category that is not synced yet is the worst case) and then answer with
     * the very list we are already holding. Paging keeps the previous location's items on screen for
     * that whole wait, so only ask Rust when we have nothing to serve.
     *
     * An empty cache is served too when the scroller has reported the location as empty: waiting on
     * getItems() to be told "still nothing" is the same stall for no gain. Only a cache that cannot
     * answer at all goes to Rust — see [ScrollerCache.mirrorsScroller].
     *
     * Note that we must not do both: a getItems() response no request is waiting for is treated as an
     * invalidation, which would refresh, serve the cache, call getItems() again, and never settle. So
     * when the cache reports it could not apply an update — the one case where it stops mirroring the
     * scroller — every refresh queries Rust again, as they all did before this cache was trusted, until a
     * full replacement puts the two back in step.
     */
    private suspend fun reloadMessages(): Either<PaginationError, List<Message>> {
        val cached = paginatorMutex.withLock { servableCachedItems() }
        if (cached != null) {
            Timber.d("rust-message-query: serving refresh from cache (%d items)", cached.size)
            return cached.right()
        }

        Timber.d("rust-message-query: cache cannot answer the refresh, calling reload()")
        val deferred = setPendingRequest(RequestType.Refresh)
        paginatorState?.paginatorWrapper?.reload()
            ?.onLeft { completeRejectedRequest(deferred, RequestType.Refresh, it) }
        return deferred.await()
    }

    override suspend fun getCursorFromActivePaginator(
        userId: UserId,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?,
        firstPage: LocalConversationId
    ): Either<PaginationError, MailMessageCursorWrapper>? {
        val pageDescriptor = PageDescriptor.Default(
            userId = userId,
            labelId = labelId, categoryLabelId = categoryLabelId
        )

        return paginatorMutex.withLock {
            val state = paginatorState
            when {
                state == null -> {
                    Timber.d("rust-message-query: No active paginator to reuse for cursor")
                    null
                }

                state.pageDescriptor != pageDescriptor -> {
                    Timber.d(
                        "rust-message-query: Active paginator cannot be reused for cursor. active=%s requested=%s",
                        state.pageDescriptor,
                        pageDescriptor
                    )
                    null
                }

                else -> {
                    Timber.d(
                        "rust-message-query: Reusing active mailbox paginator for cursor, scrollerId=%s",
                        state.paginatorWrapper.getScrollerId()
                    )
                    state.paginatorWrapper.getCursor(firstPage)
                }
            }
        }
    }

    override fun observeScrollerFetchNewStatus(): Flow<MessageScrollerStatusUpdate> =
        scrollerFetchNewStatusFlow.filterNotNull()

    override fun observeCategoryViewStatus(): Flow<CategoryViewStatus> = categoryViewStatusFlow.filterNotNull()

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

    override suspend fun setActiveCategoryLabel(categoryLabelId: LocalCategoryLabelId): Either<PaginationError, Unit> =
        paginatorState?.paginatorWrapper?.changeCategoryView(categoryLabelId) ?: run {
            Timber.w("rust-message-query: No paginator to change category view")
            PaginationError.Other(DataError.Local.IllegalStateError).left()
        }

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
                enabledCategoryId = pageDescriptor.categoryLabelId?.toLocalCategoryLabelId(),
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

            // Get initial category view status
            categoryViewStatusFlow.value = wrapper.getCategoryViewStatus()
            Timber.d("rust-message-query: Initial category view state: %s", categoryViewStatusFlow.value)

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
                            is MessageScrollerUpdate.CategoryViewChanged -> {
                                Timber.d(
                                    "rust-message-query: Category view update received: " +
                                        "${update.categoryView.available}"
                                )
                                categoryViewStatusFlow.value = update.categoryView.toCategoryViewStatus()
                                return@withLock
                            }
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

    /**
     * Completes a request that Rust rejected on the call itself, with the same fallback the update
     * handler applies to an unexpected first response: the cache snapshot for a Refresh, no items for
     * an Append.
     *
     * A rejected call gets no update callback, so leaving the request pending blocks the Paging load
     * until an unrelated update happens to resolve it — and Paging keeps the previous list on screen
     * for the whole wait. It is exactly what stalls a switch to a category that is not synced yet.
     */
    private suspend fun completeRejectedRequest(
        deferred: CompletableDeferred<Either<PaginationError, List<Message>>>,
        type: RequestType,
        error: PaginationError
    ) {
        Timber.w("rust-message-query: %s call rejected by Rust: %s", type, error)
        paginatorMutex.withLock {
            if (paginatorState?.pendingRequest?.response === deferred) {
                paginatorState = paginatorState?.copy(pendingRequest = null)
            }
            deferred.complete(fallbackResponse(type).right())
        }
    }

    private suspend fun setPendingRequest(
        type: RequestType
    ): CompletableDeferred<Either<PaginationError, List<Message>>> {
        paginatorMutex.withLock {
            // Paging can start a load while a previous one is still waiting (several invalidations in a
            // row do exactly that). Only one pending request is tracked, so complete the one being
            // replaced rather than orphaning its deferred and leaving that Paging load in flight forever.
            paginatorState?.pendingRequest?.let { outgoing ->
                if (!outgoing.isCompleted()) {
                    Timber.w("rust-message-query: replacing a pending %s request", outgoing.type)
                    outgoing.response.complete(fallbackResponse(outgoing.type).right())
                    outgoing.followUpResponse?.complete(emptyList<Message>().right())
                }
            }

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

    // The items a refresh can be served with, or null when the cache cannot answer it: nothing has been
    // loaded yet, an update was dropped, or the list holds a duplicate. An empty list is an answer — the
    // scroller reported the location as empty. Callers must hold [paginatorMutex].
    private fun servableCachedItems(): List<Message>? {
        val snapshot = paginatorState?.scrollerCache?.takeIf { it.mirrorsScroller }?.snapshot ?: return null

        // An empty list is not an answer, because the scroller empties this cache on its way *into* a load:
        // reset() clears the list and reports FIRST_PAGE_LOADING_START before it fetches anything. Serving
        // that emptiness settles the Paging refresh at the start of the load with nothing in it, so the
        // mailbox reads as "No messages" for the whole fetch. Going to Rust leaves the refresh in flight,
        // which is what the loading skeleton keys off. See ET-6647.
        if (snapshot.isEmpty()) return null

        // Messages are unique per id, so a repeat means the incremental updates left one behind (see
        // rememberDuplicateTolerantMailboxKeys). Rust's list is authoritative and replaces the cache when
        // it arrives, so refresh from it rather than serving the duplicate back for as long as it survives.
        val duplicates = snapshot.size - snapshot.distinctBy { it.id }.size
        if (duplicates > 0) {
            Timber.e("rust-message-query: cache holds %d duplicate item(s), asking Rust instead", duplicates)
            return null
        }

        return snapshot
    }

    // What a request resolves to when no Rust response is coming: everything we hold for a Refresh
    // (it asks for the loaded items), nothing for an Append (it asks for items beyond them).
    // Mirrors ScrollerOnUpdateHandler's fallback. Callers must hold [paginatorMutex].
    private fun cachedItems(): List<Message> = paginatorState?.scrollerCache?.snapshot ?: emptyList()

    private fun fallbackResponse(type: RequestType): List<Message> = when (type) {
        // Deduplicated as well: Paging should never be handed a repeated id, whichever path answers.
        RequestType.Refresh -> cachedItems().distinctBy { it.id }
        RequestType.Append -> emptyList()
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

        data class Default(
            override val userId: UserId,
            val labelId: LabelId,
            val categoryLabelId: CategoryLabelId?
        ) : PageDescriptor

        data class Search(override val userId: UserId, val keyword: String) : PageDescriptor
    }

    private fun PageKey.toPageDescriptor(userId: UserId): PageDescriptor = when (this) {
        is PageKey.DefaultPageKey -> PageDescriptor.Default(
            userId = userId,
            labelId = this.labelId,
            categoryLabelId = this.categoryLabelId
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
    is MessageScrollerUpdate.CategoryViewChanged -> "CategoryViewChanged"
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
    MessageScrollerStatusUpdate.FIRST_PAGE_LOADING_START -> "FIRST_PAGE_LOADING_START"
    MessageScrollerStatusUpdate.FIRST_PAGE_LOADING_END -> "FIRST_PAGE_LOADING_END"
}
