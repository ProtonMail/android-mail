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
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearRecentFoundItems
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val clearRecentFoundItems: ClearRecentFoundItems,
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

    /**
     * Requests to re-read the recent terms/opened history.
     */
    private val reloadRequests = Channel<Unit>(Channel.CONFLATED)

    /**
     * The terms that were read but don't fit the list. Dismissing a row promotes the first of them into
     * the slot it freed, so a run of dismissals refills the list without a read for each one.
     */
    private var overflowTerms: List<String> = emptyList()

    /** Whether the store may hold more terms. True while the last read came back at its limit. */
    private var mayHaveMoreTerms = false

    init {
        // Both triggers feed one pipeline, so only one read is ever in flight.
        merge(queryPrefix.drop(1).map { }, reloadRequests.receiveAsFlow())
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
            RecentSearchesViewAction.ClearFoundItems -> onClearFoundItems()
            is RecentSearchesViewAction.SearchResultOpened ->
                onSearchResultOpened(action.query, action.messageId, action.conversationId)
        }
    }

    /** Re-reads the history. Safe to submit repeatedly: it never drops back to the loading state. */
    private fun onRefresh() = requestReload()

    // A prefix that trims down to what is already in force leaves the history alone: the same rows would
    // come back, and re-reading them would only make the list flicker while the user keeps typing.
    private fun onQueryChanged(query: String) {
        queryPrefix.value = query.trim()
    }

    private fun onTermClicked(query: String) = reloadAfter { userId -> touchRecentSearchTerm(userId, query) }

    /**
     * Drops the row locally so it disappears on tap, and fills the slot it freed from the buffered terms
     * the last read left over. Only a dismissal that empties the buffer pays for a re-read.
     */
    private fun onTermDismissed(query: String) {
        val remainingOverflow = overflowTerms.filterNot { term -> term == query }
        val wasOnScreen = (mutableState.value as? RecentSearchesState.Data)?.terms?.contains(query) == true
        val promoted = if (wasOnScreen) remainingOverflow.firstOrNull() else null

        updateData { data ->
            val kept = data.terms.filterNot { term -> term == query }
            data.copy(terms = (kept + listOfNotNull(promoted)).toImmutableList())
        }

        overflowTerms = if (promoted != null) remainingOverflow.drop(1) else remainingOverflow

        onPrimaryUser { userId ->
            val dismissSuccessful = dismissRecentSearchTerm(userId, query).isRight()
            if (!dismissSuccessful || overflowTerms.isEmpty() && mayHaveMoreTerms) requestReload()
        }
    }

    private fun onClearTerms() {
        overflowTerms = emptyList()
        mayHaveMoreTerms = false
        updateData { it.copy(terms = persistentListOf()) }
        reloadAfter { userId -> clearRecentSearchTerms(userId) }
    }

    private fun onFoundItemClicked(item: RecentFoundItemUiModel) =
        reloadAfter { userId -> touchRecentFoundItem(userId, item.messageId) }

    private fun onFoundItemStarClicked(item: RecentFoundItemUiModel) = reloadAfter { userId ->
        val messageIds = listOf(item.messageId)
        if (item.item.isStarred) unStarMessages(userId, messageIds) else starMessages(userId, messageIds)
    }

    private fun onClearFoundItems() {
        updateData { it.copy(foundItems = persistentListOf()) }
        reloadAfter { userId -> clearRecentFoundItems(userId) }
    }

    private fun onSearchResultOpened(
        query: String,
        messageId: MessageId,
        conversationId: ConversationId
    ) = reloadAfter { userId -> recordSearchOpen(userId, query, messageId, conversationId) }

    /** Runs [block] against the primary user, then re-reads the history it may have changed. */
    private fun reloadAfter(block: suspend (UserId) -> Unit) = onPrimaryUser { userId ->
        block(userId)
        requestReload()
    }

    private fun onPrimaryUser(block: suspend (UserId) -> Unit) {
        viewModelScope.launch {
            block(observePrimaryUserId().filterNotNull().first())
        }
    }

    private fun requestReload() {
        reloadRequests.trySend(Unit)
    }

    private suspend fun reload() {
        val userId = observePrimaryUserId().filterNotNull().first()
        val prefix = queryPrefix.value
        val history = readHistory(userId, prefix)
        // A read only publishes while the filter it ran under is still the current one, so a slower read
        // started under an older prefix can't land on top of the one that replaced it.
        if (queryPrefix.value == prefix) {
            overflowTerms = history.overflowTerms
            mayHaveMoreTerms = history.mayHaveOverflowTerms
            mutableState.value = collapseIfEmpty(history.data)
        }
    }

    private suspend fun readHistory(userId: UserId, prefix: String): History {
        val allTerms = getRecentSearchTerms(userId, prefix.ifBlank { null }, ReadTermsLimit)
            .getOrElse { emptyList() }
            .map { it.query }
        val terms = allTerms.take(DisplayedTermsLimit)

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

        return History(
            data = RecentSearchesState.Data(
                terms = terms.toImmutableList(),
                foundItems = mappedFoundItems.toImmutableList()
            ),
            overflowTerms = allTerms.drop(DisplayedTermsLimit),
            mayHaveOverflowTerms = allTerms.size == ReadTermsLimit
        )
    }

    private data class History(
        val data: RecentSearchesState.Data,
        val overflowTerms: List<String>,
        val mayHaveOverflowTerms: Boolean
    )

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

    private companion object {

        const val DisplayedTermsLimit = GetRecentSearchTerms.DefaultLimit
        const val ReadTermsLimit = DisplayedTermsLimit * 2
    }
}
