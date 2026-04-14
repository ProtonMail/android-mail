/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcommon.data.repository

import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor as ConversationCursorWrapper

/**
 * Maintains a cursor position allowing sequential based access to Conversations, maintains a position allowing
 * forwards and backwards navigation through a conversationList.  The cursor is stateful and must be closed after use
 *
 * End indicates there are no more entries available in this direction.
 *
 * [moveForward] and [moveBackward]  should be called asyncryonously to move the cursor forwards and backwards in
 * response to UI events. but the UI can use [next] and [previous] to update the page immediately and then call the
 * respective functions to update cursor position
 *
 */
class RustConversationCursorImpl private constructor(
    private val conversationCursorWrapper: ConversationCursorWrapper,
    initialCurrent: CursorResult?,
    initialPrevious: CursorResult?,
    initialNext: CursorResult?
) :
    ConversationCursor {

    override var current: CursorResult? = null
        private set

    override var next: CursorResult? = null
        private set
    override var previous: CursorResult? = null
        private set

    private val mutex = Mutex()

    init {
        current = initialCurrent
        previous = initialPrevious?.recoverFromErrorIfNeeded()
        next = initialNext?.recoverFromErrorIfNeeded()
        Timber.d(
            "conversation-pager init " +
                "prev $previous current $current  $next $next"
        )

    }

    override suspend fun invalidatePrevious() {
        mutex.withLock {
            previous = null
            previous = conversationCursorWrapper.previousPage()
        }
    }

    override suspend fun moveForward() {
        mutex.withLock {
            when (next) {
                is CursorResult.End -> {
                    Timber.d("conversation-pager forward no more conversations left to scroll to")
                    previous = current
                    current = next
                    return
                }

                null -> {
                    // in the case that pre-load failed next could be null so retry here
                    Timber.d("conversation-pager next was not preloaded, trying to load again")
                    previous = current
                    // try reload next
                    current = conversationCursorWrapper.nextPage()
                    // then move forwards
                    conversationCursorWrapper.goForwards()
                    // then preload
                    next = conversationCursorWrapper.nextPage()
                }

                // an error should not block the user from moving forwards past this cursor position
                is CursorResult.Error,
                is CursorResult.Cursor -> {
                    if (next is CursorResult.Error) {
                        Timber.d("conversation-pager result was an error")
                    }
                    previous = current
                    current = next
                    // move forwards
                    conversationCursorWrapper.goForwards()
                    // preload
                    next = null
                    next = conversationCursorWrapper.nextPage().recoverFromErrorIfNeeded()
                    Timber.d(
                        "conversation-pager next " +
                            "prev $previous current $current next $next "
                    )
                }
            }
        }
    }

    override suspend fun moveBackward() {
        mutex.withLock {
            when (previous) {
                is CursorResult.End -> {
                    Timber.d("conversation-pager no more backwards conversations left to scroll to")
                    next = current
                    current = previous
                    return
                }

                null -> {
                    // in the case that pre-load failed next could be null so retry here
                    Timber.d("conversation-pager next was not preloaded, trying to load again")
                    next = current
                    // try reload next
                    current = conversationCursorWrapper.previousPage()
                    // then move forwards
                    conversationCursorWrapper.goBackwards()
                    // then preload
                    previous = conversationCursorWrapper.previousPage()
                }

                // an error should not block the user from moving backwards past this cursor position
                is CursorResult.Error,
                is CursorResult.Cursor -> {
                    if (previous is CursorResult.Error) {
                        Timber.d("conversation-pager prev result was an error")
                    }
                    next = current
                    current = previous
                    // move forwards
                    conversationCursorWrapper.goBackwards()
                    // preload
                    previous = null
                    previous = conversationCursorWrapper.previousPage().recoverFromErrorIfNeeded()
                }
            }
        }
    }

    /**
     * If error is network set result to null so that we can retry the load when the user moves the cursor
     */
    private fun CursorResult.recoverFromErrorIfNeeded() =
        if (this is CursorResult.Error && this.conversationCursorError == ConversationCursorError.Offline) {
            // recoverable error
            Timber.d("conversation-pager recoverFromErrorIfNeeded next is null")
            null
        } else this.apply {
            if (this is CursorResult.Error) {
                Timber.d("conversation-pager non recoverable error for page $this")
            }
        }


    override fun close() {
        conversationCursorWrapper.disconnect()
    }

    companion object {

        suspend operator fun invoke(
            anchorItemId: CursorId,
            conversationCursorWrapper: ConversationCursorWrapper
        ): RustConversationCursorImpl {
            val current = CursorResult.Cursor(anchorItemId.conversationId, anchorItemId.messageId)
            val previous = conversationCursorWrapper.previousPage()
            val next = conversationCursorWrapper.nextPage()
            return RustConversationCursorImpl(
                conversationCursorWrapper,
                current,
                previous,
                next
            )
        }
    }
}
