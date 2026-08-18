/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearRecentSearchTerms
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissRecentSearchTerm
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetRecentFoundMailboxItems
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetRecentSearchTerms
import ch.protonmail.android.mailcontentsearch.domain.usecase.RecordSearchOpen
import ch.protonmail.android.mailcontentsearch.domain.usecase.TouchRecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.usecase.TouchRecentSearchTerm
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentFoundItemUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesState
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesViewAction
import ch.protonmail.android.mailcontentsearch.presentation.model.isInTrashOrSpam
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Owns the search-history sections of the content search screen (recent queries and previously found
 * items). Both sections share one view model because a single tap couples them: opening a result
 * upserts the query *and* the item, so the two lists always have to be re-read together.
 *
 * The Rust side exposes no change stream, so the state is re-read after every mutation, whenever the
 * screen goes back to showing the history, and whenever the search field's text changes the filter the
 * history is read under.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@SuppressWarnings("LongParameterList")
class RecentSearchesViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val getRecentSearchTerms: GetRecentSearchTerms,
    private val getRecentFoundMailboxItems: GetRecentFoundMailboxItems,
    private val touchRecentSearchTerm: TouchRecentSearchTerm,
    private val dismissRecentSearchTerm: DismissRecentSearchTerm,
    private val clearRecentSearchTerms: ClearRecentSearchTerms,
    private val touchRecentFoundItem: TouchRecentFoundItem,
    private val recordSearchOpen: RecordSearchOpen,
    private val starMessages: StarMessages,
    private val unStarMessages: UnStarMessages,
    private val itemMapper: MailboxItemUiModelMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val mutableState = MutableStateFlow<RecentSearchesState>(RecentSearchesState.Loading)
    val state: StateFlow<RecentSearchesState> = mutableState.asStateFlow()

    /** The search field's text, which every read of the history is filtered by. */
    private val queryPrefix = MutableStateFlow("")

    init {
        // The starting value isn't an edit — the first read is the [Refresh] the screen submits when it
        // shows the history — and [mapLatest] keeps a read started on an earlier keystroke from landing
        // after the one that replaced it.
        queryPrefix
            .drop(1)
            .mapLatest { reload() }
            .launchIn(viewModelScope)
    }

    fun submit(action: RecentSearchesViewAction) {
        when (action) {
            RecentSearchesViewAction.Refresh -> onRefresh()
            is RecentSearchesViewAction.QueryChanged -> onQueryChanged(action.query)
            is RecentSearchesViewAction.TermClicked -> onTermClicked(action.query)
            is RecentSearchesViewAction.TermDismissed -> onTermDismissed(action.query)
            RecentSearchesViewAction.ClearTerms -> onClearTerms()
            is RecentSearchesViewAction.FoundItemClicked -> onFoundItemClicked(action.item)
            is RecentSearchesViewAction.FoundItemStarClicked -> onFoundItemStarClicked(action.item)
            is RecentSearchesViewAction.SearchResultOpened ->
                onSearchResultOpened(action.query, action.messageId, action.conversationId)
        }
    }

    /** Re-reads the history. Safe to submit repeatedly: it never drops back to the loading state. */
    private fun onRefresh() = reloadAfter { }

    // A prefix that trims down to what is already in force leaves the history alone: the same rows would
    // come back, and re-reading them would only make the list flicker while the user keeps typing.
    private fun onQueryChanged(query: String) {
        queryPrefix.value = query.trim()
    }

    private fun onTermClicked(query: String) = reloadAfter { userId -> touchRecentSearchTerm(userId, query) }

    private fun onTermDismissed(query: String) {
        // Drop the row locally so it disappears on tap; the reload can then pull an older query into
        // the slot it freed.
        updateData { it.copy(terms = it.terms.filterNot { term -> term == query }.toImmutableList()) }
        reloadAfter { userId -> dismissRecentSearchTerm(userId, query) }
    }

    private fun onClearTerms() {
        updateData { it.copy(terms = persistentListOf()) }
        reloadAfter { userId -> clearRecentSearchTerms(userId) }
    }

    private fun onFoundItemClicked(item: RecentFoundItemUiModel) =
        reloadAfter { userId -> touchRecentFoundItem(userId, item.messageId) }

    private fun onFoundItemStarClicked(item: RecentFoundItemUiModel) = reloadAfter { userId ->
        val messageIds = listOf(item.messageId)
        if (item.item.isStarred) unStarMessages(userId, messageIds) else starMessages(userId, messageIds)
    }

    private fun onSearchResultOpened(
        query: String,
        messageId: MessageId,
        conversationId: ConversationId
    ) = reloadAfter { userId -> recordSearchOpen(userId, query, messageId, conversationId) }

    /** Runs [block] against the primary user, then re-reads the history it may have changed. */
    private fun reloadAfter(block: suspend (UserId) -> Unit) {
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            block(userId)
            reload()
        }
    }

    private suspend fun reload() {
        val userId = observePrimaryUserId().filterNotNull().first()
        val prefix = queryPrefix.value
        val history = readHistory(userId, prefix)
        // A read only publishes while the filter it ran under is still the current one, so a slower read
        // started under an older prefix can't land on top of the one that replaced it.
        if (queryPrefix.value == prefix) mutableState.value = collapseIfEmpty(history)
    }

    private suspend fun readHistory(userId: UserId, prefix: String): RecentSearchesState.Data {
        val terms = getRecentSearchTerms(userId, prefix.ifBlank { null })
            .getOrElse { emptyList() }
            .map { it.query }

        // Previously found items (mailbox items) are only shown if the search field is empty.
        val foundItems = if (prefix.isBlank()) {
            getRecentFoundMailboxItems(userId).getOrElse { emptyList() }
        } else {
            emptyList()
        }

        val mappedFoundItems = if (foundItems.isEmpty()) {
            emptyList()
        } else {
            val folderColorSettings = observeFolderColorSettings(userId).first()
            withContext(dispatchers.Comp) {
                foundItems.map { found ->
                    RecentFoundItemUiModel(
                        item = itemMapper.toUiModel(
                            userId = userId,
                            mailboxItem = found.item,
                            folderColorSettings = folderColorSettings,
                            isShowingSearchResults = true
                        ),
                        searchQuery = found.searchQuery,
                        isInTrashOrSpam = found.item.isInTrashOrSpam()
                    )
                }
            }
        }

        return RecentSearchesState.Data(
            terms = terms.toImmutableList(),
            foundItems = mappedFoundItems.toImmutableList()
        )
    }

    private fun updateData(transform: (RecentSearchesState.Data) -> RecentSearchesState.Data) {
        mutableState.update { current ->
            if (current is RecentSearchesState.Data) collapseIfEmpty(transform(current)) else current
        }
    }

    /**
     * Dismissing the last entry has to fall through to the generic empty state rather than leave an empty
     * history page behind.
     *
     * A filter matching nothing is not that state and stays [RecentSearchesState.Data]: emptying the field
     * makes the screen eligible for the first-run state again before the unfiltered read comes back, so a
     * filtered miss collapsed to [RecentSearchesState.Empty] would flash it on the way out.
     */
    private fun collapseIfEmpty(data: RecentSearchesState.Data): RecentSearchesState =
        if (queryPrefix.value.isBlank() && data.terms.isEmpty() && data.foundItems.isEmpty()) {
            RecentSearchesState.Empty
        } else {
            data
        }
}
