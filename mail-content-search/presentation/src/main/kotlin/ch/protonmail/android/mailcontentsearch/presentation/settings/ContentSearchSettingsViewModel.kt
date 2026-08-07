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
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearContentSearchLocalData
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Data
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Error
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isActive
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isWaitingForUnmeteredConnection
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.toPercentage
import ch.protonmail.android.mailcontentsearch.presentation.settings.reducer.ContentSearchSettingsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@HiltViewModel
class ContentSearchSettingsViewModel @Inject constructor(
    private val reducer: ContentSearchSettingsReducer,
    private val isContentSearchEnabled: IsContentSearchEnabled,
    private val enableContentSearch: EnableContentSearch,
    private val disableContentSearch: DisableContentSearch,
    private val startContentIndexingForUser: StartContentIndexingForUser,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val clearContentSearchLocalData: ClearContentSearchLocalData,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val observeContentSearchIndexingStatus: ObserveContentSearchIndexingStatus,
    private val isContentSearchAllowedOnMobileData: IsContentSearchAllowedOnMobileData,
    private val setAllowContentSearchOnMobileData: SetAllowContentSearchOnMobileData,
    private val observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow<ContentSearchSettingsState>(ContentSearchSettingsState.Loading)
    val state: StateFlow<ContentSearchSettingsState> = mutableState.asStateFlow()

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
            }
        }
    }

    // Gated on the SDK's availability answer: without it the toggle renders, accepts a tap and
    // then fails against a feature Rust will not run.
    private suspend fun loadInitialState(userId: UserId): Boolean {
        if (!isContentSearchFeatureEnabled(userId)) {
            emitNewStateFor(Error.LoadingError)
            return false
        }
        return readEnabledState(userId)
    }

    private suspend fun readEnabledState(userId: UserId): Boolean = isContentSearchEnabled(userId).fold(
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

    // Rust is the single source of progress now: the status stream is seeded from the per-user
    // snapshot, so there is nothing left to cross-reference against a worker-derived state.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeIndexingProgress(userId: UserId) {
        observeContentSearchEnabled(userId)
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (!enabled) {
                    flowOf(
                        Data.IndexingProgress(
                            percentage = null,
                            isActive = false,
                            isWaitingForUnmeteredConnection = false
                        )
                    )
                } else {
                    observeContentSearchIndexingStatus(userId).map { indexingStatus ->
                        Data.IndexingProgress(
                            percentage = indexingStatus.toPercentage(),
                            isActive = indexingStatus.isActive(),
                            isWaitingForUnmeteredConnection = indexingStatus.isWaitingForUnmeteredConnection()
                        )
                    }
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
            // Enable the account, then hand it to the orchestrator directly: otherwise it is only
            // picked up whenever the orchestrator next goes looking for a candidate.
            enableContentSearch(userId).onRight { startContentIndexingForUser(userId) }
        } else {
            // No stop: it is session-wide, and `find_next_suitable_user` skips a disabled account on
            // its own, so the other accounts keep indexing.
            disableContentSearch(userId)
        }
        result.fold(
            ifLeft = { emitNewStateFor(Error.UpdateError) },
            ifRight = { emitNewStateFor(Data.ContentSearchToggled(newValue)) }
        )
    }

    // No restart: the preference is written straight into Rust, which pauses and resumes its own
    // queue against the current network type.
    private suspend fun handleToggleAllowMobileData(newValue: Boolean) {
        emitNewStateFor(Data.AllowMobileDataToggled(newValue))
        setAllowContentSearchOnMobileData(newValue)
    }

    private suspend fun handleClearLocalData() {
        val userId = currentUserId()
        disableContentSearch(userId).onLeft {
            emitNewStateFor(Error.UpdateError)
            return
        }
        clearContentSearchLocalData(userId).onLeft {
            emitNewStateFor(Error.UpdateError)
            return
        }
        // Resetting wipes the index; disabling first tears down the progress observer. When the
        // account is re-enabled, observeIndexingProgress re-seeds isAccountIndexed from the (now
        // reset) Rust status, so the latch is never mutated from this coroutine.
        emitNewStateFor(Data.LocalSearchDataCleared)
    }

    private suspend fun currentUserId(): UserId = observePrimaryUserId().filterNotNull().first()

    private fun emitNewStateFor(event: ContentSearchSettingsEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
