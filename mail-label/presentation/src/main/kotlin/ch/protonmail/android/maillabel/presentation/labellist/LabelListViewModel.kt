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

package ch.protonmail.android.maillabel.presentation.labellist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LabelListViewModel @Inject constructor(
    private val observeLabels: ObserveLabels,
    private val reducer: LabelListReducer,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val initialState: LabelListState = LabelListState.Loading()
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<LabelListState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            flowLabelListEvent(userId = primaryUserId())
                .onEach { labelListEvent -> emitNewStateFor(labelListEvent) }
                .launchIn(viewModelScope)
        }
    }

    private fun flowLabelListEvent(userId: UserId): Flow<LabelListEvent> {
        return observeLabels(userId, LabelType.MessageLabel).map { labels ->
            LabelListEvent.LabelListLoaded(
                labelList = labels.getOrElse {
                    Timber.e("Error while observing custom labels")
                    return@map LabelListEvent.ErrorLoadingLabelList
                }.filter { !it.labelId.isReservedSystemLabelId() }
            )
        }
    }

    internal fun submit(action: LabelListViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    LabelListViewAction.OnAddLabelClick -> emitNewStateFor(LabelListEvent.OpenLabelForm)
                }
            }
        }
    }

    private fun emitNewStateFor(event: LabelListEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    private suspend fun primaryUserId() = primaryUserId.first()
}
