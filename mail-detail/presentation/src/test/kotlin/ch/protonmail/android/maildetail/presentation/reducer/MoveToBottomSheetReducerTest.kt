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

import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.testdata.maillabel.MailLabelUiModelTestData
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MoveToBottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = MoveToBottomSheetReducer()

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
        private val destinationsWithArchive = MailLabelUiModelTestData.archiveAndCustomFolder
        private val spamMailLabel = destinationWithSpamSelected.first()
        private val customMailLabel = destinationWithCustomSelected.last()

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = BottomSheetState(MoveToBottomSheetState.Loading),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    destinations,
                    MoveToBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinations,
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinations,
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    updatedDestinations,
                    MoveToBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        updatedDestinations,
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinationWithSpamSelected,
                        spamMailLabel,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    destinationsWithArchive,
                    MoveToBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinationsWithArchive,
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                )
            )
        )

        private val transitionFromDataStateToSelected = listOf(
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinations,
                        null,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected(
                    MailLabelId.System.Spam
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinationWithSpamSelected,
                        spamMailLabel,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinationWithSpamSelected,
                        spamMailLabel,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected(
                    MailLabelId.Custom.Folder(LabelId("folder1"))
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        destinationWithCustomSelected,
                        customMailLabel,
                        MoveToBottomSheetEntryPoint.SelectionMode
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
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
        val currentState: BottomSheetState?,
        val operation: MoveToBottomSheetState.MoveToBottomSheetOperation,
        val expectedState: BottomSheetState?
    )

}
