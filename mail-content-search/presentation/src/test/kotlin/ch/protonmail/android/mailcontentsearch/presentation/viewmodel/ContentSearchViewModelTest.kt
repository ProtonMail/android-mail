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

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingData
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailattachments.presentation.reducer.AttachmentDownloadReducer
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchViewAction
import ch.protonmail.android.mailcontentsearch.presentation.reducer.ContentSearchReducer
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetCurrentViewModeForLabel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomBarActions
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomSheetActions
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BottomBarStateFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.MailboxActionExecutor
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.UpdateIncludeFilter
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.AvatarImageStates
import ch.protonmail.android.mailmessage.domain.usecase.HandleAvatarImageLoadingFailure
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent
import ch.protonmail.android.mailpagination.domain.usecase.ObservePageInvalidationEvents
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentSearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val userId = UserId("user-id")
    private val itemId = "item-id"
    private val attachmentId = AttachmentIdUiModel("1")
    private val intentValues = OpenAttachmentIntentValues(
        openMode = AttachmentOpenMode.Open,
        name = "invoice.pdf",
        mimeType = "application/pdf",
        uri = mockk()
    )

    private val searchPager = mockk<Pager<MailboxPageKey, MailboxItem>> {
        every { flow } returns flowOf(PagingData.empty())
    }
    private val pagerFactory = mockk<MailboxPagerFactory> {
        every { create(any(), any(), any(), any(), any()) } returns searchPager
    }
    private val itemMapper = mockk<MailboxItemUiModelMapper>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserIdWithValidSession> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(userId) } returns flowOf(
            FolderColorSettings(useFolderColor = false, inheritParentFolderColor = false)
        )
    }
    private val getCurrentViewModeForLabel = mockk<GetCurrentViewModeForLabel> {
        coEvery { this@mockk.invoke(userId, any()) } returns ViewMode.NoConversationGrouping
    }
    private val findLocalSystemLabelId = mockk<FindLocalSystemLabelId> {
        coEvery { this@mockk.invoke(userId, any()) } returns null
    }
    private val observePageInvalidationEvents = mockk<ObservePageInvalidationEvents> {
        every { this@mockk.invoke() } returns emptyFlow()
    }
    // The executor owns the conversation-vs-message fork and is covered by its own test, so the
    // view model is asserted against it rather than through it to the individual use cases.
    private val mailboxActionExecutor = mockk<MailboxActionExecutor> {
        coEvery { star(any(), any(), any()) } returns Unit.right()
        coEvery { unStar(any(), any(), any()) } returns Unit.right()
        coEvery { markRead(any(), any(), any(), any()) } returns Unit.right()
        coEvery { markUnread(any(), any(), any(), any()) } returns Unit.right()
        coEvery { move(any(), any(), any(), any<SystemLabelId>()) } returns Unit.right()
        coEvery { delete(any(), any(), any(), any()) } returns Unit.right()
    }
    private val getBottomBarActions = mockk<GetBottomBarActions> {
        coEvery {
            this@mockk.invoke(any(), any(), any(), any())
        } returns emptyList<ch.protonmail.android.mailcommon.domain.model.Action>().right()
    }
    private val getBottomSheetActions = mockk<GetBottomSheetActions> {
        coEvery {
            this@mockk.invoke(any(), any(), any(), any())
        } returns AllBottomBarActions(hiddenActions = emptyList(), visibleActions = emptyList()).right()
    }
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val actionUiModelMapper = ActionUiModelMapper()
    private val bottomBarStateFactory = BottomBarStateFactory(getBottomBarActions, actionUiModelMapper)
    private val mailLabelTextMapper = mockk<MailLabelTextMapper> {
        every { mapToString(any()) } returns "Archive"
    }
    private val bulkActionMessageFactory = BulkActionMessageFactory(mailLabelTextMapper)
    private val toolbarActionsRefreshSignal = ToolbarActionsRefreshSignal()
    private val reducer = ContentSearchReducer(
        SelectionStateReducer(),
        AttachmentDownloadReducer(),
        bulkActionMessageFactory
    )
    private val bottomSheetReducer = mockk<BottomSheetReducer> {
        every { newStateFrom(any(), any()) } returns null
    }
    private val updateIncludeFilter = mockk<UpdateIncludeFilter>(relaxUnitFun = true)
    private val observeAvatarImageStates = mockk<ObserveAvatarImageStates> {
        every { this@mockk() } returns flowOf(AvatarImageStates(emptyMap()))
    }
    private val loadAvatarImage = mockk<LoadAvatarImage>(relaxUnitFun = true)
    private val handleAvatarImageLoadingFailure = mockk<HandleAvatarImageLoadingFailure>(relaxUnitFun = true)
    private val avatarImageUiModelMapper = AvatarImageUiModelMapper()

    private fun selectableItem(
        id: String = itemId,
        isRead: Boolean = false,
        isStarred: Boolean = false
    ): MailboxItemUiModel = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.isRead } returns isRead
        every { this@mockk.isStarred } returns isStarred
    }

    private fun viewModel(viewMode: ViewMode = ViewMode.NoConversationGrouping): ContentSearchViewModel {
        coEvery { getCurrentViewModeForLabel(userId, any()) } returns viewMode
        return ContentSearchViewModel(
            pagerFactory = pagerFactory,
            itemMapper = itemMapper,
            observePrimaryUserId = observePrimaryUserId,
            observeFolderColorSettings = observeFolderColorSettings,
            getCurrentViewModeForLabel = getCurrentViewModeForLabel,
            findLocalSystemLabelId = findLocalSystemLabelId,
            observePageInvalidationEvents = observePageInvalidationEvents,
            mailboxActionExecutor = mailboxActionExecutor,
            bottomBarStateFactory = bottomBarStateFactory,
            getBottomSheetActions = getBottomSheetActions,
            getAttachmentIntentValues = getAttachmentIntentValues,
            actionUiModelMapper = actionUiModelMapper,
            bulkActionMessageFactory = bulkActionMessageFactory,
            toolbarActionsRefreshSignal = toolbarActionsRefreshSignal,
            reducer = reducer,
            bottomSheetReducer = bottomSheetReducer,
            updateIncludeFilter = updateIncludeFilter,
            observeAvatarImageStates = observeAvatarImageStates,
            loadAvatarImage = loadAvatarImage,
            handleAvatarImageLoadingFailure = handleAvatarImageLoadingFailure,
            avatarImageUiModelMapper = avatarImageUiModelMapper,
            dispatchers = TestDispatcherProvider(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `should star message when item is not starred`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.StarAction(itemId = itemId, isStarred = false))
        advanceUntilIdle()

        coVerify { mailboxActionExecutor.star(userId, ViewMode.NoConversationGrouping, listOf(itemId)) }
    }

    @Test
    fun `should unstar message when item is starred`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.StarAction(itemId = itemId, isStarred = true))
        advanceUntilIdle()

        coVerify { mailboxActionExecutor.unStar(userId, ViewMode.NoConversationGrouping, listOf(itemId)) }
    }

    @Test
    fun `should star message even when account uses conversation grouping`() = runTest(testDispatcher) {
        val sut = viewModel(ViewMode.ConversationGrouping)

        sut.submit(ContentSearchViewAction.StarAction(itemId = itemId, isStarred = false))
        advanceUntilIdle()

        coVerify { mailboxActionExecutor.star(userId, ViewMode.NoConversationGrouping, listOf(itemId)) }
    }

    @Test
    fun `should include only spam when the spam toggle is submitted`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ToggleIncludeSpam)
        advanceUntilIdle()

        assertEquals(IncludeFilter(includeSpam = true, includeTrash = false), sut.state.value.includeFilter)
    }

    @Test
    fun `should include only trash when the trash toggle is submitted`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ToggleIncludeTrash)
        advanceUntilIdle()

        assertEquals(IncludeFilter(includeSpam = false, includeTrash = true), sut.state.value.includeFilter)
    }

    @Test
    fun `should include both scopes when both toggles are submitted`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ToggleIncludeSpam)
        sut.submit(ContentSearchViewAction.ToggleIncludeTrash)
        advanceUntilIdle()

        assertEquals(IncludeFilter.spamAndTrash(true), sut.state.value.includeFilter)
    }

    @Test
    fun `should drop a scope again when its toggle is submitted twice`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ToggleIncludeSpam)
        sut.submit(ContentSearchViewAction.ToggleIncludeSpam)
        advanceUntilIdle()

        assertEquals(IncludeFilter.None, sut.state.value.includeFilter)
    }

    @Test
    fun `should update live paginator filter in place when toggle is submitted`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ToggleIncludeTrash)
        advanceUntilIdle()

        // Search results are always messages, so the message paginator's filter is mutated.
        coVerify {
            updateIncludeFilter(
                IncludeFilter(includeSpam = false, includeTrash = true),
                ViewMode.NoConversationGrouping
            )
        }
    }

    @Test
    fun `should enter selection mode when an item is long clicked`() = runTest(testDispatcher) {
        val sut = viewModel()

        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))
        advanceUntilIdle()

        assertTrue(sut.state.value.inSelectionMode)
        assertEquals(1, sut.state.value.selectionState.selectedItems.size)
    }

    @Test
    fun `should leave selection mode when the last selected item is toggled off`() = runTest(testDispatcher) {
        val sut = viewModel()
        val item = selectableItem()

        sut.submit(ContentSearchViewAction.ItemLongClicked(item))
        sut.submit(ContentSearchViewAction.ToggleItemSelection(item))
        advanceUntilIdle()

        assertFalse(sut.state.value.inSelectionMode)
    }

    @Test
    fun `should mark selection as read and keep selecting`() = runTest(testDispatcher) {
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem(isRead = false)))

        sut.submit(ContentSearchViewAction.MarkAsRead)
        advanceUntilIdle()

        coVerify {
            mailboxActionExecutor.markRead(userId, ViewMode.NoConversationGrouping, listOf(itemId), any())
        }
        assertTrue(sut.state.value.inSelectionMode)
        assertTrue(sut.state.value.selectionState.selectedItems.all { it.isRead })
    }

    @Test
    fun `should move selection to trash and leave selection mode`() = runTest(testDispatcher) {
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.Trash)
        advanceUntilIdle()

        coVerify {
            mailboxActionExecutor.move(userId, ViewMode.NoConversationGrouping, listOf(itemId), SystemLabelId.Trash)
        }
        assertFalse(sut.state.value.inSelectionMode)
    }

    @Test
    fun `should report an error and stay in selection mode when the move fails`() = runTest(testDispatcher) {
        coEvery {
            mailboxActionExecutor.move(any(), any(), any(), any<SystemLabelId>())
        } returns DataError.Local.Unknown.left()
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.Trash)
        advanceUntilIdle()

        assertNotNull(sut.state.value.errorMessage.consume())
        // The items were not moved, so the selection must survive for a retry.
        assertTrue(sut.state.value.inSelectionMode)
    }

    @Test
    fun `should report an error when the delete fails`() = runTest(testDispatcher) {
        coEvery { mailboxActionExecutor.delete(any(), any(), any(), any()) } returns DataError.Local.Unknown.left()
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.DeleteConfirmed)
        advanceUntilIdle()

        assertNotNull(sut.state.value.errorMessage.consume())
        assertTrue(sut.state.value.inSelectionMode)
    }

    @Test
    fun `should surface an undoable snackbar when moving selection to trash`() = runTest(testDispatcher) {
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.Trash)
        advanceUntilIdle()

        assertTrue(sut.state.value.actionMessage.consume() is ActionResult.UndoableActionResult)
    }

    @Test
    fun `should surface a definitive snackbar when deleting selection`() = runTest(testDispatcher) {
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.DeleteConfirmed)
        advanceUntilIdle()

        assertTrue(sut.state.value.actionMessage.consume() is ActionResult.DefinitiveActionResult)
    }

    @Test
    fun `should show delete dialog on delete and delete then exit on confirmation`() = runTest(testDispatcher) {
        val sut = viewModel()
        sut.submit(ContentSearchViewAction.ItemLongClicked(selectableItem()))

        sut.submit(ContentSearchViewAction.Delete)
        advanceUntilIdle()
        assertTrue(sut.state.value.showDeleteDialog)

        sut.submit(ContentSearchViewAction.DeleteConfirmed)
        advanceUntilIdle()

        coVerify { mailboxActionExecutor.delete(userId, ViewMode.NoConversationGrouping, listOf(itemId), any()) }
        assertFalse(sut.state.value.inSelectionMode)
        assertFalse(sut.state.value.showDeleteDialog)
    }

    @Test
    fun `should hand the downloaded attachment to the screen when a pill is clicked`() = runTest(testDispatcher) {
        val sut = viewModel()
        coEvery {
            getAttachmentIntentValues(userId, AttachmentOpenMode.Open, AttachmentId(attachmentId.value))
        } returns intentValues.right()

        sut.submit(ContentSearchViewAction.RequestAttachment(attachmentId))
        advanceUntilIdle()

        assertEquals(intentValues, sut.state.value.openAttachment.consume())
        assertNull(sut.state.value.downloadingAttachmentId)
    }

    @Test
    fun `should clear the spinner and warn when the attachment download fails`() = runTest(testDispatcher) {
        val sut = viewModel()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } returns DataError.Local.NoDataCached.left()

        sut.submit(ContentSearchViewAction.RequestAttachment(attachmentId))
        advanceUntilIdle()

        assertNotNull(sut.state.value.errorMessage.consume())
        assertNull(sut.state.value.downloadingAttachmentId)
    }

    @Test
    fun `should refuse a second attachment download while one is running`() = runTest(testDispatcher) {
        val sut = viewModel()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } coAnswers { awaitCancellation() }

        sut.submit(ContentSearchViewAction.RequestAttachment(attachmentId))
        advanceUntilIdle()
        sut.submit(ContentSearchViewAction.RequestAttachment(AttachmentIdUiModel("2")))
        advanceUntilIdle()

        coVerify(exactly = 1) { getAttachmentIntentValues(any(), any(), any()) }
        assertNotNull(sut.state.value.errorMessage.consume())
        assertEquals(attachmentId, sut.state.value.downloadingAttachmentId)
    }

    @Test
    fun `should grant the search field auto focus only once`() = runTest(testDispatcher) {
        val sut = viewModel()

        // Re-entering the bar on the way back from a message or from selection mode asks again, and
        // must not raise the keyboard over the results the user came back to read.
        assertTrue(sut.consumeSearchFieldAutoFocus())
        assertFalse(sut.consumeSearchFieldAutoFocus())
    }

    @Test
    fun `should keep a running attachment download while the query is edited`() = runTest(testDispatcher) {
        val sut = viewModel()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } coAnswers { awaitCancellation() }
        sut.submit(ContentSearchViewAction.RequestAttachment(attachmentId))
        advanceUntilIdle()

        sut.type("invoice")
        advanceUntilIdle()

        // The results the download was started from are still on screen, since typing no longer
        // searches — so the file the user asked for is still the one they'll get.
        assertEquals(attachmentId, sut.state.value.downloadingAttachmentId)
    }

    @Test
    fun `should drop a running attachment download when a search is asked for`() = runTest(testDispatcher) {
        val sut = viewModel()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } coAnswers { awaitCancellation() }
        sut.submit(ContentSearchViewAction.RequestAttachment(attachmentId))
        advanceUntilIdle()

        sut.type("invoice")
        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        assertNull(sut.state.value.downloadingAttachmentId)
    }

    @Test
    fun `should not search while the query is only being typed`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }

        sut.type("invoice")
        advanceUntilIdle()

        verify(exactly = 0) { pagerFactory.create(any(), any(), any(), any(), any()) }
        assertEquals("", sut.state.value.query)
        assertEquals(ContentSearchState.Phase.Idle, sut.state.value.phase)
        collectingItems.cancel()
    }

    @Test
    fun `should search the typed query when a search is asked for`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }
        sut.type("  invoice  ")

        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        verify {
            pagerFactory.create(
                userId = userId,
                selectedMailLabelId = MailLabelId.System(SystemLabelId.AllMail.labelId),
                type = MailboxItemType.Message,
                searchQuery = "invoice"
            )
        }
        assertEquals("invoice", sut.state.value.query)
        collectingItems.cancel()
    }

    @Test
    fun `should reload the running search in place when it is asked for again`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }
        sut.type("invoice")
        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        // Retrying reloads the results the screen already shows, rather than building a second paginator
        // and sending the user back to the loading skeleton.
        assertNotNull(sut.state.value.reloadResults.consume())
        verify(exactly = 1) { pagerFactory.create(any(), any(), any(), any(), any()) }
        assertEquals(ContentSearchState.Phase.Loading, sut.state.value.phase)
        collectingItems.cancel()
    }

    @Test
    fun `should ask for a reload once when the results are invalidated`() = runTest(testDispatcher) {
        every { observePageInvalidationEvents() } returns flowOf(PageInvalidationEvent.MessagesInvalidated())

        val sut = viewModel()
        advanceUntilIdle()

        assertNotNull(sut.state.value.reloadResults.consume())
        assertNull(sut.state.value.reloadResults.consume())
    }

    @Test
    fun `should search a term picked from the history without waiting for the search key`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }

        sut.submit(ContentSearchViewAction.SuggestionSelected("invoice"))
        advanceUntilIdle()

        assertEquals("invoice", sut.queryState.text.toString())
        assertEquals("invoice", sut.state.value.query)
        collectingItems.cancel()
    }

    @Test
    fun `should go back to the idle page when the field is emptied`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }
        sut.type("invoice")
        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        sut.type("")
        advanceUntilIdle()

        assertEquals("", sut.state.value.query)
        assertEquals(ContentSearchState.Phase.Idle, sut.state.value.phase)
        collectingItems.cancel()
    }

    @Test
    fun `should go back to the idle page when the query is cleared`() = runTest(testDispatcher) {
        val sut = viewModel()
        val collectingItems = launch { sut.items.collect() }
        sut.type("invoice")
        sut.submit(ContentSearchViewAction.Search)
        advanceUntilIdle()

        sut.submit(ContentSearchViewAction.ClearQuery)
        advanceUntilIdle()

        assertEquals("", sut.queryState.text.toString())
        assertEquals("", sut.state.value.query)
        assertEquals(ContentSearchState.Phase.Idle, sut.state.value.phase)
        collectingItems.cancel()
    }

    // Typing is not a view action: the field's text is owned by the ViewModel, so drive it directly and
    // let the snapshot observer see the write.
    private fun ContentSearchViewModel.type(text: String) {
        queryState.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }
}
