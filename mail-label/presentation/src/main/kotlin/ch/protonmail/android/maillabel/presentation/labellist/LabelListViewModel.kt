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
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LabelListViewModel @Inject constructor(
    private val observeLabels: ObserveLabels,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()

    val initialState = LabelListState.Loading

    val state: StateFlow<LabelListState> =
        observeMailLabels()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
                initialValue = initialState
            )

    private fun observeMailLabels(): Flow<LabelListState> {
        return primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(LabelListState.EmptyLabelList)
            } else {
                observeLabels(userId).map { labels ->
                    val customMailLabels = labels.onLeft {
                        Timber.e("Error while observing custom labels")
                    }.getOrElse { emptyList() }
                    if (customMailLabels.isEmpty()) {
                        LabelListState.EmptyLabelList
                    } else {
                        LabelListState.Data(customMailLabels)
                    }
                }
            }
        }
    }
}
