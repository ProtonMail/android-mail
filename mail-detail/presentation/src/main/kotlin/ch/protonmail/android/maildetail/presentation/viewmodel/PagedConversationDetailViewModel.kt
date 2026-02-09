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

package ch.protonmail.android.maildetail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.EphemeralMailboxCursor
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maildetail.domain.model.ConversationOpenMode
import ch.protonmail.android.maildetail.domain.usecase.ObserveIsSingleMessageViewModePreferred
import ch.protonmail.android.maildetail.presentation.mapper.toPage
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.NavigationArgs
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailAction
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent.AutoAdvanceRequested
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent.ClearFocusPage
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent.UpdatePage
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.PagedConversationEffects
import ch.protonmail.android.maildetail.presentation.reducer.PagedConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.PagedConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetConversationCursor
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.repository.AutoAdvanceRepository
import ch.protonmail.android.mailsettings.domain.repository.SwipeNextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import ch.protonmail.android.maildetail.presentation.model.Error as UiError

@HiltViewModel
class PagedConversationDetailViewModel @Inject constructor(
    private val autoAdvanceRepository: AutoAdvanceRepository,
    private val getConversationCursor: GetConversationCursor,
    private val swipeNextRepository: SwipeNextRepository,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeIsSingleMessageViewModePreferred: ObserveIsSingleMessageViewModePreferred,
    private val savedStateHandle: SavedStateHandle,
    private val reducer: PagedConversationDetailReducer
) : ViewModel() {

    private val _effects = MutableStateFlow(PagedConversationEffects())
    val effects = _effects.asStateFlow()

    private var conversationCursor: ConversationCursor? = null
    private val cursorMutex = Mutex()

    private val conversationId = savedStateHandle.getStateFlow(
        ConversationDetailScreen.ConversationIdKey,
        requireConversationId().id
    ).map { ConversationId(it) }

    private val mutableState = MutableStateFlow<PagedConversationDetailState>(PagedConversationDetailState.Loading)

    val state: StateFlow<PagedConversationDetailState> = mutableState.asStateFlow()

    private val cursorParamsFlow = combine(
        observePrimaryUserId().filterNotNull(),
        conversationId
    ) { userId, convId ->
        Pair(userId, convId)
    }
        .flatMapLatest { (userId, convId) ->
            swipeNextRepository.observeSwipeNext(userId)
                .map { swipeEnabledResult ->
                    val swipeEnabled = swipeEnabledResult.getOrNull()?.enabled ?: false
                    val autoAdvance = getAutoAdvance(userId)

                    CursorParams(
                        userId = userId,
                        conversationId = convId,
                        swipeEnabled = swipeEnabled,
                        autoAdvance = autoAdvance
                    )
                }
        }

    init {
        viewModelScope.launch {
            cursorParamsFlow
                .distinctUntilChanged()
                .flatMapLatest { params ->
                    resolveSingleMessageModePreferredFlow(params.userId)
                        .flatMapLatest { singleMessageModePreferred ->
                            getConversationCursor(
                                singleMessageModePreferred = singleMessageModePreferred,
                                conversationId = params.conversationId,
                                userId = params.userId,
                                messageId = getInitialScrollToMessageId()?.id,
                                locationViewModeIsConversation = requireLocationViewModeModeIsConversation()
                            ).map { cursorState ->
                                CursorResult(
                                    swipeEnabled = params.swipeEnabled,
                                    autoAdvance = params.autoAdvance,
                                    singleMessageModePreferred = singleMessageModePreferred,
                                    cursorState = cursorState
                                )
                            }
                        }
                }
                .collect { result ->
                    onCursor(
                        swipeEnabled = result.swipeEnabled,
                        autoAdvance = result.autoAdvance,
                        singleMessageModePreferred = result.singleMessageModePreferred,
                        cursorState = result.cursorState
                    )
                }
        }
    }

    private fun resolveSingleMessageModePreferredFlow(userId: UserId): Flow<Boolean> {
        return when (requireConversationOpenMode()) {
            ConversationOpenMode.Message -> flowOf(true)
            ConversationOpenMode.Conversation -> flowOf(false)
            ConversationOpenMode.UseUserPreference -> observeIsSingleMessageViewModePreferred(userId)
        }
    }

    private suspend fun onCursor(
        swipeEnabled: Boolean,
        autoAdvance: Boolean,
        singleMessageModePreferred: Boolean,
        cursorState: EphemeralMailboxCursor?
    ) {
        when (cursorState) {
            null,
            is EphemeralMailboxCursor.CursorDead,
            is EphemeralMailboxCursor.NotInitalised -> {
                emitNewStateFor(
                    PagedConversationDetailEvent.Error(ConversationCursorError.InvalidState)
                )
                _effects.value =
                    PagedConversationEffects(Effect.of(UiError.OTHER))
            }

            is EphemeralMailboxCursor.Data -> {
                val cursor = cursorState.cursor

                cursorMutex.withLock {
                    conversationCursor = cursor
                }

                emitNewStateFor(
                    PagedConversationDetailEvent.Ready(
                        swipeEnabled = swipeEnabled,
                        autoAdvance = autoAdvance,
                        cursor.current.toPage(),
                        cursor.next.toPage(),
                        cursor.previous.toPage(),
                        navigationArgs = NavigationArgs(
                            openedFromLocation = requireLabelId(),
                            singleMessageMode = singleMessageModePreferred,
                            conversationEntryPoint = getEntryPoint()
                        )
                    )
                )
            }

            EphemeralMailboxCursor.Initialising -> {}
        }
    }

    private fun emitNewStateFor(event: PagedConversationDetailEvent) {
        val currentState = mutableState.value
        mutableState.update { reducer.newStateFrom(currentState, event) }
    }

    fun submit(action: PagedConversationDetailAction) {
        viewModelScope.launch {
            when (action) {
                is PagedConversationDetailAction.SetSettledPage -> onSettledPage(action.value)
                is PagedConversationDetailAction.ClearFocusPage -> emitNewStateFor(ClearFocusPage)
                is PagedConversationDetailAction.AutoAdvance ->
                    handleAutoAdvance()

            }
        }
    }

    private fun handleAutoAdvance() {
        // block scrolling whilst we fetch pages, usually not async
        emitNewStateFor(AutoAdvanceRequested)
    }


    private suspend fun onSettledPage(index: Int) {
        guardCursor { cursor ->
            (state.value as? PagedConversationDetailState.Ready)?.let { state ->
                state.dynamicViewPagerState.currentPageIndex?.let { currentIndex ->
                    if (index < currentIndex) {
                        cursor.moveBackward()
                        emitNewStateFor(
                            UpdatePage(
                                cursor.current.toPage(),
                                cursor.next.toPage(),
                                cursor.previous.toPage()
                            )
                        )
                    }
                    if (index > currentIndex) {
                        cursor.moveForward()
                        emitNewStateFor(
                            UpdatePage(
                                cursor.current.toPage(),
                                cursor.next.toPage(),
                                cursor.previous.toPage()
                            )
                        )
                    }

                    state.dynamicViewPagerState.pendingRemoval?.let {
                        cursor.invalidatePrevious()
                    }
                }
            }
        }
    }

    private suspend fun getAutoAdvance(userId: UserId) = autoAdvanceRepository.getAutoAdvance(userId)
        .fold(
            ifLeft = {
                Timber.e("Error getting Auto Advance Settings for user: $userId")
                false
            },
            ifRight = { autoAdvanceEnabled ->
                autoAdvanceEnabled
            }
        )

    override fun onCleared() {
        viewModelScope.launch {
            cursorMutex.withLock {
                conversationCursor = null
            }
        }
        super.onCleared()
    }

    private fun requireConversationId(): ConversationId {
        val conversationId = savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey)
            ?: throw IllegalStateException("No Conversation id given")
        return ConversationId(conversationId)
    }

    private fun requireLabelId(): LabelId {
        val labelId = savedStateHandle.get<String>(ConversationDetailScreen.OpenedFromLocationKey)
            ?: throw IllegalStateException("No Conversation id given")
        return LabelId(labelId)
    }

    private fun requireConversationOpenMode(): ConversationOpenMode {
        val value = savedStateHandle.get<String>(ConversationDetailScreen.ConversationOpenModeKey)
            ?: throw IllegalStateException("No ConversationOpenMode given")
        return ConversationOpenMode.valueOf(value)
    }

    @Suppress("FunctionMaxLength")
    private fun requireLocationViewModeModeIsConversation(): Boolean {
        val isConversation = savedStateHandle.get<String>(PagedConversationDetailScreen.LocationViewModeIsConversation)
            ?: throw IllegalStateException("No viewMode given")
        return isConversation.toBoolean()
    }

    private fun getInitialScrollToMessageId(): MessageIdUiModel? {
        val messageIdStr = savedStateHandle.get<String>(ConversationDetailScreen.ScrollToMessageIdKey)
        return messageIdStr?.let { if (it == "null") null else MessageIdUiModel(it) }
    }

    private fun getEntryPoint(): ConversationDetailEntryPoint {
        val value = savedStateHandle.get<String>(ConversationDetailScreen.ConversationDetailEntryPointNameKey)
            ?: throw IllegalStateException("No Entry point given")
        return ConversationDetailEntryPoint.valueOf(value)
    }

    private suspend fun guardCursor(block: suspend (cursor: ConversationCursor) -> Unit) {
        cursorMutex.withLock {
            val cursor = conversationCursor
            if (cursor != null) {
                block(cursor)
            } else {
                Timber.w(
                    "conversation-cursor PagedConversationDetailViewModel" +
                        " guardCursor received a null cursor and couldn't execute block"
                )
            }
        }
    }

    private data class CursorParams(
        val userId: UserId,
        val conversationId: ConversationId,
        val swipeEnabled: Boolean,
        val autoAdvance: Boolean
    )

    private data class CursorResult(
        val swipeEnabled: Boolean,
        val autoAdvance: Boolean,
        val singleMessageModePreferred: Boolean,
        val cursorState: EphemeralMailboxCursor?
    )
}
