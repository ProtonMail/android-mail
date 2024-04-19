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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailupselling.domain.usecase.FilterDynamicPlansByUserSubscription
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlans
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FilterDynamicPlansByUserSubscriptionTest {

    private val isPaidUser = mockk<IsPaidUser>()
    private val isPaidMailUser = mockk<IsPaidMailUser>()
    private val filterDynamicPlansByUserSubscription = FilterDynamicPlansByUserSubscription(isPaidUser, isPaidMailUser)

    @Test
    fun `should return unlimited if paid non mail user`() = runTest {
        // Given
        coEvery { isPaidUser(UserId) } returns true.right()
        coEvery { isPaidMailUser(UserId) } returns false.right()
        val expected = DynamicPlans(defaultCycle = 12, plans = listOf(UpsellingTestData.UnlimitedPlan)).plans

        // When
        val actual = filterDynamicPlansByUserSubscription(UserId, DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return plus plan if free user`() = runTest {
        // Given
        coEvery { isPaidUser(UserId) } returns false.right()
        coEvery { isPaidMailUser(UserId) } returns false.right()
        val expected = DynamicPlans(defaultCycle = 12, plans = listOf(UpsellingTestData.PlusPlan)).plans

        // When
        val actual = filterDynamicPlansByUserSubscription(UserId, DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return no plans if paid mail plan user`() = runTest {
        // Given
        coEvery { isPaidUser(UserId) } returns true.right()
        coEvery { isPaidMailUser(UserId) } returns true.right()
        val expected = emptyList<DynamicPlan>()

        // When
        val actual = filterDynamicPlansByUserSubscription(UserId, DynamicPlans)

        // Then
        assertEquals(expected, actual)
    }

    private companion object {

        val UserId = UserId("id")
        val DynamicPlans = DynamicPlans(
            defaultCycle = 12,
            plans = listOf(UpsellingTestData.PlusPlan, UpsellingTestData.UnlimitedPlan)
        )
    }
}
