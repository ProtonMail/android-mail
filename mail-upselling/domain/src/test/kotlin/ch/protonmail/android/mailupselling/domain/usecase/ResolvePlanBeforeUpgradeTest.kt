/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.usecase

import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.SubscriptionDetail
import me.proton.android.core.payment.domain.usecase.GetCurrentSubscriptions
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResolvePlanBeforeUpgradeTest {

    private val getCurrentSubscriptions = mockk<GetCurrentSubscriptions>()
    private val resolvePlanBeforeUpgrade = ResolvePlanBeforeUpgrade(getCurrentSubscriptions)

    @Test
    fun `returns the name of the current subscription`() = runTest {
        coEvery { getCurrentSubscriptions() } returns listOf(subscription(PlanUpgradeIds.PlusPlanId))

        assertEquals(PlanUpgradeIds.PlusPlanId, resolvePlanBeforeUpgrade())
    }

    @Test
    fun `falls back to the free plan when there is no subscription`() = runTest {
        coEvery { getCurrentSubscriptions() } returns emptyList()

        assertEquals("Free plan", resolvePlanBeforeUpgrade())
    }

    @Test
    fun `falls back to the free plan when the subscriptions cannot be read`() = runTest {
        coEvery { getCurrentSubscriptions() } throws IllegalStateException("no payments sdk")

        assertEquals("Free plan", resolvePlanBeforeUpgrade())
    }

    private fun subscription(name: String) = mockk<SubscriptionDetail> {
        every { this@mockk.name } returns name
    }
}
