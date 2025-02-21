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

package ch.protonmail.android.testdata.upselling

import java.time.Instant
import me.proton.core.domain.type.IntEnum
import me.proton.core.plan.domain.entity.DynamicEntitlement
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType
import me.proton.core.plan.domain.entity.DynamicPlans

object UpsellingTestData {

    val MonthlyDynamicPlanInstance = DynamicPlanInstance(
        cycle = 1,
        description = "1 month cycle",
        periodEnd = Instant.now(),
        price = mapOf("EUR" to DynamicPlanPrice(id = "id", currency = "EUR", current = 10, default = null))
    )

    val MonthlyDynamicPromoPlanInstance = DynamicPlanInstance(
        cycle = 1,
        description = "1 month cycle",
        periodEnd = Instant.now(),
        price = mapOf("EUR" to DynamicPlanPrice(id = "id", currency = "EUR", current = 10, default = 40))
    )

    val YearlyDynamicPromoPlanInstance = DynamicPlanInstance(
        cycle = 12,
        description = "12 months cycle",
        periodEnd = Instant.now(),
        price = mapOf("EUR" to DynamicPlanPrice(id = "id", currency = "EUR", current = 108, default = 120))
    )

    val YearlyDynamicPlanInstance = DynamicPlanInstance(
        cycle = 12,
        description = "12 months cycle",
        periodEnd = Instant.now(),
        price = mapOf("EUR" to DynamicPlanPrice(id = "id", currency = "EUR", current = 108, default = null))
    )

    val UnlimitedPlan = DynamicPlan(
        name = "bundle2022",
        description = "Mail Plus 2022",
        order = 1,
        state = DynamicPlanState.Available,
        title = "Proton Unlimited",
        instances = mapOf(1 to MonthlyDynamicPlanInstance, 12 to YearlyDynamicPlanInstance),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = listOf<DynamicEntitlement>(
            DynamicEntitlement.Description(
                text = "10 email addresses",
                iconUrl = "iconUrl"
            )
        )
    )

    val PlusPlan = DynamicPlan(
        name = "mail2022",
        description = "Mail Plus 2022",
        order = 2,
        state = DynamicPlanState.Available,
        title = "Mail Plus",
        instances = mapOf(1 to MonthlyDynamicPlanInstance, 12 to YearlyDynamicPlanInstance),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = listOf<DynamicEntitlement>(
            DynamicEntitlement.Description(
                text = "10 email addresses",
                iconUrl = "iconUrl"
            )
        )
    )

    val PlusMonthlyPromoPlan = DynamicPlan(
        name = "mail2022",
        description = "Mail Plus 2022",
        order = 2,
        state = DynamicPlanState.Available,
        title = "Mail Plus",
        instances = mapOf(1 to MonthlyDynamicPromoPlanInstance, 12 to YearlyDynamicPlanInstance),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = listOf<DynamicEntitlement>(
            DynamicEntitlement.Description(
                text = "10 email addresses",
                iconUrl = "iconUrl"
            )
        )
    )
    val PlusYearlyPromoPlan = DynamicPlan(
        name = "mail2022",
        description = "Mail Plus 2022",
        order = 2,
        state = DynamicPlanState.Available,
        title = "Mail Plus",
        instances = mapOf(1 to MonthlyDynamicPlanInstance, 12 to YearlyDynamicPromoPlanInstance),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = listOf<DynamicEntitlement>(
            DynamicEntitlement.Description(
                text = "10 email addresses",
                iconUrl = "iconUrl"
            )
        )
    )

    val DynamicPlans = DynamicPlans(null, listOf(UnlimitedPlan, PlusPlan))

    val DynamicPlanPlusWithNoInstances = DynamicPlan(
        name = "mail2022",
        description = "Mail Plus 2022",
        order = 2,
        state = DynamicPlanState.Available,
        title = "Mail Plus",
        instances = emptyMap(),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = emptyList()
    )

    val DynamicPlanPlusWithIdenticalInstances = DynamicPlan(
        name = "mail2022",
        description = "Mail Plus 2022",
        order = 2,
        state = DynamicPlanState.Available,
        title = "Mail Plus",
        instances = mapOf(1 to MonthlyDynamicPlanInstance, 1 to MonthlyDynamicPlanInstance),
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        entitlements = emptyList()
    )
}
