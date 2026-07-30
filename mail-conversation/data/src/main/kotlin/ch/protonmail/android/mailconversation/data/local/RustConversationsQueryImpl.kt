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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcategory.data.mapper.toCategoryViewStatus
import ch.protonmail.android.mailcategory.domain.model.CategoryViewStatus
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.ConversationQueryCoroutineScope
import ch.protonmail.android.mailconversation.data.model.PageDescriptor
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationPaginator
import ch.protonmail.android.mailconversation.data.wrapper.ConversationCursorWrapper
import ch.protonmail.android.mailconversation.data.wrapper.ConversationPaginatorWrapper
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalCategoryLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.util.awaitWithTimeout
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
import uniffi.mail_uniffi.ConversationScrollerListUpdate
import uniffi.mail_uniffi.ConversationScrollerLiveQueryCallback
import uniffi.mail_uniffi.ConversationScrollerStatusUpdate
import uniffi.mail_uniffi.ConversationScrollerUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RustConversationsQueryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val createRustConversationPaginator: CreateRustConversationPaginator,
    @ConversationQueryCoroutineScope private val coroutineScope: CoroutineScope,
    private val invalidationRepository: PageInvalidationRepository
) : RustConversationsQuery {

    private var paginatorState: PaginatorState? = null
    private val paginatorMutex = Mutex()

    private val scrollerFetchNewStatusFlow = MutableStateFlow<ConversationScrollerStatusUpdate?>(null)
    private val categoryViewStatusFlow = MutableStateFlow<CategoryViewStatus?>(null)

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey
    ): Either<PaginationError, List<LocalConversation>> {

        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation-query: trying to load conversation with a null mailbox")
            return PaginationError.Other(DataError.Local.IllegalStateError).left()
        }

        val labelId = pageKey.labelId
        Timber.d("rust-conversation-query: observe conversations for labelId $labelId")

        val pageDescriptor = pageKey.toPageDescriptor(userId)

        val newPaginatorInitialized = paginatorMutex.withLock {
            if (shouldInitPaginator(pageDescriptor, pageKey)) {
                initPaginator(pageDescriptor, mailbox)
                true
            } else {
                false
            }
        }

        Timber.d("rust-conversation-query: Paging: querying ${pageKey.pageToLoad.name} page for conversation")

        return when (pageKey.pageToLoad) {
            PageToLoad.First,
            PageToLoad.Next -> loadNextConversationPage()

            PageToLoad.All -> {
                if (newPaginatorInitialized) {
                    Timber.d(
                        "rust-conversation-query: paginator newly created, calling nextPage() " +
                            "instead of reload() for pageKey: %s",
                        pageKey
                    )
                    loadNextConversationPage()
                } else {
                    Timber.d("rust-conversation-query: refreshing for pageKey: %s", pageKey)
                    reloadConversations()
                }
            }
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
    private suspend fun reloadConversations(): Either<PaginationError, List<LocalConversation>> {
        val cached = paginatorMutex.withLock { servableCachedItems() }
        if (cached != null) {
            Timber.d("rust-conversation-query: serving refresh from cache (%d items)", cached.size)
            return cached.right()
        }

        Timber.d("rust-conversation-query: cache cannot answer the refresh, calling reload()")
        val deferred = setPendingRequest(RequestType.Refresh)
        paginatorState?.paginatorWrapper?.reload()
            ?.onLeft { completeRejectedRequest(deferred, RequestType.Refresh, it) }
        return deferred.await()
    }

    private suspend fun loadNextConversationPage(): Either<PaginationError, List<LocalConversation>> {
        val deferred = setPendingRequest(RequestType.Append)
        paginatorState?.paginatorWrapper?.nextPage()
            ?.onLeft { completeRejectedRequest(deferred, RequestType.Append, it) }

        return deferred.await().let { firstResponse ->
            val followUp = paginatorState?.pendingRequest?.followUpResponse
            followUp?.awaitWithTimeout(NONE_FOLLOWUP_GRACE_MS, firstResponse) {
                Timber.d("rust-conversation-query: Follow-up response timed out.")
                clearPendingRequest()
            } ?: firstResponse
        }
    }

    override suspend fun supportsIncludeFilter() = paginatorState?.paginatorWrapper?.supportsIncludeFilter() == true

    override suspend fun terminatePaginator(userId: UserId) {
        if (paginatorState?.pageDescriptor?.userId == userId) {
            paginatorMutex.withLock {
                destroy()
            }
        } else {
            Timber.d("rust-conversation-query: Not terminating paginator, userId does not match")
        }
    }

    override suspend fun updateUnreadFilter(filterUnread: Boolean) {
        paginatorState?.paginatorWrapper?.filterUnread(filterUnread)
    }

    override suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean) {
        paginatorState?.paginatorWrapper?.showSpamAndTrash(showSpamTrash)
    }

    private suspend fun initPaginator(pageDescriptor: PageDescriptor, mailbox: MailboxWrapper) {

        Timber.d("rust-conversation-query: [destroy and] initialize paginator instance...")
        destroy()

        val scrollerOnUpdateHandler = ScrollerOnUpdateHandler<LocalConversation>(
            tag = "rust-conversation-query",
            invalidate = { invalidateLoadedItems() }
        )

        createRustConversationPaginator(
            mailbox = mailbox,
            enabledCategoryId = pageDescriptor.categoryLabelId?.toLocalCategoryLabelId(),
            callback = conversationsUpdatedCallback(scrollerOnUpdateHandler)
        )
            .onRight {
                Timber.d(
                    "rust-conversation-query: Paginator instance created, id=%s",
                    it.getScrollerId()
                )
                paginatorState = PaginatorState(
                    paginatorWrapper = it,
                    pageDescriptor = pageDescriptor,
                    scrollerCache = ScrollerCache()
                )

                // Get initial category view status
                categoryViewStatusFlow.value = it.getCategoryViewStatus()
                Timber.d(
                    "rust-conversation-query: Initial category view state: %s",
                    categoryViewStatusFlow.value
                )
            }
    }

    private fun conversationsUpdatedCallback(onUpdateHandler: ScrollerOnUpdateHandler<LocalConversation>) =
        object : ConversationScrollerLiveQueryCallback {
            override fun onUpdate(update: ConversationScrollerUpdate) {
                coroutineScope.launch {
                    paginatorMutex.withLock {
                        val scrollerUpdate = when (update) {
                            is ConversationScrollerUpdate.Status -> {
                                Timber.d("rust-conversation-query: Scroller fetch new status update: ${update.v1}")
                                scrollerFetchNewStatusFlow.value = update.v1
                                return@withLock
                            }

                            is ConversationScrollerUpdate.List -> update.toScrollerUpdate()

                            is ConversationScrollerUpdate.Error -> update.toScrollerUpdate()
                            is ConversationScrollerUpdate.CategoryViewChanged -> {
                                categoryViewStatusFlow.value = update.categoryView.toCategoryViewStatus()
                                return@withLock
                            }
                        }

                        Timber.d(
                            "rust-conversation-query: Received paginator update: %s with %d items, " +
                                "current cache: %d scrollerId=%s",
                            update.debugTypeName(),
                            scrollerUpdate.itemCount(),
                            paginatorState?.scrollerCache?.itemCount() ?: 0,
                            scrollerUpdate.scrollerId
                        )

                        // Update internal cache
                        val snapshot = paginatorState?.scrollerCache?.applyUpdate(
                            scrollerUpdate
                        ) ?: emptyList()
                        val pending = paginatorState?.pendingRequest

                        Timber.d("rust-conversation-query: Cache now has ${snapshot.size} items")

                        onUpdateHandler.handleUpdate(pending, scrollerUpdate, snapshot) {
                            // We need to wait for the follow-up response
                            if (pending?.type == RequestType.Append) {
                                Timber.d("rust-conversation-query: Triggering follow-up after immediate Append None")
                                paginatorState = paginatorState?.withFollowUpResponse()
                            }
                        }

                        if (paginatorState?.pendingRequest == null) {
                            Timber.d("rust-conversation-query: No pending request")
                        } else if (paginatorState?.pendingRequest?.isCompleted() == true) {
                            Timber.d("rust-conversation-query: Clearing completed pending request")
                            paginatorState = paginatorState?.copy(pendingRequest = null)
                        } else {
                            Timber.d("rust-conversation-query: Keeping pending request, waiting for more data")
                        }
                    }
                }
            }
        }

    private fun shouldInitPaginator(pageDescriptor: PageDescriptor, pageKey: PageKey.DefaultPageKey) =
        paginatorState == null ||
            paginatorState?.pageDescriptor != pageDescriptor ||
            pageKey.pageToLoad == PageToLoad.First

    override suspend fun getCursorFromActivePaginator(
        userId: UserId,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?,
        anchorConversationId: LocalConversationId
    ): Either<PaginationError, ConversationCursorWrapper>? {
        val pageDescriptor = PageDescriptor(userId, labelId, categoryLabelId)

        return paginatorMutex.withLock {
            val state = paginatorState
            when {
                state == null -> {
                    Timber.d("rust-conversation-query: No active paginator to reuse for cursor")
                    null
                }

                state.pageDescriptor != pageDescriptor -> {
                    Timber.d(
                        "rust-conversation-query: Active paginator cannot be reused for cursor. active=%s requested=%s",
                        state.pageDescriptor,
                        pageDescriptor
                    )
                    null
                }

                else -> {
                    Timber.d(
                        "rust-conversation-query: Reusing active mailbox paginator for cursor, scrollerId=%s",
                        state.paginatorWrapper.getScrollerId()
                    )
                    state.paginatorWrapper.getCursor(anchorConversationId)
                }
            }
        }
    }

    override fun observeScrollerFetchNewStatus(): Flow<ConversationScrollerStatusUpdate> =
        scrollerFetchNewStatusFlow.filterNotNull()

    override fun observeCategoryViewStatus(): Flow<CategoryViewStatus> = categoryViewStatusFlow.filterNotNull()

    override suspend fun setActiveCategoryLabel(categoryLabelId: LocalCategoryLabelId): Either<PaginationError, Unit> =
        paginatorState?.paginatorWrapper?.changeCategoryView(categoryLabelId)
            ?: PaginationError.Other(DataError.Local.IllegalStateError).left()

    private fun destroy() {
        if (paginatorState == null) {
            Timber.d("rust-conversation-query: no paginator to destroy")
        } else {
            Timber.d(
                "rust-conversation-query: disconnecting and destroying paginator with id=%s",
                paginatorState?.paginatorWrapper?.getScrollerId()
            )
            paginatorState?.paginatorWrapper?.disconnect()
            paginatorState = null
        }
    }

    private fun invalidateLoadedItems() {
        coroutineScope.launch {
            invalidationRepository.submit(PageInvalidationEvent.ConversationsInvalidated())
        }
    }

    private fun clearPendingRequest() {
        coroutineScope.launch {
            paginatorMutex.withLock {
                paginatorState = paginatorState?.copy(pendingRequest = null)
                Timber.d("rust-conversation-query: Cleared pending request")
            }
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
        deferred: CompletableDeferred<Either<PaginationError, List<LocalConversation>>>,
        type: RequestType,
        error: PaginationError
    ) {
        Timber.w("rust-conversation-query: %s call rejected by Rust: %s", type, error)
        paginatorMutex.withLock {
            if (paginatorState?.pendingRequest?.response === deferred) {
                paginatorState = paginatorState?.copy(pendingRequest = null)
            }
            deferred.complete(fallbackResponse(type).right())
        }
    }

    private suspend fun setPendingRequest(
        type: RequestType
    ): CompletableDeferred<Either<PaginationError, List<LocalConversation>>> {
        paginatorMutex.withLock {
            // Paging can start a load while a previous one is still waiting (several invalidations in a
            // row do exactly that). Only one pending request is tracked, so complete the one being
            // replaced rather than orphaning its deferred and leaving that Paging load in flight forever.
            paginatorState?.pendingRequest?.let { outgoing ->
                if (!outgoing.isCompleted()) {
                    Timber.w("rust-conversation-query: replacing a pending %s request", outgoing.type)
                    outgoing.response.complete(fallbackResponse(outgoing.type).right())
                    outgoing.followUpResponse?.complete(emptyList<LocalConversation>().right())
                }
            }

            val deferred = CompletableDeferred<Either<PaginationError, List<LocalConversation>>>()
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
    private fun servableCachedItems(): List<LocalConversation>? {
        val snapshot = paginatorState?.scrollerCache?.takeIf { it.mirrorsScroller }?.snapshot ?: return null

        // An empty list is not an answer, because the scroller empties this cache on its way *into* a load:
        // reset() clears the list and reports FIRST_PAGE_LOADING_START before it fetches anything. Serving
        // that emptiness settles the Paging refresh at the start of the load with nothing in it, so the
        // mailbox reads as "No messages" for the whole fetch. Going to Rust leaves the refresh in flight,
        // which is what the loading skeleton keys off. See ET-6647.
        if (snapshot.isEmpty()) return null

        // Conversations are unique per id, so a repeat means the incremental updates left one behind (see
        // rememberDuplicateTolerantMailboxKeys). Rust's list is authoritative and replaces the cache when
        // it arrives, so refresh from it rather than serving the duplicate back for as long as it survives.
        val duplicates = snapshot.size - snapshot.distinctBy { it.id }.size
        if (duplicates > 0) {
            Timber.e("rust-conversation-query: cache holds %d duplicate item(s), asking Rust instead", duplicates)
            return null
        }

        return snapshot
    }

    // What a request resolves to when no Rust response is coming: everything we hold for a Refresh
    // (it asks for the loaded items), nothing for an Append (it asks for items beyond them).
    // Mirrors ScrollerOnUpdateHandler's fallback. Callers must hold [paginatorMutex].
    private fun cachedItems(): List<LocalConversation> = paginatorState?.scrollerCache?.snapshot ?: emptyList()

    private fun fallbackResponse(type: RequestType): List<LocalConversation> = when (type) {
        // Deduplicated as well: Paging should never be handed a repeated id, whichever path answers.
        RequestType.Refresh -> cachedItems().distinctBy { it.id }
        RequestType.Append -> emptyList()
    }

    private data class PaginatorState(
        val paginatorWrapper: ConversationPaginatorWrapper,
        val pageDescriptor: PageDescriptor,
        val scrollerCache: ScrollerCache<LocalConversation>,
        val pendingRequest: PendingRequest<LocalConversation>? = null
    )

    private fun PageKey.DefaultPageKey.toPageDescriptor(userId: UserId): PageDescriptor = PageDescriptor(
        userId = userId,
        labelId = this.labelId,
        categoryLabelId = this.categoryLabelId
    )

    private fun PaginatorState.withFollowUpResponse(): PaginatorState {
        val currentPending = this.pendingRequest
            ?: return this

        val newPending = currentPending.copy(followUpResponse = CompletableDeferred())

        return this.copy(pendingRequest = newPending)
    }

    companion object {

        const val NONE_FOLLOWUP_GRACE_MS = 250L
    }
}

fun ConversationScrollerUpdate.List.toScrollerUpdate(): ScrollerUpdate<LocalConversation> =
    when (val listResult = this.v1) {
        is ConversationScrollerListUpdate.Append -> ScrollerUpdate.Append(
            scrollerId = listResult.scrollerId,
            items = listResult.items
        )

        is ConversationScrollerListUpdate.ReplaceFrom -> ScrollerUpdate.ReplaceFrom(
            scrollerId = listResult.scrollerId,
            idx = listResult.idx.toInt(),
            items = listResult.items
        )

        is ConversationScrollerListUpdate.ReplaceBefore -> ScrollerUpdate.ReplaceBefore(
            scrollerId = listResult.scrollerId,
            idx = listResult.idx.toInt(),
            items = listResult.items
        )

        is ConversationScrollerListUpdate.ReplaceRange -> ScrollerUpdate.ReplaceRange(
            scrollerId = listResult.scrollerId,
            fromIdx = listResult.from.toInt(),
            toIdx = listResult.to.toInt(),
            items = listResult.items
        )

        is ConversationScrollerListUpdate.None -> ScrollerUpdate.None(
            scrollerId = listResult.scrollerId
        )
    }

fun ConversationScrollerUpdate.Error.toScrollerUpdate(): ScrollerUpdate<LocalConversation> = ScrollerUpdate.Error(
    error = this.error
)

fun ConversationScrollerUpdate.debugTypeName(): String = when (this) {
    is ConversationScrollerUpdate.List -> this.v1.debugTypeName()
    is ConversationScrollerUpdate.Status -> this.v1.debugTypeName()
    is ConversationScrollerUpdate.Error -> "Error"
    is ConversationScrollerUpdate.CategoryViewChanged -> "CategoryViewChanged"
}

fun ConversationScrollerListUpdate.debugTypeName(): String = when (this) {
    is ConversationScrollerListUpdate.None -> "None"
    is ConversationScrollerListUpdate.Append -> "Append"
    is ConversationScrollerListUpdate.ReplaceFrom -> "ReplaceFrom"
    is ConversationScrollerListUpdate.ReplaceBefore -> "ReplaceBefore"
    is ConversationScrollerListUpdate.ReplaceRange -> "ReplaceRange"
}

fun ConversationScrollerStatusUpdate.debugTypeName(): String = when (this) {
    ConversationScrollerStatusUpdate.FETCH_NEW_START -> "FETCH_NEW_START"
    ConversationScrollerStatusUpdate.FETCH_NEW_END -> "FETCH_NEW_END"
    ConversationScrollerStatusUpdate.FIRST_PAGE_LOADING_START -> "FIRST_PAGE_LOADING_START"
    ConversationScrollerStatusUpdate.FIRST_PAGE_LOADING_END -> "FIRST_PAGE_LOADING_END"
}
