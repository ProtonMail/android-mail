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

import java.time.Instant
import ch.protonmail.android.mailupselling.domain.usecase.FilterDynamicPlansByUserSubscription
import ch.protonmail.android.mailupselling.domain.usecase.GetPromotionStatus
import ch.protonmail.android.mailupselling.domain.usecase.PromoStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetPromotionStatusTest {

    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices> {}
    private val filterDynamicPlansByUserSubscription = mockk<FilterDynamicPlansByUserSubscription>()
    private val sut = GetPromotionStatus(
        getDynamicPlansAdjustedPrices,
        filterDynamicPlansByUserSubscription
    )

    @Test
    fun `should return no plans if plans cannot be fetched for the given user`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } throws Exception()

        // When
        val actual = sut(UserId)

        // Then
        assertEquals(PromoStatus.NO_PLANS, actual)
    }

    @Test
    fun `should return no plans if no plans are available for the given user`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } returns RemoteDynamicPlansNormal
        coEvery { filterDynamicPlansByUserSubscription(UserId, RemoteDynamicPlansNormal) } returns
            EmptyDynamicPlans.plans

        // When
        val actual = sut(UserId)

        // Then
        assertEquals(PromoStatus.NO_PLANS, actual)
    }

    @Test
    fun `should return normal if the plans list is not empty for the given user without promos`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } returns RemoteDynamicPlansNormal
        coEvery { filterDynamicPlansByUserSubscription(UserId, RemoteDynamicPlansNormal) } returns
            PopulatedDynamicPlansNormal.plans

        // When
        val actual = sut(UserId)

        // Then
        assertEquals(PromoStatus.NORMAL, actual)
    }

    @Test
    fun `should return promo if the plans list is not empty for the given user with promos`() = runTest {
        // Given
        coEvery { getDynamicPlansAdjustedPrices(UserId) } returns RemoteDynamicPlansPromo
        coEvery { filterDynamicPlansByUserSubscription(UserId, RemoteDynamicPlansPromo) } returns
            PopulatedDynamicPlansPromos.plans

        // When
        val actual = sut(UserId)

        // Then
        assertEquals(PromoStatus.PROMO, actual)
    }

    private companion object {

        val UserId = UserId("id")

        val PlanPriceNormal = DynamicPlanPrice(
            id = "id1",
            currency = "EUR",
            current = 10,
            default = 10
        )
        val PlanPricePromo = DynamicPlanPrice(
            id = "id1",
            currency = "EUR",
            current = 10,
            default = 11
        )
        val PlanInstanceNormal = DynamicPlanInstance(
            cycle = 0,
            description = "desc",
            periodEnd = Instant.EPOCH,
            price = mapOf("EUR" to PlanPriceNormal)
        )
        val PlanInstancePromo = DynamicPlanInstance(
            cycle = 0,
            description = "desc",
            periodEnd = Instant.EPOCH,
            price = mapOf("EUR" to PlanPricePromo)
        )
        val DynamicPlanNormal = DynamicPlan(
            name = "Plan",
            order = 1,
            state = DynamicPlanState.Available,
            title = "Title",
            type = IntEnum(0, DynamicPlanType.Primary),
            instances = mapOf(
                0 to PlanInstanceNormal
            )
        )
        val DynamicPlanPromo = DynamicPlan(
            name = "Plan",
            order = 1,
            state = DynamicPlanState.Available,
            title = "Title",
            type = IntEnum(0, DynamicPlanType.Primary),
            instances = mapOf(
                0 to PlanInstancePromo
            )
        )
        val RemoteDynamicPlansNormal = DynamicPlans(defaultCycle = 12, plans = listOf(DynamicPlanNormal))
        val RemoteDynamicPlansPromo = DynamicPlans(
            defaultCycle = 12,
            plans = listOf(DynamicPlanNormal, DynamicPlanPromo)
        )
        val EmptyDynamicPlans = DynamicPlans(defaultCycle = 12, plans = emptyList())
        val PopulatedDynamicPlansNormal = DynamicPlans(defaultCycle = 12, plans = listOf(DynamicPlanNormal))
        val PopulatedDynamicPlansPromos = DynamicPlans(
            defaultCycle = 12,
            plans = listOf(DynamicPlanNormal, DynamicPlanPromo)
        )
    }
}
