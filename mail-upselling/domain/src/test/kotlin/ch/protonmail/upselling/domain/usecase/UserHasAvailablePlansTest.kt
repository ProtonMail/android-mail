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

package ch.protonmail.upselling.domain.usecase

import ch.protonmail.android.mailupselling.domain.usecase.FilterDynamicPlansByUserSubscription
import ch.protonmail.android.mailupselling.domain.usecase.UserHasAvailablePlans
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UserHasAvailablePlansTest {

    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices>()
    private val filterDynamicPlansByUserSubscription = mockk<FilterDynamicPlansByUserSubscription>()
    private val userHasAvailablePlans = UserHasAvailablePlans(
        getDynamicPlansAdjustedPrices,
        filterDynamicPlansByUserSubscription
    )

    @Test
    fun `should return false if plans cannot be fetched for the given user`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } throws Exception()

        // When
        val actual = userHasAvailablePlans(UserId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false if no plans are available for the given user`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } returns RemoteDynamicPlans
        coEvery { filterDynamicPlansByUserSubscription(UserId, RemoteDynamicPlans) } returns EmptyDynamicPlans.plans

        // When
        val actual = userHasAvailablePlans(UserId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true if the plans list is not empty for the given user`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } returns RemoteDynamicPlans
        coEvery { filterDynamicPlansByUserSubscription(UserId, RemoteDynamicPlans) } returns PopulatedDynamicPlans.plans

        // When
        val actual = userHasAvailablePlans(UserId)

        // Then
        assertTrue(actual)
    }

    private companion object {

        val UserId = UserId("id")
        val RemoteDynamicPlans = DynamicPlans(defaultCycle = 12, plans = listOf(mockk(), mockk()))
        val EmptyDynamicPlans = DynamicPlans(defaultCycle = 12, plans = emptyList())
        val PopulatedDynamicPlans = DynamicPlans(defaultCycle = 12, plans = listOf(mockk()))
    }
}
