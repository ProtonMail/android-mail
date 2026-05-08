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
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.model.ConversationId
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
import ch.protonmail.android.maildetail.presentation.model.PagerSettings
import ch.protonmail.android.maildetail.presentation.reducer.PagedConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.PagedConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.ClearMessageBodyCache
import ch.protonmail.android.maildetail.presentation.usecase.GetConversationCursor
import ch.protonmail.android.maildetail.presentation.usecase.ObserveAutoAdvanceSetting
import ch.protonmail.android.maildetail.presentation.usecase.ObserveSwipeNextPreference
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val observeAutoAdvanceSetting: ObserveAutoAdvanceSetting,
    private val getConversationCursor: GetConversationCursor,
    private val observeSwipeNextPreference: ObserveSwipeNextPreference,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeIsSingleMessageViewModePreferred: ObserveIsSingleMessageViewModePreferred,
    private val clearMessageBodyCache: ClearMessageBodyCache,
    @AppScope private val appScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val reducer: PagedConversationDetailReducer
) : ViewModel() {

    private val _effects = MutableStateFlow(PagedConversationEffects())
    val effects = _effects.asStateFlow()

    private var conversationCursor: ConversationCursor? = null
    private val cursorMutex = Mutex()

    private val mutableState = MutableStateFlow<PagedConversationDetailState>(PagedConversationDetailState.Loading)

    val state: StateFlow<PagedConversationDetailState> = mutableState.asStateFlow()

    init {
        initializeConversationCursor()

        observePagerSettings().onEach {
            emitNewStateFor(PagedConversationDetailEvent.SettingsUpdated(it))
        }.launchIn(viewModelScope)

    }

    private fun observePagerSettings(): Flow<PagerSettings> = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            combine(
                observeSwipeNextPreference(userId),
                observeAutoAdvanceSetting(userId)
            ) { swipeEnabled, autoAdvanceEnabled ->
                PagerSettings(
                    swipeEnabled = swipeEnabled,
                    autoAdvanceEnabled = autoAdvanceEnabled
                )
            }
        }
        .distinctUntilChanged()

    private fun observeCursorInitializationParams(): Flow<CursorInitializationParams> = combine(
        observePrimaryUserId().filterNotNull().distinctUntilChanged(),
        observePagerSettings()
    ) { userId, settings ->
        CursorInitializationParams(
            userId = userId,
            conversationId = requireConversationId(),
            pagerSettings = settings
        )
    }.distinctUntilChanged()


    private fun initializeConversationCursor() {
        viewModelScope.launch {
            observeCursorInitializationParams()
                .first()
                .let { params ->

                    val singleMessageModePreferred = resolveSingleMessageModePreferred(params.userId)

                    getConversationCursor(
                        userId = params.userId,
                        conversationId = params.conversationId,
                        messageId = getInitialScrollToMessageId()?.id,
                        locationViewModeIsConversation = requireLocationViewModeModeIsConversation(),
                        labelId = requireLabelId()
                    ).fold(
                        ifLeft = { error ->
                            Timber.e(
                                "Failed to get conversation cursor for conversation " +
                                    "${params.conversationId} with error $error"
                            )
                            emitNewStateFor(PagedConversationDetailEvent.Error(error))
                            _effects.value = PagedConversationEffects(Effect.of(UiError.OTHER))
                        },
                        ifRight = { cursor ->
                            cursorMutex.withLock {
                                conversationCursor = cursor
                            }

                            emitNewStateFor(
                                PagedConversationDetailEvent.Ready(
                                    pagerSettings = params.pagerSettings,
                                    currentItem = cursor.current.toPage(),
                                    nextItem = cursor.next.toPage(),
                                    previousItem = cursor.previous.toPage(),
                                    navigationArgs = NavigationArgs(
                                        openedFromLocation = requireLabelId(),
                                        singleMessageMode = singleMessageModePreferred,
                                        conversationEntryPoint = getEntryPoint()
                                    )
                                )
                            )
                        }
                    )
                }
        }
    }

    private suspend fun resolveSingleMessageModePreferred(userId: UserId): Boolean {
        return when (requireConversationOpenMode()) {
            ConversationOpenMode.Message -> true
            ConversationOpenMode.Conversation -> false
            ConversationOpenMode.UseUserPreference -> observeIsSingleMessageViewModePreferred(userId).first()
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

    override fun onCleared() {
        Timber.d("ViewModel cleared, clearing conversation cursor for conversation ${requireConversationId()}")

        appScope.launch {
            clearMessageBodyCache()

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

    private data class CursorInitializationParams(
        val userId: UserId,
        val conversationId: ConversationId,
        val pagerSettings: PagerSettings
    )
}
