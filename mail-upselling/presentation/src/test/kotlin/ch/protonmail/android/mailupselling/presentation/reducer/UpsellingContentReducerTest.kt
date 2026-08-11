/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.reducer

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.mapper.PlanMappingError
import ch.protonmail.android.mailupselling.presentation.mapper.PlanUpgradeUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeUiModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpsellingContentReducerTest {

    private val planUpgradeUiMapper = mockk<PlanUpgradeUiMapper>()
    private lateinit var reducer: UpsellingContentReducer

    @BeforeTest
    fun setup() {
        reducer = UpsellingContentReducer(planUpgradeUiMapper)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should reduce to error when mapping fails (no subs)`() = runTest {
        // Given
        val expected = UpsellingScreenContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
        )

        // When

        val actual = reducer.newStateFrom(UpsellingScreenContentEvent.LoadingError.NoSubscriptions)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should reduce to data when mapping succeeds`() = runTest {
        // Given
        val uiModel = mockk<PlanUpgradeUiModel>()
        coEvery {
            planUpgradeUiMapper.toUiModel(any(), any(), any())
        } returns uiModel.right()

        // When
        val actual = reducer.newStateFrom(UpsellingScreenContentEvent.DataLoaded(mockk(), mockk(), theme = null))

        // Then
        assertEquals(UpsellingScreenContentState.Data(uiModel), actual)
    }

    @Test
    fun `should reduce to error when mapping fails`() = runTest {
        // Given
        coEvery {
            planUpgradeUiMapper.toUiModel(any(), any(), any())
        } returns PlanMappingError.EmptyList.left()

        val expected = UpsellingScreenContentState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
        )

        // When
        val actual = reducer.newStateFrom(UpsellingScreenContentEvent.DataLoaded(mockk(), mockk(), theme = null))

        // Then
        assertEquals(expected, actual)
    }
}
