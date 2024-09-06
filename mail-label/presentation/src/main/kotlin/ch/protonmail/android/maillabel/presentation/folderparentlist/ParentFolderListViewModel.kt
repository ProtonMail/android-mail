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

package ch.protonmail.android.maillabel.presentation.folderparentlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.maillabel.presentation.model.toFolderUiModel
import ch.protonmail.android.maillabel.presentation.model.toParentFolderUiModel
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ParentFolderListViewModel @Inject constructor(
    private val observeLabels: ObserveLabels,
    private val reducer: ParentFolderListReducer,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val colorMapper: ColorMapper,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    val initialState: ParentFolderListState = ParentFolderListState.Loading()
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<ParentFolderListState> = mutableState.asStateFlow()

    init {
        val labelId = savedStateHandle.get<String>(
            ParentFolderListScreen.ParentFolderListLabelIdKey
        )?.let { if (it == "null") null else LabelId(it) }
        val parentLabelId = savedStateHandle.get<String>(
            ParentFolderListScreen.ParentFolderListParentLabelIdKey
        )?.let { if (it == "null") null else LabelId(it) }
        viewModelScope.launch {
            flowFolderListEvent(userId = primaryUserId(), labelId, parentLabelId)
                .onEach { folderListEvent -> emitNewStateFor(folderListEvent) }
                .launchIn(viewModelScope)
        }
    }

    private fun flowFolderListEvent(
        userId: UserId,
        labelId: LabelId?,
        parentLabelId: LabelId?
    ): Flow<ParentFolderListEvent> {
        return combine(
            observeLabels(userId, LabelType.MessageFolder),
            observeFolderColorSettings(userId)
        ) { foldersResult, folderColorSettings ->
            val folders = foldersResult.getOrElse {
                Timber.e("Unable to fetch custom folders list.")
                return@combine ParentFolderListEvent.ErrorLoadingFolderList
            }.filter { !it.labelId.isReservedSystemLabelId() }

            ParentFolderListEvent.FolderListLoaded(
                folderList = folders.toFolderUiModel(
                    folderColorSettings,
                    colorMapper
                ).toParentFolderUiModel(labelId, parentLabelId),
                labelId = labelId,
                parentLabelId = parentLabelId,
                useFolderColor = folderColorSettings.useFolderColor,
                inheritParentFolderColor = folderColorSettings.inheritParentFolderColor
            )
        }
    }

    private fun emitNewStateFor(event: ParentFolderListEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    private suspend fun primaryUserId() = primaryUserId.first()
}
