/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailconversation.data.repository

import javax.inject.Singleton

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.repository.RustConversationCursorImpl
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailconversation.data.local.RustConversationsQuery
import ch.protonmail.android.mailconversation.data.local.debugTypeName
import ch.protonmail.android.mailconversation.data.model.PageDescriptor
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationPaginator
import ch.protonmail.android.mailconversation.data.wrapper.ConversationCursorWrapper
import ch.protonmail.android.mailconversation.data.wrapper.ConversationPaginatorWrapper
import ch.protonmail.android.mailconversation.domain.repository.ConversationCursorRepository
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalCategoryLabelId
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.toConversationCursorError
import ch.protonmail.android.mailsnooze.data.mapper.toLocalConversationId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ConversationScrollerLiveQueryCallback
import uniffi.mail_uniffi.ConversationScrollerUpdate
import javax.inject.Inject

@Singleton
class ConversationCursorRepositoryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val createRustConversationPaginator: CreateRustConversationPaginator,
    private val rustConversationsQuery: RustConversationsQuery
) : ConversationCursorRepository {

    private var cursorPaginatorState: CursorPaginatorState? = null
    private val cursorPaginatorMutex = Mutex()

    override suspend fun getCursor(
        anchorItemId: CursorId,
        userId: UserId,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?
    ): Either<ConversationCursorError, ConversationCursor> {
        val anchorConversationId = anchorItemId.conversationId.toLocalConversationId()

        return getCursorWrapper(
            userId = userId,
            labelId = labelId,
            categoryLabelId = categoryLabelId,
            anchorConversationId = anchorConversationId
        ).map { cursorWrapper ->
            RustConversationCursorImpl(anchorItemId, cursorWrapper)
        }
    }

    /**
     * Cursor lookup strategy:
     *
     * 1. Try to obtain the cursor from the active mailbox paginator.
     * 2. If mailbox paginator reuse is unavailable or fails, fall back to a dedicated
     *    cursor paginator managed by this repository.
     */
    private suspend fun getCursorWrapper(
        userId: UserId,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?,
        anchorConversationId: LocalConversationId
    ): Either<ConversationCursorError, ConversationCursorWrapper> {
        when (
            val mailboxCursorResult = rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                categoryLabelId = categoryLabelId,
                anchorConversationId = anchorConversationId
            )
        ) {
            null -> {
                Timber.d(
                    "conversation-cursor-repository: No reusable mailbox paginator cursor, falling back"
                )
            }

            is Either.Right -> {
                Timber.d("conversation-cursor-repository: Reused mailbox paginator cursor")
                cursorPaginatorMutex.withLock {
                    if (hasCursorPaginator()) destroyCursorPaginator()
                }
                return mailboxCursorResult
            }

            is Either.Left -> {
                Timber.w(
                    "conversation-cursor-repository: Failed to get cursor from active mailbox paginator, " +
                        "falling back. error=%s",
                    mailboxCursorResult.value
                )
            }
        }

        return getCursorFromCursorPaginator(
            userId = userId,
            labelId = labelId,
            anchorConversationId = anchorConversationId
        )
    }

    private suspend fun getCursorFromCursorPaginator(
        userId: UserId,
        labelId: LabelId,
        anchorConversationId: LocalConversationId
    ): Either<ConversationCursorError, ConversationCursorWrapper> = cursorPaginatorMutex.withLock {
        val pageDescriptor = PageDescriptor(userId = userId, labelId = labelId)
        val state = cursorPaginatorState

        if (state == null || state.pageDescriptor != pageDescriptor) {
            Timber.d(
                "conversation-cursor-repository: Initializing cursor paginator for pageDescriptor=%s",
                pageDescriptor
            )

            val mailbox = rustMailboxFactory.create(userId, labelId.toLocalLabelId()).getOrNull()
            if (mailbox == null) {
                Timber.e(
                    "conversation-cursor-repository: Unable to create mailbox for userId=%s and labelId=%s",
                    userId,
                    labelId
                )
                return@withLock ConversationCursorError.InvalidState.left()
            }

            val initError = initCursorPaginator(pageDescriptor, mailbox)
            if (initError != null) {
                return@withLock initError.left()
            }
        } else {
            Timber.d(
                "conversation-cursor-repository: Reusing existing cursor paginator, scrollerId=%s",
                state.paginatorWrapper.getScrollerId()
            )
        }

        cursorPaginatorState?.paginatorWrapper?.getCursor(anchorConversationId)?.mapLeft {
            it.toConversationCursorError()
        } ?: ConversationCursorError.InvalidState.left()
    }

    private suspend fun initCursorPaginator(
        pageDescriptor: PageDescriptor,
        mailbox: MailboxWrapper
    ): ConversationCursorError? {
        if (hasCursorPaginator()) destroyCursorPaginator()

        return createRustConversationPaginator(
            mailbox = mailbox,
            enabledCategoryId = pageDescriptor.categoryLabelId?.toLocalCategoryLabelId(),
            callback = object : ConversationScrollerLiveQueryCallback {
                override fun onUpdate(update: ConversationScrollerUpdate) {
                    Timber.d(
                        "conversation-cursor-repository: Cursor paginator update=%s",
                        update.debugTypeName()
                    )
                }
            }
        ).fold(
            ifLeft = { error ->
                Timber.e(
                    "conversation-cursor-repository: Failed to create cursor paginator. error=%s",
                    error
                )
                ConversationCursorError.Other(error)
            },
            ifRight = { paginator ->
                Timber.d(
                    "conversation-cursor-repository: Cursor paginator created, scrollerId=%s",
                    paginator.getScrollerId()
                )
                cursorPaginatorState = CursorPaginatorState(
                    paginatorWrapper = paginator,
                    pageDescriptor = pageDescriptor
                )
                null
            }
        )
    }

    private fun destroyCursorPaginator() {
        val state = cursorPaginatorState ?: run {
            Timber.d("conversation-cursor-repository: No cursor paginator to destroy")
            return
        }

        Timber.d(
            "conversation-cursor-repository: Destroying cursor paginator, scrollerId=%s",
            state.paginatorWrapper.getScrollerId()
        )
        state.paginatorWrapper.disconnect()
        cursorPaginatorState = null
    }

    private fun hasCursorPaginator(): Boolean = cursorPaginatorState != null

    private data class CursorPaginatorState(
        val paginatorWrapper: ConversationPaginatorWrapper,
        val pageDescriptor: PageDescriptor
    )

}
