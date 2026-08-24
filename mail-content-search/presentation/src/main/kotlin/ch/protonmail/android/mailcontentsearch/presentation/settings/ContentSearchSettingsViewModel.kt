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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartOutcome
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearContentSearchLocalData
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Data
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsEvent.Error
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isActive
import ch.protonmail.android.mailcontentsearch.presentation.settings.mapper.isFailed
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
import timber.log.Timber
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
    private val getContentSearchIndexingStatus: GetContentSearchIndexingStatus,
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
            val isEnabled = loadInitialState(userId) ?: return@launch
            if (isEnabled) readIndexingStatus(userId)
            observeIndexingProgress(userId)
            observeEnabledChanges(userId)
        }
    }

    private suspend fun loadInitialState(userId: UserId): Boolean? {
        if (!isContentSearchFeatureEnabled(userId)) {
            emitNewStateFor(Error.LoadingError)
            return null
        }
        return readEnabledState(userId)
    }

    private suspend fun readEnabledState(userId: UserId): Boolean? = isContentSearchEnabled(userId).fold(
        ifLeft = {
            emitNewStateFor(Error.LoadingError)
            null
        },
        ifRight = { enabled ->
            emitNewStateFor(
                Data.ContentLoaded(
                    isContentSearchEnabled = enabled,
                    isAllowMobileDataEnabled = isContentSearchAllowedOnMobileData()
                )
            )
            enabled
        }
    )

    private suspend fun readIndexingStatus(userId: UserId) {
        emitNewStateFor(getContentSearchIndexingStatus(userId).toIndexingProgress())
    }

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
                            isWaitingForUnmeteredConnection = false,
                            isFailed = false
                        )
                    )
                } else {
                    observeContentSearchIndexingStatus(userId).map { it.toIndexingProgress() }
                }
            }
            .onEach { emitNewStateFor(it) }
            .launchIn(viewModelScope)
    }

    private fun observeEnabledChanges(userId: UserId) {
        observeContentSearchEnabled(userId)
            .distinctUntilChanged()
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
            ContentSearchSettingsViewAction.RetryIndexing -> handleRetryIndexing()
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

    /**
     * Hands the account back to the orchestrator, which is the only way out of a failure: Rust holds
     * the account in a failed state until something starts it again, and never retries on its own.
     */
    private suspend fun handleRetryIndexing() {
        val userId = currentUserId()

        emitNewStateFor(Data.IndexingRetryStarted)
        startContentIndexingForUser(userId).fold(
            ifLeft = { emitNewStateFor(Error.IndexingRetryFailed) },
            ifRight = { outcome ->
                when (outcome) {
                    // Rust reports the account as syncing from here on and [observeIndexingProgress]
                    // is subscribed to exactly that, but the stream takes a moment to catch up and
                    // the card would sit on the failure meanwhile, as if the tap had done nothing.
                    ContentIndexingStartOutcome.Started,
                    ContentIndexingStartOutcome.AlreadyRunning -> emitNewStateFor(Data.IndexingRetryAccepted)

                    // Rust cleared the recorded failure on its way to answering this, but has nothing
                    // to publish for an account with no work left, so the stream stays silent. Read
                    // the state back, or the card sits on a failure Rust no longer has.
                    ContentIndexingStartOutcome.AlreadyCompleted -> readIndexingStatus(userId)

                    // The one outcome that leaves the account exactly as it was, failure and all:
                    // content search is off for it, or Rust is holding its runtime lock.
                    ContentIndexingStartOutcome.Refused -> {
                        Timber.w("content-search: the orchestrator refused to retry $userId")
                        emitNewStateFor(Error.IndexingRetryFailed)
                    }
                }
            }
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

    private fun ContentIndexingState.toIndexingProgress() = Data.IndexingProgress(
        percentage = toPercentage(),
        isActive = isActive(),
        isWaitingForUnmeteredConnection = isWaitingForUnmeteredConnection(),
        isFailed = isFailed()
    )

    private suspend fun currentUserId(): UserId = observePrimaryUserId().filterNotNull().first()

    private fun emitNewStateFor(event: ContentSearchSettingsEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
