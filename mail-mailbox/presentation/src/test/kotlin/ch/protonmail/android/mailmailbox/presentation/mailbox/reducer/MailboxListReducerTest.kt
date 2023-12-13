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
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SwipeActionsUiModel
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

        private const val UNREAD_COUNT = 42

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelOne),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.ConversationGrouping
                ),
                expectedState = MailboxListState.Loading(selectionModeEnabled = false)
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = MailboxListState.Loading(selectionModeEnabled = false)
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Loading(selectionModeEnabled = false)
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Loading(selectionModeEnabled = false)
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Loading(selectionModeEnabled = false)
            ),
            TestInput(
                currentState = MailboxListState.Loading(selectionModeEnabled = false),
                operation = MailboxEvent.SelectionModeEnabledChanged(true),
                expectedState = MailboxListState.Loading(selectionModeEnabled = true)
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelTwo),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelTwo, UNREAD_COUNT),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.of(MailLabelTestData.customLabelTwo.id),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.of(Unit),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.of(Unit),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.EnterSelectionMode(MailboxItemUiModelTestData.readMailboxItemUiModel),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false,
                            type = MailboxItemType.Message
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.ExitSelectionMode,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        ),
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.unreadMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false,
                            type = MailboxItemType.Message
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.ItemClicked.ItemRemovedFromSelection(
                    MailboxItemUiModelTestData.readMailboxItemUiModel
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.SelectionModeEnabledChanged(true),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = true,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.MarkAsRead,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = true,
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.MarkAsUnread,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.Trash(5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.ConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.DeleteConfirmed(ViewMode.NoConversationGrouping, 5),
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxEvent.ItemsRemovedFromSelection(
                    listOf(MailboxItemUiModelTestData.readMailboxItemUiModel.id)
                ),
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = emptySet(),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.MoveToConfirmed,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.Star,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = true,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.UnStar,
                expectedState = MailboxListState.Data.SelectionMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    selectedMailboxItems = setOf(
                        SelectedMailboxItem(
                            userId = UserIdTestData.userId,
                            id = MailboxItemUiModelTestData.readMailboxItemUiModel.id,
                            isRead = false,
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.MoveToArchive,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                            isStarred = false,
                            type = MailboxItemType.Conversation
                        )
                    ),
                    selectionModeEnabled = false,
                    swipeActions = null
                ),
                operation = MailboxViewAction.MoveToSpam,
                expectedState = MailboxListState.Data.ViewMode(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false,
                    selectionModeEnabled = false,
                    swipeActions = null
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
                    selectionModeEnabled = true,
                    swipeActions = null
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
                    selectionModeEnabled = true,
                    swipeActions = SwipeActionsUiModel(
                        start = SwipeUiModelSampleData.Trash,
                        end = SwipeUiModelSampleData.Archive
                    )
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
