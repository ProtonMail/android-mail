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

package ch.protonmail.android.mailmessage.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.sample.ActionUiModelSample
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MailboxMoreActionsBottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = MailboxMoreActionsBottomSheetReducer()

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomSheetState(MailboxMoreActionsBottomSheetState.Loading),
                operation = MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                    actionUiModels = listOf(
                        ActionUiModelSample.Archive,
                        ActionUiModelSample.MarkUnread
                    ).toImmutableList()
                ),
                expectedState = BottomSheetState(
                    contentState = MailboxMoreActionsBottomSheetState.Data(
                        actionUiModels = listOf(
                            ActionUiModelSample.Archive,
                            ActionUiModelSample.MarkUnread
                        ).toImmutableList()
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(MailboxMoreActionsBottomSheetState.Loading),
                operation = MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                    actionUiModels = listOf(
                        ActionUiModelSample.Archive,
                        ActionUiModelSample.MarkUnread,
                        ActionUiModelSample.CustomizeToolbar
                    ).toImmutableList()
                ),
                expectedState = BottomSheetState(
                    contentState = MailboxMoreActionsBottomSheetState.Data(
                        actionUiModels = listOf(
                            ActionUiModelSample.Archive,
                            ActionUiModelSample.MarkUnread,
                            ActionUiModelSample.CustomizeToolbar
                        ).toImmutableList()
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = transitionsFromLoadingState
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Operation: ${testInput.operation}
                    Next state: ${testInput.expectedState}   
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: BottomSheetState,
        val operation: MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetOperation,
        val expectedState: BottomSheetState
    )
}
