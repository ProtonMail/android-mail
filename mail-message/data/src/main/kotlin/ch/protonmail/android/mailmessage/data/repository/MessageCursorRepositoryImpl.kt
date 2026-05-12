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

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalItemId
import ch.protonmail.android.mailcommon.data.repository.RustConversationCursorImpl
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.local.RustMessageListQuery
import ch.protonmail.android.mailmessage.data.local.debugTypeName
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessagesPaginator
import ch.protonmail.android.mailmessage.data.wrapper.MailMessageCursorWrapper
import ch.protonmail.android.mailmessage.data.wrapper.MessagePaginatorWrapper
import ch.protonmail.android.mailmessage.domain.model.toConversationCursorError
import ch.protonmail.android.mailmessage.domain.repository.MessageCursorRepository
import ch.protonmail.android.mailsnooze.data.mapper.toLocalConversationId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.MessageScrollerLiveQueryCallback
import uniffi.mail_uniffi.MessageScrollerUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageCursorRepositoryImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val createRustMessagesPaginator: CreateRustMessagesPaginator,
    private val rustMessageListQuery: RustMessageListQuery
) : MessageCursorRepository {

    private var cursorPaginatorState: CursorPaginatorState? = null
    private val cursorPaginatorMutex = Mutex()

    override suspend fun getCursor(
        anchorItemId: CursorId,
        userId: UserId,
        labelId: LabelId,
        categoryLabelId: CategoryLabelId?
    ): Either<ConversationCursorError, ConversationCursor> {
        val itemId = anchorItemId.messageId?.toLocalConversationId()
            ?: anchorItemId.conversationId.toLocalConversationId()

        return getCursorWrapper(
            userId = userId,
            labelId = labelId,
            categoryLabelId = categoryLabelId,
            anchorItemId = itemId
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
        anchorItemId: LocalItemId
    ): Either<ConversationCursorError, MailMessageCursorWrapper> {
        when (
            val mailboxCursorResult = rustMessageListQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                categoryLabelId = categoryLabelId,
                anchorItemId = anchorItemId
            )
        ) {
            null -> {
                Timber.d(
                    "message-cursor-repository: No reusable mailbox paginator cursor, falling back"
                )
            }

            is Either.Right -> {
                Timber.d("message-cursor-repository: Reused mailbox paginator cursor")
                cursorPaginatorMutex.withLock {
                    if (hasCursorPaginator()) destroyCursorPaginator()
                }
                return mailboxCursorResult
            }

            is Either.Left -> {
                Timber.w(
                    "message-cursor-repository: Failed to get cursor from active mailbox paginator, " +
                        "falling back. error=%s",
                    mailboxCursorResult.value
                )
            }
        }

        return getCursorFromCursorPaginator(
            userId = userId,
            labelId = labelId,
            anchorItemId = anchorItemId
        )
    }

    private suspend fun getCursorFromCursorPaginator(
        userId: UserId,
        labelId: LabelId,
        anchorItemId: LocalItemId
    ): Either<ConversationCursorError, MailMessageCursorWrapper> = cursorPaginatorMutex.withLock {
        val pageDescriptor = PageDescriptor(userId = userId, labelId = labelId)
        val state = cursorPaginatorState

        if (state == null || state.pageDescriptor != pageDescriptor) {
            Timber.d(
                "message-cursor-repository: Initializing cursor paginator for pageDescriptor=%s",
                pageDescriptor
            )

            val mailbox = rustMailboxFactory.create(userId, labelId.toLocalLabelId()).getOrNull()
            if (mailbox == null) {
                Timber.e(
                    "message-cursor-repository: Unable to create mailbox for userId=%s and labelId=%s",
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
                "message-cursor-repository: Reusing existing cursor paginator, scrollerId=%s",
                state.paginatorWrapper.getScrollerId()
            )
        }

        cursorPaginatorState?.paginatorWrapper?.getCursor(anchorItemId)?.mapLeft {
            it.toConversationCursorError()
        } ?: ConversationCursorError.InvalidState.left()
    }

    private suspend fun initCursorPaginator(
        pageDescriptor: PageDescriptor,
        mailbox: MailboxWrapper
    ): ConversationCursorError? {
        if (hasCursorPaginator()) destroyCursorPaginator()

        return createRustMessagesPaginator(
            mailbox = mailbox,
            callback = object : MessageScrollerLiveQueryCallback {
                override fun onUpdate(update: MessageScrollerUpdate) {
                    Timber.d(
                        "message-cursor-repository: Cursor paginator update=%s",
                        update.debugTypeName()
                    )
                }
            }
        ).fold(
            ifLeft = { error ->
                Timber.e(
                    "message-cursor-repository: Failed to create cursor paginator. error=%s",
                    error
                )
                ConversationCursorError.Other(error)
            },
            ifRight = { paginator ->
                Timber.d(
                    "message-cursor-repository: Cursor paginator created, scrollerId=%s",
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
            Timber.d("message-cursor-repository: No cursor paginator to destroy")
            return
        }

        Timber.d(
            "message-cursor-repository: Destroying cursor paginator, scrollerId=%s",
            state.paginatorWrapper.getScrollerId()
        )
        state.paginatorWrapper.disconnect()
        cursorPaginatorState = null
    }

    private fun hasCursorPaginator(): Boolean = cursorPaginatorState != null

    private fun String.toLocalConversationId() = LocalConversationId(this.toULong())

    private data class CursorPaginatorState(
        val paginatorWrapper: MessagePaginatorWrapper,
        val pageDescriptor: PageDescriptor
    )

    private data class PageDescriptor(
        val userId: UserId,
        val labelId: LabelId
    )
}
