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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundMailboxItem
import ch.protonmail.android.mailcontentsearch.domain.model.RecentSearchTerm
import ch.protonmail.android.mailcontentsearch.domain.usecase.ClearRecentSearchTerms
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissRecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissRecentSearchTerm
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetRecentFoundMailboxItems
import ch.protonmail.android.mailcontentsearch.domain.usecase.GetRecentSearchTerms
import ch.protonmail.android.mailcontentsearch.domain.usecase.RecordSearchOpen
import ch.protonmail.android.mailcontentsearch.domain.usecase.TouchRecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.usecase.TouchRecentSearchTerm
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentFoundItemUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesState
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesViewAction
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecentSearchesViewModelTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()

    private val observePrimaryUserId = mockk<ObservePrimaryUserIdWithValidSession> {
        every { this@mockk() } returns flowOf(userId)
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk(userId) } returns flowOf(FolderColors)
    }
    private val getRecentSearchTerms = mockk<GetRecentSearchTerms>()
    private val getRecentFoundMailboxItems = mockk<GetRecentFoundMailboxItems>()
    private val touchRecentSearchTerm = mockk<TouchRecentSearchTerm>()
    private val dismissRecentSearchTerm = mockk<DismissRecentSearchTerm>()
    private val clearRecentSearchTerms = mockk<ClearRecentSearchTerms>()
    private val touchRecentFoundItem = mockk<TouchRecentFoundItem>()
    private val dismissRecentFoundItem = mockk<DismissRecentFoundItem>()
    private val recordSearchOpen = mockk<RecordSearchOpen>()
    private val starMessages = mockk<StarMessages>()
    private val unStarMessages = mockk<UnStarMessages>()
    private val itemMapper = mockk<MailboxItemUiModelMapper>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `state starts as loading so the generic empty state is not flashed first`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = emptyList())

        // When
        val viewModel = buildViewModel()

        // Then
        assertEquals(RecentSearchesState.Loading, viewModel.state.value)
    }

    @Test
    fun `refresh exposes the history it reads, with found items mapped to mailbox rows`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = listOf(term("invoice")), items = listOf(foundItem(FirstMessageId)))
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.Refresh)

        // Then
        val expected = RecentSearchesState.Data(
            terms = persistentListOf("invoice"),
            foundItems = persistentListOf(uiModel(FirstMessageId))
        )
        assertEquals(expected, viewModel.state.value)
    }

    @Test
    fun `found items report whether they now sit in trash or spam`() = runTest(dispatcher) {
        // Given an item that has been moved to Trash since it was found, and one that has not.
        givenHistory(
            terms = emptyList(),
            items = listOf(
                foundItem(FirstMessageId, exclusiveLocations = listOf(trashLocation)),
                foundItem(SecondMessageId, exclusiveLocations = listOf(archiveLocation))
            )
        )
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.Refresh)

        // Then the screen can tell which of them has to be opened from All Mail.
        val foundItems = (viewModel.state.value as RecentSearchesState.Data).foundItems
        assertEquals(listOf(true, false), foundItems.map { it.isInTrashOrSpam })
    }

    @Test
    fun `refresh reports empty when there is no history`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = emptyList())
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.Refresh)

        // Then
        assertEquals(RecentSearchesState.Empty, viewModel.state.value)
    }

    @Test
    fun `a failed read is treated as no history rather than surfacing an error page`() = runTest(dispatcher) {
        // Given
        coEvery { getRecentSearchTerms(userId) } returns DataError.Local.Unknown.left()
        coEvery { getRecentFoundMailboxItems(userId) } returns DataError.Local.Unknown.left()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.Refresh)

        // Then
        assertEquals(RecentSearchesState.Empty, viewModel.state.value)
    }

    @Test
    fun `tapping a term bumps it without recording a search open`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = listOf(term("invoice")), items = emptyList())
        coEvery { touchRecentSearchTerm(userId, "invoice") } returns Unit.right()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.TermClicked("invoice"))

        // Then
        coVerify { touchRecentSearchTerm(userId, "invoice") }
        coVerify(exactly = 0) { recordSearchOpen(any(), any(), any(), any()) }
    }

    @Test
    fun `dismissing a term drops it and re-reads so a freed slot refills`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = listOf(term("invoice"), term("parcel")), items = emptyList())
        coEvery { dismissRecentSearchTerm(userId, "invoice") } returns Unit.right()
        val viewModel = buildViewModel()
        viewModel.submit(RecentSearchesViewAction.Refresh)
        // The reload that follows the dismiss sees the term gone and an older one promoted.
        givenHistory(terms = listOf(term("parcel"), term("flight")), items = emptyList())

        // When
        viewModel.submit(RecentSearchesViewAction.TermDismissed("invoice"))

        // Then
        coVerify { dismissRecentSearchTerm(userId, "invoice") }
        assertEquals(
            RecentSearchesState.Data(persistentListOf("parcel", "flight"), persistentListOf()),
            viewModel.state.value
        )
    }

    @Test
    fun `dismissing the last entry falls back to the generic empty state`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = listOf(term("invoice")), items = emptyList())
        coEvery { dismissRecentSearchTerm(userId, "invoice") } returns Unit.right()
        val viewModel = buildViewModel()
        viewModel.submit(RecentSearchesViewAction.Refresh)
        givenHistory(terms = emptyList(), items = emptyList())

        // When
        viewModel.submit(RecentSearchesViewAction.TermDismissed("invoice"))

        // Then
        assertEquals(RecentSearchesState.Empty, viewModel.state.value)
    }

    @Test
    fun `clear drops every term but keeps the previously found items`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = listOf(term("invoice"), term("parcel")), items = listOf(foundItem(FirstMessageId)))
        coEvery { clearRecentSearchTerms(userId) } returns Unit
        val viewModel = buildViewModel()
        viewModel.submit(RecentSearchesViewAction.Refresh)
        givenHistory(terms = emptyList(), items = listOf(foundItem(FirstMessageId)))

        // When
        viewModel.submit(RecentSearchesViewAction.ClearTerms)

        // Then
        coVerify { clearRecentSearchTerms(userId) }
        assertEquals(
            RecentSearchesState.Data(persistentListOf(), persistentListOf(uiModel(FirstMessageId))),
            viewModel.state.value
        )
    }

    @Test
    fun `dismissing a found item drops it from the state`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = listOf(foundItem(FirstMessageId), foundItem(SecondMessageId)))
        coEvery { dismissRecentFoundItem(userId, MessageId(FirstMessageId)) } returns Unit.right()
        val viewModel = buildViewModel()
        viewModel.submit(RecentSearchesViewAction.Refresh)
        givenHistory(terms = emptyList(), items = listOf(foundItem(SecondMessageId)))

        // When
        viewModel.submit(RecentSearchesViewAction.FoundItemDismissed(uiModel(FirstMessageId)))

        // Then
        coVerify { dismissRecentFoundItem(userId, MessageId(FirstMessageId)) }
        assertEquals(
            RecentSearchesState.Data(persistentListOf(), persistentListOf(uiModel(SecondMessageId))),
            viewModel.state.value
        )
    }

    @Test
    fun `re-opening a found item bumps it instead of recording a new search open`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = listOf(foundItem(FirstMessageId)))
        coEvery { touchRecentFoundItem(userId, MessageId(FirstMessageId)) } returns Unit.right()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.FoundItemClicked(uiModel(FirstMessageId)))

        // Then
        coVerify { touchRecentFoundItem(userId, MessageId(FirstMessageId)) }
        coVerify(exactly = 0) { recordSearchOpen(any(), any(), any(), any()) }
    }

    @Test
    fun `starring an unstarred found item stars it and re-reads afterwards`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = listOf(foundItem(FirstMessageId)))
        coEvery { starMessages(userId, listOf(MessageId(FirstMessageId))) } returns Unit.right()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.FoundItemStarClicked(uiModel(FirstMessageId, isStarred = false)))

        // Then
        coVerify { starMessages(userId, listOf(MessageId(FirstMessageId))) }
        coVerify(exactly = 0) { unStarMessages(any(), any()) }
    }

    @Test
    fun `starring an already starred found item unstars it`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = listOf(foundItem(FirstMessageId)))
        coEvery { unStarMessages(userId, listOf(MessageId(FirstMessageId))) } returns Unit.right()
        val viewModel = buildViewModel()

        // When
        viewModel.submit(RecentSearchesViewAction.FoundItemStarClicked(uiModel(FirstMessageId, isStarred = true)))

        // Then
        coVerify { unStarMessages(userId, listOf(MessageId(FirstMessageId))) }
        coVerify(exactly = 0) { starMessages(any(), any()) }
    }

    @Test
    fun `opening a search result records it and re-reads the history`() = runTest(dispatcher) {
        // Given
        givenHistory(terms = emptyList(), items = emptyList())
        coEvery {
            recordSearchOpen(userId, "invoice", MessageId(FirstMessageId), ConversationId("conversation"))
        } returns Unit.right()
        val viewModel = buildViewModel()
        givenHistory(terms = listOf(term("invoice")), items = listOf(foundItem(FirstMessageId)))

        // When
        viewModel.submit(
            RecentSearchesViewAction.SearchResultOpened(
                query = "invoice",
                messageId = MessageId(FirstMessageId),
                conversationId = ConversationId("conversation")
            )
        )

        // Then
        coVerify {
            recordSearchOpen(userId, "invoice", MessageId(FirstMessageId), ConversationId("conversation"))
        }
        assertEquals(
            RecentSearchesState.Data(persistentListOf("invoice"), persistentListOf(uiModel(FirstMessageId))),
            viewModel.state.value
        )
    }

    private fun buildViewModel() = RecentSearchesViewModel(
        observePrimaryUserId = observePrimaryUserId,
        observeFolderColorSettings = observeFolderColorSettings,
        getRecentSearchTerms = getRecentSearchTerms,
        getRecentFoundMailboxItems = getRecentFoundMailboxItems,
        touchRecentSearchTerm = touchRecentSearchTerm,
        dismissRecentSearchTerm = dismissRecentSearchTerm,
        clearRecentSearchTerms = clearRecentSearchTerms,
        touchRecentFoundItem = touchRecentFoundItem,
        dismissRecentFoundItem = dismissRecentFoundItem,
        recordSearchOpen = recordSearchOpen,
        starMessages = starMessages,
        unStarMessages = unStarMessages,
        itemMapper = itemMapper,
        dispatchers = object : DispatcherProvider {
            override val Io = dispatcher
            override val Comp = dispatcher
            override val Main = dispatcher
        }
    )

    private fun givenHistory(terms: List<RecentSearchTerm>, items: List<RecentFoundMailboxItem>) {
        coEvery { getRecentSearchTerms(userId) } returns terms.right()
        coEvery { getRecentFoundMailboxItems(userId) } returns items.right()
        items.forEach { found ->
            coEvery {
                itemMapper.toUiModel(userId, found.item, FolderColors, isShowingSearchResults = true)
            } returns mailboxUiModel(found.item.id)
        }
    }

    private fun term(query: String) = RecentSearchTerm(query = query, lastUsedAt = 1_700_000_000L)

    private fun foundItem(messageId: String, exclusiveLocations: List<ExclusiveLocation> = emptyList()) =
        RecentFoundMailboxItem(
            item = mockk<MailboxItem> {
                every { id } returns messageId
                every { this@mockk.exclusiveLocations } returns exclusiveLocations
            },
            searchQuery = "invoice"
        )

    private fun mailboxUiModel(messageId: String, isStarred: Boolean = false): MailboxItemUiModel =
        MailboxItemUiModelPreviewData.Message.WeatherForecastAug.copy(id = messageId, isStarred = isStarred)

    private fun uiModel(messageId: String, isStarred: Boolean = false) = RecentFoundItemUiModel(
        item = mailboxUiModel(messageId, isStarred),
        searchQuery = "invoice"
    )

    private companion object {

        val FolderColors = FolderColorSettings(useFolderColor = true, inheritParentFolderColor = false)

        const val FirstMessageId = "42"
        const val SecondMessageId = "43"

        val trashLocation = ExclusiveLocation.System(SystemLabelId.Trash, SystemLabelId.Trash.labelId)
        val archiveLocation = ExclusiveLocation.System(SystemLabelId.Archive, SystemLabelId.Archive.labelId)
    }
}
