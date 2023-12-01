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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation.AffectingDeleteDialog
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageDeleteDialogReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val deleteDialogReducer = MessageDeleteDialogReducer()

    @Test
    fun `should produce the expected new state`() = with(testInput) {
        val actualState = deleteDialogReducer.newStateFrom(operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val transitions = listOf(
            TestInput(
                operation = MessageViewAction.DeleteRequested,
                expectedState = DeleteDialogState.Shown(
                    title = TextUiModel.TextRes(R.string.message_delete_dialog_title),
                    message = TextUiModel.TextRes(R.string.message_delete_dialog_message)
                )
            ),
            TestInput(
                operation = MessageViewAction.DeleteDialogDismissed,
                expectedState = DeleteDialogState.Hidden
            ),
            TestInput(
                operation = MessageViewAction.DeleteConfirmed,
                expectedState = DeleteDialogState.Hidden
            ),
            TestInput(
                operation = MessageDetailEvent.ErrorDeletingMessage,
                expectedState = DeleteDialogState.Hidden
            ),
            TestInput(
                operation = MessageDetailEvent.ErrorDeletingNoApplicableFolder,
                expectedState = DeleteDialogState.Hidden
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return transitions
                .map {
                    val testName = """
                        Operation: ${it.operation}
                        Next State: ${it.expectedState}
                    """.trimIndent()
                    arrayOf(testName, it)
                }
        }

        data class TestInput(
            val operation: AffectingDeleteDialog,
            val expectedState: DeleteDialogState
        )
    }
}
