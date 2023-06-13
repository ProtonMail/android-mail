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
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.testdata.mailbox.MailboxItemUiModelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
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
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelOne),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelOne, UNREAD_COUNT),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.ConversationGrouping
                ),
                expectedState = MailboxListState.Loading
            ),
            TestInput(
                currentState = MailboxListState.Loading,
                operation = MailboxEvent.ItemDetailsOpenedInViewMode(
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
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxEvent.SelectedLabelChanged(MailLabelTestData.customLabelTwo),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxEvent.NewLabelSelected(MailLabelTestData.customLabelTwo, UNREAD_COUNT),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelTwo,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.of(MailLabelTestData.customLabelTwo.id),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxEvent.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.ConversationGrouping
                ),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.conversationId.id),
                            itemType = MailboxItemType.Conversation
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxEvent.ItemDetailsOpenedInViewMode(
                    item = MailboxItemUiModelTestData.readMailboxItemUiModel,
                    preferredViewMode = ViewMode.NoConversationGrouping
                ),
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.of(
                        OpenMailboxItemRequest(
                            itemId = MailboxItemId(MailboxItemUiModelTestData.readMailboxItemUiModel.id),
                            itemType = MailboxItemType.Message
                        )
                    ),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.of(Unit),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxViewAction.OnOfflineWithData,
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.of(Unit),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxViewAction.OnErrorWithData,
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                )
            ),
            TestInput(
                currentState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = false
                ),
                operation = MailboxViewAction.Refresh,
                expectedState = MailboxListState.Data(
                    currentMailLabel = MailLabelTestData.customLabelOne,
                    openItemEffect = Effect.empty(),
                    scrollToMailboxTop = Effect.empty(),
                    offlineEffect = Effect.empty(),
                    refreshErrorEffect = Effect.empty(),
                    refreshRequested = true
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
