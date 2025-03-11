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

package ch.protonmail.android.maillabel.presentation.folderform

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.domain.usecase.CreateFolder
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.domain.usecase.IsLabelLimitReached
import ch.protonmail.android.maillabel.domain.usecase.IsLabelNameAllowed
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.usecase.GetUpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.util.kotlin.equalsNoCase
import javax.inject.Inject

@HiltViewModel
class FolderFormViewModel @Inject constructor(
    private val getLabel: GetLabel,
    private val createFolder: CreateFolder,
    private val updateLabel: UpdateLabel,
    private val deleteLabel: DeleteLabel,
    private val getLabelColors: GetLabelColors,
    private val isLabelNameAllowed: IsLabelNameAllowed,
    private val isLabelLimitReached: IsLabelLimitReached,
    private val getUpsellingVisibility: GetUpsellingVisibility,
    private val userUpgradeState: UserUpgradeState,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val reducer: FolderFormReducer,
    private val colorMapper: ColorMapper,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialState: FolderFormState = FolderFormState.Loading()
    private val mutableState = MutableStateFlow<FolderFormState>(FolderFormState.Loading())
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<FolderFormState> = mutableState

    init {
        val labelId = savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        viewModelScope.launch {
            val colors = getLabelColors().map {
                colorMapper.toColor(it).getOrElse { Color.Black }
            }
            val folderColorSettings = observeFolderColorSettings(primaryUserId()).filterNotNull().first()
            if (labelId != null) {
                val label = getLabel(
                    userId = primaryUserId(),
                    labelId = LabelId(labelId),
                    labelType = LabelType.MessageFolder
                ).getOrNull() ?: return@launch emitNewStateFor(FolderFormEvent.LoadFolderError)
                val parent = label.parentId?.let { parentId ->
                    getLabel(
                        userId = primaryUserId(),
                        labelId = parentId,
                        labelType = LabelType.MessageFolder
                    ).getOrNull() ?: return@launch emitNewStateFor(FolderFormEvent.LoadFolderError)
                }
                emitNewStateFor(
                    FolderFormEvent.FolderLoaded(
                        labelId = label.labelId,
                        name = label.name,
                        color = label.color,
                        parent = parent,
                        notifications = label.isNotified ?: true,
                        colorList = colors,
                        useFolderColor = folderColorSettings.useFolderColor,
                        inheritParentFolderColor = folderColorSettings.inheritParentFolderColor
                    )
                )
            } else {
                emitNewStateFor(
                    FolderFormEvent.FolderLoaded(
                        labelId = null,
                        name = "",
                        color = colors.random().getHexStringFromColor(),
                        parent = null,
                        notifications = true,
                        colorList = colors,
                        useFolderColor = folderColorSettings.useFolderColor,
                        inheritParentFolderColor = folderColorSettings.inheritParentFolderColor
                    )
                )
            }
        }
    }

    internal fun submit(action: FolderFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is FolderFormViewAction.FolderColorChanged -> emitNewStateFor(
                        FolderFormEvent.UpdateFolderColor(action.color.getHexStringFromColor())
                    )

                    is FolderFormViewAction.FolderNameChanged -> emitNewStateFor(
                        FolderFormEvent.UpdateFolderName(action.name)
                    )

                    is FolderFormViewAction.FolderNotificationsChanged -> emitNewStateFor(
                        FolderFormEvent.UpdateFolderNotifications(action.enabled)
                    )

                    is FolderFormViewAction.FolderParentChanged -> handleFolderParentChanged(action.parentId)
                    FolderFormViewAction.OnCloseFolderFormClick -> emitNewStateFor(FolderFormEvent.CloseFolderForm)
                    FolderFormViewAction.OnDeleteRequested -> emitNewStateFor(FolderFormEvent.ShowDeleteDialog)
                    FolderFormViewAction.OnDeleteConfirmed -> handleOnDeleteClick()
                    FolderFormViewAction.OnDeleteCanceled -> emitNewStateFor(FolderFormEvent.HideDeleteDialog)
                    FolderFormViewAction.OnSaveClick -> handleOnSaveClick()
                    FolderFormViewAction.HideUpselling -> handleHideUpselling()
                }
            }
        }
    }

    private fun handleFolderParentChanged(parentId: LabelId?) {
        if (parentId == null) return emitNewStateFor(FolderFormEvent.UpdateFolderParent(null))
        viewModelScope.launch {
            emitNewStateFor(
                FolderFormEvent.UpdateFolderParent(
                    getLabel(
                        userId = primaryUserId(),
                        labelId = parentId,
                        labelType = LabelType.MessageFolder
                    ).getOrNull()
                )
            )
        }
    }

    private suspend fun handleOnSaveClick() {
        when (val currentState = state.value) {
            is FolderFormState.Data -> {
                val cleanName = currentState.name.trim()
                when (currentState) {
                    is FolderFormState.Data.Create -> {
                        createFolder(
                            cleanName,
                            currentState.color,
                            currentState.parent?.labelId,
                            currentState.notifications
                        )
                    }

                    is FolderFormState.Data.Update -> {
                        editFolder(
                            currentState.labelId,
                            cleanName,
                            currentState.color,
                            currentState.parent?.labelId,
                            currentState.notifications
                        )
                    }
                }
            }

            is FolderFormState.Loading -> {}
        }
    }

    private fun handleHideUpselling() {
        val currentState = state.value
        if (currentState is FolderFormState.Data.Create) {
            emitNewStateFor(FolderFormEvent.HideUpselling)
        }
    }

    private suspend fun handleOnDeleteClick() {
        val currentState = state.value
        if (currentState is FolderFormState.Data.Update) {
            deleteFolder(currentState.labelId)
        }
    }

    @SuppressWarnings("ReturnCount")
    private suspend fun createFolder(
        name: String,
        color: String,
        parentId: LabelId?,
        notifications: Boolean
    ) {
        emitNewStateFor(FolderFormEvent.CreatingFolder)

        val userId = primaryUserId()
        val isFolderLimitReached = isLabelLimitReached(userId, LabelType.MessageFolder).getOrElse {
            return emitNewStateFor(FolderFormEvent.SaveFolderError)
        }

        when {
            isFolderLimitReached && userUpgradeState.isUserPendingUpgrade -> {
                return emitNewStateFor(FolderFormEvent.UpsellingInProgress)
            }

            isFolderLimitReached -> {
                val shouldShowUpselling = getUpsellingVisibility(UpsellingEntryPoint.Feature.Folders)
                return if (shouldShowUpselling) {
                    emitNewStateFor(FolderFormEvent.ShowUpselling)
                } else emitNewStateFor(FolderFormEvent.FolderLimitReached)
            }

            else -> Unit
        }

        val isFolderNameAllowed = isLabelNameAllowed(userId, name, parentId).getOrElse {
            return emitNewStateFor(FolderFormEvent.SaveFolderError)
        }
        if (!isFolderNameAllowed) return emitNewStateFor(FolderFormEvent.FolderAlreadyExists)

        createFolder(userId, name, color, parentId, notifications).fold(ifLeft = {
            emitNewStateFor(FolderFormEvent.SaveFolderError)
        }, ifRight = {
            emitNewStateFor(FolderFormEvent.FolderCreated)
        })
    }

    @SuppressWarnings("ComplexCondition")
    private suspend fun editFolder(
        labelId: LabelId,
        name: String,
        color: String,
        parentId: LabelId?,
        notifications: Boolean
    ) {
        getLabel(primaryUserId(), labelId, LabelType.MessageFolder).getOrNull()?.let { label ->
            if (name.equalsNoCase(label.name) && color.equalsNoCase(label.color) &&
                parentId == label.parentId && notifications == label.isNotified
            ) {
                return emitNewStateFor(FolderFormEvent.CloseFolderForm)
            }
            if (!name.equalsNoCase(label.name)) {
                val isFolderNameAllowed = isLabelNameAllowed(primaryUserId(), name, parentId).getOrElse {
                    return emitNewStateFor(FolderFormEvent.SaveFolderError)
                }
                if (!isFolderNameAllowed) return emitNewStateFor(FolderFormEvent.FolderAlreadyExists)
            }
            updateLabel(
                primaryUserId(),
                label.copy(
                    name = name,
                    color = color,
                    parentId = parentId,
                    isNotified = notifications
                )
            )
        }
        emitNewStateFor(FolderFormEvent.FolderUpdated)
    }

    private suspend fun deleteFolder(labelId: LabelId) {
        deleteLabel(primaryUserId(), labelId, LabelType.MessageFolder)
        emitNewStateFor(FolderFormEvent.FolderDeleted)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: FolderFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
