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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.SwipeUiModelSampleData
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.testdata.avatar.AvatarImageStatesTestData
import ch.protonmail.android.testdata.avatar.AvatarImagesUiModelTestData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxListReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val topAppBarReducer = MailboxListReducer(AvatarImageUiModelMapper())

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = topAppBarReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val listStateWithSearchModeNone = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabelTestData.inboxSystemLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshOngoing = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            shouldShowFab = true,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            loadingBarState = LoadingBarUiState.Hide
        )
        private val listStateWithSearchModeNewSearch = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.NewSearch,
            shouldShowFab = false,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            loadingBarState = LoadingBarUiState.Hide
        )
        private val listStateWithSearchModeNewSearchLoading = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.SearchLoading,
            shouldShowFab = false,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            loadingBarState = LoadingBarUiState.Hide
        )
        private val listStateWithSearchModeSearchData = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.SearchData,
            shouldShowFab = false,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            loadingBarState = LoadingBarUiState.Hide
        )

        private val listStateWithSearchSelectionMode = MailboxListState.Data.SelectionMode(
            currentMailLabel = MailLabelTestData.inboxSystemLabel,
            selectedMailboxItems = setOf(
                SelectedMailboxItem(
                    id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                    isRead = true,
                    isStarred = false
                )
            ),
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.SearchData,
            shouldShowFab = false,
            avatarImagesUiModel = AvatarImagesUiModel.Empty,
            areAllItemsSelected = false,
            refreshOngoing = false,
            loadingBarState = LoadingBarUiState.Hide
        )

        private val listStateSearchSelectionInCustomFolder = listStateWithSearchSelectionMode.copy(
            currentMailLabel = MailLabelTestData.customLabelOne
        )

        private val listStateSearchDataInCustomFolder = listStateWithSearchModeSearchData.copy(
            currentMailLabel = MailLabelTestData.customLabelOne
        )

        private const val UNREAD_COUNT = 42

        private val attachmentIdUiModel = AttachmentIdUiModel("attachment-id")

        private val attachmentIntentValues = OpenAttachmentIntentValues(
            mimeType = "mimeType",
            openMode = AttachmentOpenMode.Open,
            uri = mockk(),
            name = "file.pdf"
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelOne),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,

                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.trashSystemLabel),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.trashSystemLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.spamSystemLabel),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.spamSystemLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.CouldNotLoadUserSession,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.spamSystemLabel),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.spamSystemLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.CouldNotLoadUserSession,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.spamSystemLabel, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.spamSystemLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.trashSystemLabel, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.trashSystemLabel,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    contextLabel = LabelIdSample.RustLabel3,
                    viewModeIsConversationGrouping = true,
                    subitemId = null
                ),
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    contextLabel = LabelIdSample.RustLabel3,
                    viewModeIsConversationGrouping = true,
                    subitemId = null
                ),
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.SearchQuery("query"),
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxViewAction.SearchResult,
                expectedState = MailboxListState.Loading
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelTwo),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelTwo, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.of(MailLabelTestData.customLabelTwo.id),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    contextLabel = MailLabelTestData.customLabelOne.id.labelId,
                    viewModeIsConversationGrouping = true,
                    subitemId = null
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.conversationId.id),
                            shouldOpenInComposer = false,
                            openedFromLocation = MailLabelTestData.customLabelOne.id.labelId
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    contextLabel = MailLabelTestData.customLabelOne.id.labelId,
                    viewModeIsConversationGrouping = true,
                    subitemId = null
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.conversationId.id),
                            shouldOpenInComposer = false,
                            subItemId = null,
                            openedFromLocation = MailLabelTestData.customLabelOne.id.labelId,
                            locationViewModeIsConversation = true
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.ItemClicked.OpenComposer(
                    item = MailboxItemUiModelTestData.draftMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.draftMailboxItemUiModel.id),
                            shouldOpenInComposer = true,
                            openedFromLocation = MailLabelTestData.customLabelOne.id.labelId,
                            locationViewModeIsConversation = true
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.ItemClicked.OpenComposer(
                    item = MailboxItemUiModelTestData.draftMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.draftMailboxItemUiModel.id),
                            shouldOpenInComposer = true,
                            openedFromLocation = MailLabelTestData.customLabelOne.id.labelId
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.of(Unit),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.AttachmentReadyEvent(attachmentIntentValues),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    downloadingAttachmentId = null,
                    displayAttachment = Effect.of(attachmentIntentValues)
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.AttachmentErrorEvent,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    downloadingAttachmentId = null,
                    displayAttachmentError = Effect.of(TextUiModel.TextRes(R.string.mailbox_attachment_download_error))
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.AttachmentDownloadStartedEvent(attachmentIdUiModel),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    downloadingAttachmentId = attachmentIdUiModel
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(
                    MailboxItemUiModelTestData.unreadMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        ),
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(
                    MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.MarkAsRead,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.MarkAsUnread,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.MoveToConfirmed.Trash(ViewMode.ConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = true,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.ItemsRemovedFromSelection(
                    listOf(MailboxItemUiModelTestData.readMailboxItemUiModel.id)
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.UnStar,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.MoveToArchive,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = false,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxViewAction.MoveToSpam,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.SwipeActionsChanged(
                    SwipeActionsUiModel(
                        start = SwipeUiModelSampleData.Trash,
                        end = SwipeUiModelSampleData.Archive
                    )
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = SwipeActionsUiModel(
                        start = SwipeUiModelSampleData.Trash,
                        end = SwipeUiModelSampleData.Archive
                    ),
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.AvatarImageStatesUpdated(
                    AvatarImageStatesTestData.SampleData1
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModelTestData.SampleData1,
                    loadingBarState = LoadingBarUiState.Hide
                )
            ),
            TestInput(
                currentState = listStateWithSearchModeNone,
                operation = MailboxViewAction.EnterSearchMode,
                expectedState = listStateWithSearchModeNewSearch
            ),
            TestInput(
                currentState = listStateWithSearchModeNewSearch,
                operation = MailboxViewAction.SearchQuery(MailboxSearchStateSampleData.QueryString),
                expectedState = listStateWithSearchModeNewSearchLoading
            ),
            TestInput(
                currentState = listStateWithSearchModeNewSearchLoading,
                operation = MailboxViewAction.SearchResult,
                expectedState = listStateWithSearchModeSearchData
            ),
            TestInput(
                currentState = listStateWithSearchModeSearchData,
                operation = MailboxViewAction.SearchQuery(MailboxSearchStateSampleData.QueryString),
                expectedState = listStateWithSearchModeSearchData
            ),
            TestInput(
                currentState = listStateWithSearchModeNewSearch,
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = listStateWithSearchModeNone
            ),
            TestInput(
                currentState = listStateWithSearchModeNewSearchLoading,
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = listStateWithSearchModeNone
            ),
            TestInput(
                currentState = listStateWithSearchModeSearchData,
                operation = MailboxViewAction.ExitSearchMode,
                expectedState = listStateWithSearchModeNone
            ),
            TestInput(
                currentState = listStateWithSearchModeSearchData,
                operation = MailboxViewAction.SearchQuery(MailboxSearchStateSampleData.QueryString),
                expectedState = listStateWithSearchModeSearchData
            ),
            TestInput(
                currentState = listStateWithSearchModeSearchData,
                operation = MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                expectedState = listStateWithSearchSelectionMode
            ),
            TestInput(
                currentState = listStateWithSearchSelectionMode,
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = listStateWithSearchModeSearchData
            ),
            TestInput(
                currentState = listStateSearchSelectionInCustomFolder,
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = listStateSearchDataInCustomFolder
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.AllItemsSelected(
                    allItems = listOf(
                        MailboxItemUiModelTestData.readMailboxItemUiModel,
                        MailboxItemUiModelTestData.unreadMailboxItemUiModel
                    )
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = MailboxItemUiModelTestData.readMailboxItemUiModel.isRead,
                            isStarred = MailboxItemUiModelTestData.readMailboxItemUiModel.isStarred
                        ),
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = MailboxItemUiModelTestData.unreadMailboxItemUiModel.isRead,
                            isStarred = MailboxItemUiModelTestData.unreadMailboxItemUiModel.isStarred
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = true,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.AllItemsSelected(
                    allItems = mutableListOf<MailboxItemUiModel>().apply {
                        for (i in 0..200) {
                            add(MailboxItemUiModelTestData.readMailboxItemUiModel.copy(id = i.toString()))
                        }
                    }
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = mutableSetOf<SelectedMailboxItem>().apply {
                        for (i in 0..99) {
                            add(
                                SelectedMailboxItem(
                                    id = i.toString(),
                                    isRead = MailboxItemUiModelTestData.readMailboxItemUiModel.isRead,
                                    isStarred = MailboxItemUiModelTestData.readMailboxItemUiModel.isStarred
                                )
                            )
                        }
                    },
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = true,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = mutableSetOf<SelectedMailboxItem>().apply {
                        for (i in 0..5) {
                            add(
                                SelectedMailboxItem(
                                    id = i.toString(),
                                    isRead = MailboxItemUiModelTestData.readMailboxItemUiModel.isRead,
                                    isStarred = MailboxItemUiModelTestData.readMailboxItemUiModel.isStarred
                                )
                            )
                        }
                    },
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.AllItemsSelected(
                    allItems = mutableListOf<MailboxItemUiModel>().apply {
                        for (i in 0..200) {
                            add(MailboxItemUiModelTestData.readMailboxItemUiModel.copy(id = i.toString()))
                        }
                    }
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = mutableSetOf<SelectedMailboxItem>().apply {
                        for (i in 0..99) {
                            add(
                                SelectedMailboxItem(
                                    id = i.toString(),
                                    isRead = MailboxItemUiModelTestData.readMailboxItemUiModel.isRead,
                                    isStarred = MailboxItemUiModelTestData.readMailboxItemUiModel.isStarred
                                )
                            )
                        }
                    },
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = true,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = MailboxItemUiModelTestData.readMailboxItemUiModel.isRead,
                            isStarred = MailboxItemUiModelTestData.readMailboxItemUiModel.isStarred
                        ),
                        SelectedMailboxItem(
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = MailboxItemUiModelTestData.unreadMailboxItemUiModel.isRead,
                            isStarred = MailboxItemUiModelTestData.unreadMailboxItemUiModel.isStarred
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = true,
                    refreshOngoing = false
                ),
                operation = MailboxEvent.AllItemsDeselected,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    swipeActions = null,
                    searchState = MailboxSearchState.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide,
                    areAllItemsSelected = false,
                    refreshOngoing = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshOngoing = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    shouldShowFab = true,
                    avatarImagesUiModel = AvatarImagesUiModel.Empty,
                    loadingBarState = LoadingBarUiState.Hide
                ),
                operation = MailboxEvent.CouldNotLoadUserSession,
                expectedState = MailboxListState.CouldNotLoadUserSession
            ),
            TestInput(
                currentState = listStateWithSearchModeNone,
                operation = MailboxEvent.LoadingBarStateUpdated(
                    LoadingBarUiState.Show(cycleDurationMs = 1500)
                ),
                expectedState = listStateWithSearchModeNone.copy(
                    loadingBarState = LoadingBarUiState.Show(cycleDurationMs = 1500)
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return (transitionsFromLoadingState + transitionsFromDataState)
                .map { testInput ->
                    val testName = """
                        Current state: ${testInput.currentState}
                        Operation: ${testInput.operation}
                        Next state: ${testInput.expectedState}

                    """.trimIndent()
                    arrayOf(testName, testInput)
                }
        }
    }

    data class TestInput(
        val currentState: MailboxListState,
        val operation: MailboxOperation.AffectingMailboxList,
        val expectedState: MailboxListState
    )
}
