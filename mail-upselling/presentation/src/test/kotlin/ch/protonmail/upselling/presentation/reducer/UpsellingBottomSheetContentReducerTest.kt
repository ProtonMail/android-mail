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

package ch.protonmail.upselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlansUiModel
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation.UpsellingBottomSheetContentEvent
import ch.protonmail.android.mailupselling.presentation.reducer.UpsellingBottomSheetContentReducer
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class UpsellingBottomSheetContentReducerTest(private val testInput: TestInput) {

    private val plansUiMapper = mockk<DynamicPlanUiMapper> {
        every { this@mockk.toUiModel(any(), any()) } returns expectedPlanUiModel
    }

    private val reducer = UpsellingBottomSheetContentReducer(plansUiMapper)

    @Test
    fun `should reduce the correct state`() = with(testInput) {
        // When
        val actualState = reducer.newStateFrom(operation)

        // Then
        assertEquals(expectedState, actualState)
    }

    companion object {

        private val expectedPlanUiModel = mockk<DynamicPlansUiModel>()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                operation = UpsellingBottomSheetContentEvent.LoadingError.NoUserId,
                expectedState = UpsellingBottomSheetContentState.Error(
                    error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
                )
            ),
            TestInput(
                operation = UpsellingBottomSheetContentEvent.LoadingError.NoSubscriptions,
                expectedState = UpsellingBottomSheetContentState.Error(
                    error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
                )
            ),
            TestInput(
                operation = UpsellingBottomSheetContentEvent.DataLoaded(mockk(), mockk()),
                expectedState = UpsellingBottomSheetContentState.Data(
                    plans = expectedPlanUiModel
                )
            )
        )
    }

    data class TestInput(
        val operation: UpsellingBottomSheetContentOperation,
        val expectedState: UpsellingBottomSheetContentState
    )
}
