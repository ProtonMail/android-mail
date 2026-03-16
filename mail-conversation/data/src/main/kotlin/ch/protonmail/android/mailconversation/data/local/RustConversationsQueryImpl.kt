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
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.ConversationQueryCoroutineScope
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationPaginator
import ch.protonmail.android.mailconversation.data.wrapper.ConversationCursorWrapper
import ch.protonmail.android.mailconversation.data.wrapper.ConversationPaginatorWrapper
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
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

        paginatorMutex.withLock {
            if (shouldInitPaginator(pageDescriptor, pageKey)) {
                initPaginator(pageDescriptor, mailbox)
            }
        }

        Timber.d("rust-conversation-query: Paging: querying ${pageKey.pageToLoad.name} page for conversation")

        val response = when (pageKey.pageToLoad) {
            PageToLoad.First,
            PageToLoad.Next -> {
                val deferred = setPendingRequest(RequestType.Append)
                paginatorState?.paginatorWrapper?.nextPage()

                // Wait for immediate Append response
                deferred.await().let { firstResponse ->

                    // If available wait for follow-up response
                    val followUp = paginatorState?.pendingRequest?.followUpResponse
                    followUp?.awaitWithTimeout(NONE_FOLLOWUP_GRACE_MS, firstResponse) {
                        Timber.d("rust-conversation-query: Follow-up response timed out.")

                        clearPendingRequest()
                    } ?: firstResponse
                }
            }

            PageToLoad.All -> {
                val deferred = setPendingRequest(RequestType.Refresh)
                paginatorState?.paginatorWrapper?.reload()

                // Wait for immediate Refresh response
                deferred.await()
            }
        }

        response.fold(
            ifLeft = { error ->
                Timber.d("rust-conversation-query: Page loading failed with error=%s", error)
            },
            ifRight = { conversations ->
                Timber.d("rust-conversation-query: Page loading completed with %d conversations", conversations.size)
            }
        )

        return response
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


    private fun shouldInitPaginatorForCursor(pageDescriptor: PageDescriptor): Boolean =
        paginatorState == null || paginatorState?.pageDescriptor != pageDescriptor

    override suspend fun getCursor(
        userId: UserId,
        labelId: LabelId,
        conversationId: LocalConversationId
    ): Either<PaginationError, ConversationCursorWrapper>? {

        val pageDescriptor = PageDescriptor(userId, labelId)
        val initError = paginatorMutex.withLock {
            if (shouldInitPaginatorForCursor(pageDescriptor)) {
                Timber.d(
                    "rust-conversation-query: Initializing paginator for getCursor with page desc=%s",
                    pageDescriptor
                )
                val mailbox = rustMailboxFactory.create(userId, labelId.toLocalLabelId()).getOrNull()
                if (mailbox == null) {
                    Timber.e(
                        "rust-conversation-query: Unable to create mailbox for userId=%s and labelId=%s",
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
                    "rust-conversation-query: Reusing existing paginator for getCursor with page desc=%s",
                    pageDescriptor
                )
                null
            }
        }

        if (initError != null) return initError

        return paginatorState?.paginatorWrapper?.getCursor(conversationId)
    }

    override fun observeScrollerFetchNewStatus(): Flow<ConversationScrollerStatusUpdate> =
        scrollerFetchNewStatusFlow.filterNotNull()

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

    private suspend fun setPendingRequest(
        type: RequestType
    ): CompletableDeferred<Either<PaginationError, List<LocalConversation>>> {
        paginatorMutex.withLock {
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

    private data class PaginatorState(
        val paginatorWrapper: ConversationPaginatorWrapper,
        val pageDescriptor: PageDescriptor,
        val scrollerCache: ScrollerCache<LocalConversation>,
        val pendingRequest: PendingRequest<LocalConversation>? = null
    )

    private data class PageDescriptor(
        val userId: UserId,
        val labelId: LabelId
    )

    private fun PageKey.DefaultPageKey.toPageDescriptor(userId: UserId): PageDescriptor =
        PageDescriptor(userId = userId, labelId = this.labelId)

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
}
