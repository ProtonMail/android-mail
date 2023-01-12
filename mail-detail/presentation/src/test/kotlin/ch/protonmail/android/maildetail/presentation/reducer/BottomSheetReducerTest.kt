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

import ch.protonmail.android.maildetail.presentation.model.BottomSheetContentState
import ch.protonmail.android.maildetail.presentation.model.BottomSheetOperation
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class BottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = BottomSheetReducer()

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val destinations = MailLabelUiModelTestData.spamAndCustomFolder
        private val updatedDestinations = MailLabelUiModelTestData.systemAndTwoCustomFolders
        private val destinationWithSpamSelected = MailLabelUiModelTestData.spamAndCustomFolderWithSpamSelected
        private val destinationWithCustomSelected = MailLabelUiModelTestData.spamAndCustomFolderWithCustomSelected
        private val spamMailLabel = destinationWithSpamSelected.first()
        private val customMailLabel = destinationWithCustomSelected.last()

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = MoveToBottomSheetState.Loading,
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(destinations),
                expectedState = MoveToBottomSheetState.Data(destinations, null)
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = MoveToBottomSheetState.Data(destinations, null),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(updatedDestinations),
                expectedState = MoveToBottomSheetState.Data(updatedDestinations, null)
            )
        )

        private val transitionFromDataStateToSelected = listOf(
            TestInput(
                currentState = MoveToBottomSheetState.Data(destinations, null),
                operation = MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected(
                    MailLabelId.System.Spam
                ),
                expectedState = MoveToBottomSheetState.Data(destinationWithSpamSelected, spamMailLabel)
            ),
            TestInput(
                currentState = MoveToBottomSheetState.Data(destinationWithSpamSelected, spamMailLabel),
                operation = MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected(
                    MailLabelId.Custom.Folder(LabelId("folder1"))
                ),
                expectedState = MoveToBottomSheetState.Data(destinationWithCustomSelected, customMailLabel)
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            (
                transitionsFromLoadingState +
                    transitionsFromDataState +
                    transitionFromDataStateToSelected
                )
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
        val currentState: BottomSheetContentState?,
        val operation: BottomSheetOperation,
        val expectedState: BottomSheetContentState?
    )

}
