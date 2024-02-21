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

package ch.protonmail.android.mailcontact.presentation.managemembers

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.previewdata.ManageMembersPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ManageMembersReducerTest(
    private val testName: String,
    private val testInput: TestInput
) {

    private val reducer = ManageMembersReducer()

    @Test
    fun `should produce the expected state`() = with(testInput) {
        val actualState = reducer.newStateFrom(currentState, event)

        assertEquals(expectedState, actualState, testName)
    }

    companion object {

        private val loadedManageMembersUiModelList = ManageMembersPreviewData.manageMembersSampleData()
        private val loadedManageMembersUiModelUpdatedList = ArrayList(
            ManageMembersPreviewData.manageMembersSampleData()
        ).apply {
            this[0] = this[0].copy(isSelected = true)
        }


        private val emptyLoadingState = ManageMembersState.Loading()
        private val loadedManageMembersState = ManageMembersState.Data(
            members = loadedManageMembersUiModelList
        )

        private val transitionsFromLoadingState = listOf(
            TestInput(
                currentState = emptyLoadingState,
                event = ManageMembersEvent.MembersLoaded(loadedManageMembersUiModelList),
                expectedState = loadedManageMembersState
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ManageMembersEvent.LoadMembersError,
                expectedState = emptyLoadingState.copy(
                    errorLoading = Effect.of(TextUiModel(R.string.members_loading_error))
                )
            ),
            TestInput(
                currentState = emptyLoadingState,
                event = ManageMembersEvent.Close,
                expectedState = emptyLoadingState.copy(
                    close = Effect.of(Unit)
                )
            )
        )

        private val transitionsFromDataState = listOf(
            TestInput(
                currentState = loadedManageMembersState,
                event = ManageMembersEvent.MembersLoaded(loadedManageMembersUiModelUpdatedList),
                expectedState = loadedManageMembersState.copy(
                    members = loadedManageMembersUiModelUpdatedList
                )
            ),
            TestInput(
                currentState = loadedManageMembersState,
                event = ManageMembersEvent.LoadMembersError,
                expectedState = loadedManageMembersState
            ),
            TestInput(
                currentState = loadedManageMembersState,
                event = ManageMembersEvent.Close,
                expectedState = loadedManageMembersState.copy(
                    close = Effect.of(Unit)
                )
            ),
            TestInput(
                currentState = loadedManageMembersState,
                event = ManageMembersEvent.OnDone(listOf()),
                expectedState = loadedManageMembersState.copy(
                    onDone = Effect.of(emptyList())
                )
            ),
            TestInput(
                currentState = loadedManageMembersState,
                event = ManageMembersEvent.ErrorUpdatingMember,
                expectedState = loadedManageMembersState.copy(
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.member_update_error))
                )
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = (transitionsFromLoadingState + transitionsFromDataState)
            .map { testInput ->
                val testName = """
                    Current state: ${testInput.currentState}
                    Event: ${testInput.event}
                    Next state: ${testInput.expectedState}
                    
                """.trimIndent()
                arrayOf(testName, testInput)
            }
    }

    data class TestInput(
        val currentState: ManageMembersState,
        val event: ManageMembersEvent,
        val expectedState: ManageMembersState
    )

}
