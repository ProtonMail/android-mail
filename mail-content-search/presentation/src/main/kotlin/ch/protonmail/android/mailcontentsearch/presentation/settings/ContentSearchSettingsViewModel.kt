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

package ch.protonmail.android.mailcontentsearch.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearContentSearchLocalData
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingSweep
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Data
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Error
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isActive
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isTerminal
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.toPercentage
import ch.protonmail.android.mailcontentsearch.presentation.settings.reducer.ContentSearchSettingsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ContentSearchSettingsViewModel @Inject constructor(
    private val reducer: ContentSearchSettingsReducer,
    private val isContentSearchEnabled: IsContentSearchEnabled,
    private val enableContentSearch: EnableContentSearch,
    private val disableContentSearch: DisableContentSearch,
    private val startContentIndexingSweep: StartContentIndexingSweep,
    private val clearContentSearchLocalData: ClearContentSearchLocalData,
    private val observeContentIndexingState: ObserveContentIndexingState,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val observeContentSearchIndexingStatus: ObserveContentSearchIndexingStatus,
    private val isContentSearchAllowedOnMobileData: IsContentSearchAllowedOnMobileData,
    private val setAllowContentSearchOnMobileData: SetAllowContentSearchOnMobileData,
    private val observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow<ContentSearchSettingsState>(ContentSearchSettingsState.Loading)
    val state: StateFlow<ContentSearchSettingsState> = mutableState.asStateFlow()

    private val rescheduleRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private val actions = Channel<ContentSearchSettingsViewAction>(Channel.BUFFERED)

    init {
        actions.receiveAsFlow()
            .onEach { handle(it) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val userId = currentUserId()
            if (loadInitialState(userId)) {
                observeIndexingProgress(userId)
                observeEnabledChanges(userId)
                observeRescheduleRequests()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeRescheduleRequests() {
        rescheduleRequests
            .debounce(RescheduleDebounceMillis.milliseconds)
            .onEach {
                if (isContentSearchCurrentlyEnabled()) startContentIndexingSweep()
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadInitialState(userId: UserId): Boolean = isContentSearchEnabled(userId).fold(
        ifLeft = {
            emitNewStateFor(Error.LoadingError)
            false
        },
        ifRight = { enabled ->
            emitNewStateFor(
                Data.ContentLoaded(
                    isContentSearchEnabled = enabled,
                    isAllowMobileDataEnabled = isContentSearchAllowedOnMobileData()
                )
            )
            true
        }
    )

    private fun observeIndexingProgress(userId: UserId) {
        combine(
            observeContentSearchEnabled(userId),
            observeContentSearchIndexingStatus(userId),
            observeContentIndexingState(userId)
        ) { enabled, indexingStatus, workerState ->
            if (!enabled) {
                Data.IndexingProgress(percentage = null, isActive = false)
            } else {
                // The worker's Initializing state covers the window before Rust streams progress.
                // Ignore it once Rust reports the account complete, so a completed account never shows
                // "preparing" while a sweep for another account is starting up.
                val preparing = workerState == ContentIndexingState.Initializing &&
                    indexingStatus !is ContentIndexingState.Completed
                Data.IndexingProgress(
                    percentage = indexingStatus.toPercentage(),
                    isActive = indexingStatus.isActive() || preparing
                )
            }
        }
            .onEach { emitNewStateFor(it) }
            .launchIn(viewModelScope)
    }

    private fun observeEnabledChanges(userId: UserId) {
        observeContentSearchEnabled(userId)
            .onEach { enabled -> emitNewStateFor(Data.ContentSearchToggled(enabled)) }
            .launchIn(viewModelScope)
    }

    fun submit(action: ContentSearchSettingsViewAction) {
        actions.trySend(action)
    }

    private suspend fun handle(action: ContentSearchSettingsViewAction) {
        when (action) {
            is ContentSearchSettingsViewAction.ToggleContentSearch -> handleToggleContentSearch(action.enabled)
            is ContentSearchSettingsViewAction.ToggleAllowMobileData -> handleToggleAllowMobileData(action.enabled)
            ContentSearchSettingsViewAction.ClearLocalData -> handleClearLocalData()
        }
    }

    private suspend fun handleToggleContentSearch(newValue: Boolean) {
        val userId = currentUserId()
        val result = if (newValue) {
            // Enable the account first so it is eligible, then (re)start the sweep. The sweep indexes
            // every enabled account in turn, so enabling one account never blocks another.
            enableContentSearch(userId).onRight { startContentIndexingSweep() }
        } else {
            disableContentSearch(userId)
        }
        result.fold(
            ifLeft = { emitNewStateFor(Error.UpdateError) },
            ifRight = { emitNewStateFor(Data.ContentSearchToggled(newValue)) }
        )
    }

    private suspend fun handleToggleAllowMobileData(newValue: Boolean) {
        emitNewStateFor(Data.AllowMobileDataToggled(newValue))
        setAllowContentSearchOnMobileData(newValue)
        rescheduleRequests.tryEmit(Unit)
    }

    private suspend fun handleClearLocalData() {
        val userId = currentUserId()
        disableContentSearch(userId).onLeft {
            emitNewStateFor(Error.UpdateError)
            return
        }
        observeContentIndexingState(userId).first { it.isTerminal() }
        clearContentSearchLocalData(userId).onLeft {
            emitNewStateFor(Error.UpdateError)
            return
        }
        emitNewStateFor(Data.LocalSearchDataCleared)
    }

    private suspend fun currentUserId(): UserId = observePrimaryUserId().filterNotNull().first()

    private fun isContentSearchCurrentlyEnabled(): Boolean =
        (mutableState.value as? ContentSearchSettingsState.Data)?.isContentSearchEnabled == true

    private fun emitNewStateFor(event: ContentSearchSettingsEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }

    private companion object {

        const val RescheduleDebounceMillis = 500L
    }
}
