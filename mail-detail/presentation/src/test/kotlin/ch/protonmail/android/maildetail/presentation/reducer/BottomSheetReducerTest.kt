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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maildetail.presentation.model.BottomSheetOperation
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maildetail.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
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

    private val moveToBottomSheetReducer: MoveToBottomSheetReducer = mockk(relaxed = true)
    private val labelAsBottomSheetReducer: LabelAsBottomSheetReducer = mockk(relaxed = true)
    private val reducer = BottomSheetReducer(moveToBottomSheetReducer, labelAsBottomSheetReducer)

    @Test
    fun `should produce the expected new bottom sheet state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, operation)

        if (reducesBottomSheetVisibilityEffects) {
            assertEquals(expectedState, actualState, testName)
        }

        if (reducesMoveTo) {
            verify {
                moveToBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as MoveToBottomSheetState.MoveToBottomSheetOperation
                )
            }
        } else {
            verify { moveToBottomSheetReducer wasNot Called }
        }

        if (reducesLabelAs) {
            verify {
                labelAsBottomSheetReducer.newStateFrom(
                    currentState,
                    testInput.operation as LabelAsBottomSheetState.LabelAsBottomSheetOperation
                )
            }
        } else {
            verify { labelAsBottomSheetReducer wasNot Called }
        }
    }

    companion object {

        private val bottomSheetVisibilityOperations = listOf(
            TestInput(
                currentState = null,
                operation = BottomSheetOperation.Requested,
                expectedState = BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Show)),
                reducesBottomSheetVisibilityEffects = true,
                reducesMoveTo = false,
                reducesLabelAs = false
            ),
            TestInput(
                currentState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        listOf<MailLabelUiModel>().toImmutableList(),
                        null
                    )
                ),
                operation = BottomSheetOperation.Dismiss,
                expectedState = BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Hide)),
                reducesBottomSheetVisibilityEffects = true,
                reducesMoveTo = false,
                reducesLabelAs = false
            )
        )

        private val moveToBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    listOf<MailLabelUiModel>()
                        .toImmutableList()
                ),
                expectedState = BottomSheetState(
                    MoveToBottomSheetState.Data(
                        listOf<MailLabelUiModel>()
                            .toImmutableList(),
                        null
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesMoveTo = true,
                reducesLabelAs = false
            )
        )

        private val labelAsBottomSheetOperation = listOf(
            TestInput(
                currentState = BottomSheetState(null, Effect.empty()),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    listOf<MailLabelUiModel.Custom>()
                        .toImmutableList(),
                    listOf<LabelId>().toImmutableList()
                ),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList()
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = true,
                reducesMoveTo = false
            ),
            TestInput(
                currentState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList()
                    )
                ),
                operation = LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled(LabelId("labelId")),
                expectedState = BottomSheetState(
                    LabelAsBottomSheetState.Data(
                        listOf<LabelUiModelWithSelectedState>()
                            .toImmutableList()
                    )
                ),
                reducesBottomSheetVisibilityEffects = false,
                reducesLabelAs = true,
                reducesMoveTo = false
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (
            bottomSheetVisibilityOperations +
                moveToBottomSheetOperation +
                labelAsBottomSheetOperation
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
        val operation: BottomSheetOperation,
        val expectedState: BottomSheetState?,
        val reducesBottomSheetVisibilityEffects: Boolean,
        val reducesMoveTo: Boolean,
        val reducesLabelAs: Boolean
    )

}
