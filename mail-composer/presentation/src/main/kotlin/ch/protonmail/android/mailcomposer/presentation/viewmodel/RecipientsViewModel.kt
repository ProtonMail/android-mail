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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.repository.ContactsPermissionRepository
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.usecase.SortContactsForSuggestions
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel.Companion.maxContactAutocompletionCount
import ch.protonmail.android.mailcontact.domain.usecase.SearchContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = RecipientsViewModel.Factory::class)
internal class RecipientsViewModel @AssistedInject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val searchContacts: SearchContacts,
    private val searchContactGroups: SearchContactGroups,
    private val searchDeviceContacts: SearchDeviceContacts,
    private val sortContactsForSuggestions: SortContactsForSuggestions,
    private val contactsPermissionRepository: ContactsPermissionRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @Assisted val recipientsStateManager: RecipientsStateManager
) : ViewModel() {

    private val searchTerm = MutableStateFlow("")

    private val mutableContactSuggestionsFieldFlow = MutableStateFlow<ContactSuggestionsField?>(null)
    val contactSuggestionsFieldFlow = mutableContactSuggestionsFieldFlow.asStateFlow()

    val contactsSuggestions = observeContactsSuggestions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<ContactSuggestionUiModel>()
    )

    val contactsPermissionDenied = contactsPermissionRepository.observePermissionDenied()
        .map { it.getOrNull() == true }

    fun denyContactsPermission() {
        viewModelScope.launch { contactsPermissionRepository.trackPermissionDenied() }
    }

    fun updateSearchTerm(term: String, contactSuggestionsField: ContactSuggestionsField) {
        searchTerm.update { term }
        mutableContactSuggestionsFieldFlow.update { contactSuggestionsField }
    }

    fun updateRecipients(values: List<RecipientUiModel>, type: ContactSuggestionsField) {
        recipientsStateManager.updateRecipients(values, type)
    }

    fun closeSuggestions() {
        mutableContactSuggestionsFieldFlow.update { null }
    }

    @OptIn(FlowPreview::class)
    private fun observeContactsSuggestions() = combine(
        primaryUserId(),
        searchTerm
    ) { userId, searchTerm ->
        userId to searchTerm
    }
        .flatMapLatest { (userId, searchTerm) ->
            if (searchTerm.isBlank()) return@flatMapLatest flowOf(emptyList<ContactSuggestionUiModel>())

            combine(
                searchContacts(userId, searchTerm, onlyMatchingContactEmails = true),
                searchContactGroups(userId, searchTerm)
            ) { contactsResult, contactGroupsResult ->
                val deviceContacts = withContext(defaultDispatcher) {
                    searchDeviceContacts(searchTerm).getOrNull() ?: emptyList()
                }

                sortContactsForSuggestions(
                    contactsResult.getOrNull() ?: emptyList(),
                    deviceContacts,
                    contactGroupsResult.getOrNull() ?: emptyList(),
                    maxContactAutocompletionCount
                )
            }.debounce(SuggestionsDebounce)
        }

    private fun primaryUserId() = observePrimaryUserId.invoke().filterNotNull()

    @AssistedFactory
    interface Factory {

        fun create(recipientsStateManager: RecipientsStateManager): RecipientsViewModel
    }

    private companion object {

        val SuggestionsDebounce = 200.milliseconds
    }
}
