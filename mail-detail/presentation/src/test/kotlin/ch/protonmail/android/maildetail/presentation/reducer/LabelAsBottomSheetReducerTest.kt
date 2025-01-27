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

import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelWithSelectedStateSample
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.reducer.LabelAsBottomSheetReducer
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class LabelAsBottomSheetReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = LabelAsBottomSheetReducer()

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        assertEquals(expectedState, actualState, testName)
    }


    companion object {

        private val labelListWithoutSelection = LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection
        private val labelListWithSelection = LabelUiModelWithSelectedStateSample.customLabelListWithSelection
        private val labelListWithPartialSelection =
            LabelUiModelWithSelectedStateSample.customLabelListWithPartialSelection
        private val labelOperation = LabelId("label1")


        private val transitionFromLoadingState = listOf(
            TestInput(
                currentState = BottomSheetState(LabelAsBottomSheetState.Loading),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = labelListWithoutSelection.map { it.labelUiModel }.toImmutableList(),
                    selectedLabels = emptyList<LabelId>().toImmutableList(),
                    entryPoint = LabelAsBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(labelListWithoutSelection, LabelAsBottomSheetEntryPoint.SelectionMode)
                )
            ),
            TestInput(
                currentState = BottomSheetState(LabelAsBottomSheetState.Loading),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = labelListWithSelection.map { it.labelUiModel }.toImmutableList(),
                    selectedLabels = labelListWithSelection
                        .filter { it.selectedState == LabelSelectedState.Selected }
                        .map { it.labelUiModel.id.labelId }
                        .toImmutableList(),
                    entryPoint = LabelAsBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(labelListWithSelection, LabelAsBottomSheetEntryPoint.SelectionMode)
                )
            ),
            TestInput(
                currentState = BottomSheetState(LabelAsBottomSheetState.Loading),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(LabelId("label1")),
                expectedState = BottomSheetState(LabelAsBottomSheetState.Loading)
            ),
            TestInput(
                currentState = BottomSheetState(LabelAsBottomSheetState.Loading),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = labelListWithPartialSelection.map { it.labelUiModel }.toImmutableList(),
                    selectedLabels = labelListWithPartialSelection
                        .filter { it.selectedState == LabelSelectedState.Selected }
                        .map { it.labelUiModel.id.labelId }
                        .toImmutableList(),
                    partiallySelectedLabels = labelListWithPartialSelection
                        .filter { it.selectedState == LabelSelectedState.PartiallySelected }
                        .map { it.labelUiModel.id.labelId }
                        .toImmutableList(),
                    entryPoint = LabelAsBottomSheetEntryPoint.SelectionMode
                ),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        labelListWithPartialSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                )
            )
        )

        private val transitionFromDataState = listOf(
            TestInput(
                currentState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        labelListWithoutSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(labelOperation),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        LabelUiModelWithSelectedStateSample.customLabelListWithSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        labelListWithSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(labelOperation),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        LabelUiModelWithSelectedStateSample.customLabelListWithoutSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                )
            ),
            TestInput(
                currentState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        labelListWithPartialSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                ),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(labelOperation),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        LabelUiModelWithSelectedStateSample.customLabelListWithSelection,
                        LabelAsBottomSheetEntryPoint.SelectionMode
                    )
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionFromLoadingState + transitionFromDataState)
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
        val operation: LabelAsBottomSheetState.LabelAsBottomSheetOperation,
        val expectedState: BottomSheetState?
    )

}
