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

package ch.protonmail.android.mailcontact.presentation.contactlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroupLabels
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModelMapper
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.usecase.GetUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val observeContacts: ObserveContacts,
    private val observeContactGroupLabels: ObserveContactGroupLabels,
    private val isPaidUser: IsPaidUser,
    private val reducer: ContactListReducer,
    private val contactListItemUiModelMapper: ContactListItemUiModelMapper,
    private val contactGroupItemUiModelMapper: ContactGroupItemUiModelMapper,
    private val observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val getUpsellingVisibility: GetUpsellingVisibility,
    private val userUpgradeState: UserUpgradeState,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState: MutableStateFlow<ContactListState> = MutableStateFlow(ContactListState.Loading())
    val state: StateFlow<ContactListState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            flowContactListEvent(userId = primaryUserId())
                .onEach { contactListEvent -> emitNewStateFor(contactListEvent) }
                .launchIn(viewModelScope)
        }
    }

    internal fun submit(action: ContactListViewAction) {
        viewModelScope.launch {
            when (action) {
                ContactListViewAction.OnOpenBottomSheet -> emitNewStateFor(ContactListEvent.OpenBottomSheet)
                ContactListViewAction.OnOpenContactSearch -> emitNewStateFor(ContactListEvent.OpenContactSearch)
                ContactListViewAction.OnDismissBottomSheet -> emitNewStateFor(ContactListEvent.DismissBottomSheet)
                ContactListViewAction.OnNewContactClick -> emitNewStateFor(ContactListEvent.OpenContactForm)
                ContactListViewAction.OnNewContactGroupClick -> handleOnNewContactGroupClick()
                ContactListViewAction.OnImportContactClick -> emitNewStateFor(ContactListEvent.OpenImportContact)
            }
        }
    }

    private suspend fun handleOnNewContactGroupClick() {
        if (userUpgradeState.isUserPendingUpgrade) return emitNewStateFor(ContactListEvent.UpsellingInProgress)

        val shouldShowUpselling = getUpsellingVisibility(UpsellingEntryPoint.Feature.ContactGroups)
        if (shouldShowUpselling) {
            emitNewStateFor(ContactListEvent.OpenUpsellingBottomSheet)
        } else {
            val isPaid = isPaidUser(primaryUserId()).getOrElse { false }

            if (isPaid) {
                emitNewStateFor(ContactListEvent.OpenContactGroupForm)
            } else {
                emitNewStateFor(ContactListEvent.SubscriptionUpgradeRequiredError)
            }
        }
    }

    private fun flowContactListEvent(userId: UserId): Flow<ContactListEvent> {
        return combine(
            observeContacts(userId),
            observeContactGroupLabels(userId),
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.ContactGroups)
        ) { contacts, contactGroups, isContactGroupsUpsellingVisible ->
            val contactList = contacts.getOrElse {
                Timber.e("Error while observing contacts")
                return@combine ContactListEvent.ErrorLoadingContactList
            }
            ContactListEvent.ContactListLoaded(
                contactList = contactListItemUiModelMapper.toContactListItemUiModel(
                    contactList
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    contactList,
                    contactGroups.getOrElse {
                        Timber.e("Error while observing contact groups")
                        return@combine ContactListEvent.ErrorLoadingContactList
                    }
                ),
                isContactGroupsUpsellingVisible = isContactGroupsUpsellingVisible
            )
        }
    }

    private fun emitNewStateFor(event: ContactListEvent) {
        val currentState = state.value
        mutableState.update { reducer.newStateFrom(currentState, event) }
    }

    private suspend fun primaryUserId() = primaryUserId.first()
}
