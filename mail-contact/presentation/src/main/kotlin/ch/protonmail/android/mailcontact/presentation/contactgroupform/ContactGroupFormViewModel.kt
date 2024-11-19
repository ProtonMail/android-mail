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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.ColorHexWithName
import ch.protonmail.android.mailcommon.presentation.usecase.GetColorHexWithNameList
import ch.protonmail.android.mailcontact.domain.usecase.CreateContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.CreateContactGroupError
import ch.protonmail.android.mailcontact.domain.usecase.DeleteContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.EditContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.GetContactEmailsById
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyContactGroupFormUiModel
import ch.protonmail.android.maillabel.domain.model.ColorRgbHex
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactGroupFormViewModel @Inject constructor(
    private val observeContactGroup: ObserveContactGroup,
    private val getContactEmailsById: GetContactEmailsById,
    private val reducer: ContactGroupFormReducer,
    private val contactGroupFormUiModelMapper: ContactGroupFormUiModelMapper,
    private val savedStateHandle: SavedStateHandle,
    private val createContactGroup: CreateContactGroup,
    private val editContactGroup: EditContactGroup,
    private val deleteContactGroup: DeleteContactGroup,
    private val colorMapper: ColorMapper,
    private val isPaidMailUser: IsPaidMailUser,
    getColorHexWithNameList: GetColorHexWithNameList,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactGroupFormState> = mutableState

    init {
        val colors = getColorHexWithNameList()
        extractLabelId()?.let { labelId ->
            viewModelScope.launch {
                emitNewStateFor(
                    getContactGroupFormEvent(userId = primaryUserId(), labelId = LabelId(labelId), colors = colors)
                )
            }
        } ?: run {
            emitNewStateFor(
                ContactGroupFormEvent.ContactGroupLoaded(
                    contactGroupFormUiModel = emptyContactGroupFormUiModel(
                        randomColor = colorMapper.toColor(colors.random().colorHex).getOrElse { Color.Black }
                    ),
                    colors = colors
                )
            )
        }
    }

    internal fun submit(action: ContactGroupFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is ContactGroupFormViewAction.OnUpdateMemberList -> handleOnUpdateMemberList(action)
                    is ContactGroupFormViewAction.OnRemoveMemberClick -> handleOnRemoveMemberClick(action)
                    is ContactGroupFormViewAction.OnUpdateName -> handleOnUpdateName(action)
                    is ContactGroupFormViewAction.OnUpdateColor -> handleOnUpdateColor(action)
                    ContactGroupFormViewAction.OnCloseClick -> emitNewStateFor(
                        ContactGroupFormEvent.Close
                    )

                    ContactGroupFormViewAction.OnSaveClick -> handleSave()
                    ContactGroupFormViewAction.OnDeleteClick -> handleShowDeleteDialog()
                    ContactGroupFormViewAction.OnDeleteConfirmedClick -> handleOnDeleteConfirmedClick()
                    ContactGroupFormViewAction.OnDeleteDismissedClick -> handleOnDeleteDismissedClick()
                }
            }
        }
    }

    private fun handleOnUpdateMemberList(action: ContactGroupFormViewAction.OnUpdateMemberList) {
        val stateValue = state.value
        if (stateValue !is ContactGroupFormState.Data) return

        if (action.selectedContactEmailIds.isEmpty()) {
            emitNewStateFor(
                ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                    contactGroupFormUiModel = stateValue.contactGroup.copy(memberCount = 0, members = emptyList())
                )
            )
        } else {
            viewModelScope.launch {
                val newContactEmailIds = action.selectedContactEmailIds.mapNotNull { contactEmailId ->
                    contactEmailId.takeIf {
                        stateValue.contactGroup.members.none { it.id.id == contactEmailId }
                    }
                }
                val newContactEmails = getContactEmailsById(primaryUserId(), newContactEmailIds).getOrElse {
                    Timber.e("Failed to get contact emails by id")
                    return@launch emitNewStateFor(ContactGroupFormEvent.UpdateMembersError)
                }
                val newContactMembers = contactGroupFormUiModelMapper.toContactGroupFormMemberList(newContactEmails)

                val updatedGroupMemberList = stateValue.contactGroup.members.mapNotNull { member ->
                    member.takeIf { action.selectedContactEmailIds.contains(it.id.id) }
                }.plus(newContactMembers)

                emitNewStateFor(
                    ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                        contactGroupFormUiModel = stateValue.contactGroup.copy(
                            memberCount = updatedGroupMemberList.size,
                            members = updatedGroupMemberList
                        )
                    )
                )
            }
        }
    }

    private fun handleOnRemoveMemberClick(action: ContactGroupFormViewAction.OnRemoveMemberClick) {
        val stateValue = state.value
        if (stateValue !is ContactGroupFormState.Data) return

        val newMembers = stateValue.contactGroup.members.toMutableList().apply {
            this.removeIf { it.id == action.contactEmailId }
        }
        emitNewStateFor(
            ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                contactGroupFormUiModel = stateValue.contactGroup.copy(
                    memberCount = newMembers.size,
                    members = newMembers
                )
            )
        )
    }

    private fun handleOnUpdateName(action: ContactGroupFormViewAction.OnUpdateName) {
        val stateValue = state.value
        if (stateValue !is ContactGroupFormState.Data) return

        emitNewStateFor(
            ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                contactGroupFormUiModel = stateValue.contactGroup.copy(
                    name = action.name
                )
            )
        )
    }

    private fun handleOnUpdateColor(action: ContactGroupFormViewAction.OnUpdateColor) {
        val stateValue = state.value
        if (stateValue !is ContactGroupFormState.Data) return

        emitNewStateFor(
            ContactGroupFormEvent.UpdateContactGroupFormUiModel(
                contactGroupFormUiModel = stateValue.contactGroup.copy(
                    color = action.color
                )
            )
        )
    }

    private fun handleShowDeleteDialog() {
        emitNewStateFor(
            ContactGroupFormEvent.ShowDeleteDialog
        )
    }

    private fun handleOnDeleteConfirmedClick() {
        val currentState = state.value
        if (currentState !is ContactGroupFormState.Data) return
        if (currentState.contactGroup.id == null) return

        viewModelScope.launch {
            deleteContactGroup(
                userId = primaryUserId(),
                labelId = currentState.contactGroup.id
            ).getOrElse {
                return@launch emitNewStateFor(ContactGroupFormEvent.DeletingError)
            }

            emitNewStateFor(ContactGroupFormEvent.DeletingSuccess)
        }
    }

    private fun handleOnDeleteDismissedClick() {
        val currentState = state.value
        if (currentState !is ContactGroupFormState.Data) return

        emitNewStateFor(ContactGroupFormEvent.DismissDeleteDialog)
    }

    private fun handleSave() {
        val stateValue = state.value
        if (stateValue !is ContactGroupFormState.Data) return

        viewModelScope.launch {
            stateValue.contactGroup.id?.let { labelId ->
                handleUpdateContactGroup(labelId, stateValue.contactGroup)
            } ?: handleCreateContactGroup(stateValue.contactGroup)
        }
    }

    private suspend fun handleCreateContactGroup(contactGroupFormUiModel: ContactGroupFormUiModel) {
        emitNewStateFor(ContactGroupFormEvent.SavingContactGroup)

        if (isPaidMailUser(primaryUserId()).getOrElse { false }) {
            createContactGroup(
                userId = primaryUserId(),
                name = contactGroupFormUiModel.name,
                color = ColorRgbHex(contactGroupFormUiModel.color.getHexStringFromColor()),
                contactEmailIds = contactGroupFormUiModel.members.map { it.id }
            ).getOrElse {
                return if (it is CreateContactGroupError.GroupNameDuplicate) {
                    emitNewStateFor(ContactGroupFormEvent.DuplicatedContactGroupName)
                } else {
                    emitNewStateFor(ContactGroupFormEvent.SaveContactGroupError)
                }
            }

            emitNewStateFor(ContactGroupFormEvent.ContactGroupCreated)
        } else {
            return emitNewStateFor(ContactGroupFormEvent.SubscriptionNeededError)
        }
    }

    private suspend fun handleUpdateContactGroup(labelId: LabelId, contactGroupFormUiModel: ContactGroupFormUiModel) {
        emitNewStateFor(ContactGroupFormEvent.SavingContactGroup)

        editContactGroup(
            userId = primaryUserId(),
            labelId = labelId,
            name = contactGroupFormUiModel.name,
            color = ColorRgbHex(contactGroupFormUiModel.color.getHexStringFromColor()),
            contactEmailIds = contactGroupFormUiModel.members.map { it.id }
        ).getOrElse {
            return emitNewStateFor(ContactGroupFormEvent.SaveContactGroupError)
        }

        emitNewStateFor(ContactGroupFormEvent.ContactGroupUpdated)
    }

    private suspend fun getContactGroupFormEvent(
        userId: UserId,
        labelId: LabelId,
        colors: List<ColorHexWithName>
    ): ContactGroupFormEvent {
        val contactGroup = observeContactGroup(userId, labelId).firstOrNull()?.getOrNull() ?: run {
            Timber.e("Error while observing contact group by id")
            return ContactGroupFormEvent.LoadError
        }
        return ContactGroupFormEvent.ContactGroupLoaded(
            contactGroupFormUiModelMapper.toContactGroupFormUiModel(
                contactGroup = contactGroup
            ),
            colors = colors
        )
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun extractLabelId() = savedStateHandle.get<String>(ContactGroupFormScreen.ContactGroupFormLabelIdKey)

    private fun emitNewStateFor(event: ContactGroupFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ContactGroupFormState = ContactGroupFormState.Loading()
    }
}
