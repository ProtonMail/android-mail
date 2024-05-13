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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxSearchStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.SwipeUiModelSampleData
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxListReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val topAppBarReducer = MailboxListReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = topAppBarReducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val listStateWithSearchModeNone = MailboxListState.Data.ViewMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.NotSearching,
            clearState = MailboxListState.Data.ClearState.Hidden
        )
        private val listStateWithSearchModeNewSearch = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.NewSearch
        )
        private val listStateWithSearchModeNewSearchLoading = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.SearchLoading
        )
        private val listStateWithSearchModeSearchData = listStateWithSearchModeNone.copy(
            searchState = MailboxSearchStateSampleData.SearchData
        )

        private val listStateWithSearchSelectionMode = MailboxListState.Data.SelectionMode(
            currentMailLabel = MailLabel.System(MailLabelId.System.Inbox),
            selectedMailboxItems = setOf(
                SelectedMailboxItem(
                    userId = UserIdTestData.userId,
                    id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                    isRead = true,
                    isStarred = false
                )
            ),
            swipeActions = null,
            searchState = MailboxSearchStateSampleData.SearchData,
            clearState = MailboxListState.Data.ClearState.Hidden
        )

        private val listStateSearchSelectionInCustomFolder = listStateWithSearchSelectionMode.copy(
            currentMailLabel = MailLabelTestData.customLabelOne
        )

        private val listStateSearchDataInCustomFolder = listStateWithSearchModeSearchData.copy(
            currentMailLabel = MailLabelTestData.customLabelOne
        )

        private const val UNREAD_COUNT = 42

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelOne),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,

                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabel.System(MailLabelId.System.Trash)),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabel.System(MailLabelId.System.Spam)),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabel.System(MailLabelId.System.Spam),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabel.System(MailLabelId.System.Spam), UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabel.System(MailLabelId.System.Spam),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabel.System(MailLabelId.System.Trash), UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabel.System(MailLabelId.System.Trash),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.ConversationGrouping
                ),
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.NoConversationGrouping
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
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelTwo),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Trash.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ClearAllOperationStatus(false),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Trash.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Visible.Button(
                        text = TextUiModel(R.string.mailbox_action_button_clear_trash)
                    )
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Trash.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ClearAllOperationStatus(true),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Trash.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Visible.Banner
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Spam.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ClearAllOperationStatus(false),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Spam.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Visible.Button(
                        text = TextUiModel(R.string.mailbox_action_button_clear_spam)
                    )
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Spam.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ClearAllOperationStatus(true),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = SystemLabelId.Spam.toMailLabelSystem(),
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Visible.Banner
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelTwo, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.of(MailLabelTestData.customLabelTwo.id),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.ConversationGrouping
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.conversationId.id),
                            itemType = MailboxItemType.Conversation,
                            shouldOpenInComposer = false
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.id),
                            itemType = MailboxItemType.Message,
                            shouldOpenInComposer = false
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.OpenComposer(
                    item = MailboxItemUiModelTestData.draftMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.draftMailboxItemUiModel.id),
                            itemType = MailboxItemType.Message,
                            shouldOpenInComposer = true
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.OpenComposer(
                    item = MailboxItemUiModelTestData.draftMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.draftMailboxItemUiModel.id),
                            itemType = MailboxItemType.Message,
                            shouldOpenInComposer = true
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.of(Unit),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.of(Unit),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.ItemAddedToSelection(
                    MailboxItemUiModelTestData.unreadMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        ),
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(
                    MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.MarkAsRead,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.MarkAsUnread,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.Trash(5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxEvent.ItemsRemovedFromSelection(
                    listOf(MailboxItemUiModelTestData.readMailboxItemUiModel.id)
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.MoveToConfirmed,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.UnStar,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.MoveToArchive,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false
                        )
                    ),
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                ),
                operation = MailboxViewAction.MoveToSpam,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
                )
            ),
            TestInput(
                currentState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = null,
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
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
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    swipeActions = SwipeActionsUiModel(
                        start = SwipeUiModelSampleData.Trash,
                        end = SwipeUiModelSampleData.Archive
                    ),
                    searchState = MailboxSearchStateSampleData.NotSearching,
                    clearState = MailboxListState.Data.ClearState.Hidden
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
