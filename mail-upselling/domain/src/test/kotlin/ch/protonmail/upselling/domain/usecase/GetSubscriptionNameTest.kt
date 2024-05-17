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

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.Plan
import me.proton.core.plan.domain.entity.Subscription
import me.proton.core.plan.domain.usecase.GetCurrentSubscription
import me.proton.core.test.kotlin.assertIs
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetSubscriptionNameTest {

    private val getCurrentSubscription = mockk<GetCurrentSubscription>()
    private val getSubscriptionName = GetSubscriptionName(getCurrentSubscription)

    private val mockPlan = mockk<Plan> {
        every { this@mockk.name } returns BasePlan
    }

    private val mockFreePlan = mockk<Plan> {
        every { this@mockk.name } returns FreePlan
    }

    private val mockSubscription = mockk<Subscription> {
        every { this@mockk.plans } returns listOf(mockPlan)
    }

    private val freeMockSubscription = mockk<Subscription> {
        every { this@mockk.plans } returns listOf(mockFreePlan)
    }

    private val emptySubscription = mockk<Subscription> {
        every { this@mockk.plans } returns listOf()
    }

    @Test
    fun `should call the underlying UC when no cache is available`() = runTest {
        // Given
        val userId = UserSample.Primary.userId
        val secondUserId = UserId("userId2")
        coEvery { getCurrentSubscription(userId) } returns mockSubscription
        coEvery { getCurrentSubscription(secondUserId) } returns freeMockSubscription

        // When
        val firstSubscription = getSubscriptionName(userId).getOrNull()?.value
        val secondSubscription = getSubscriptionName(secondUserId).getOrNull()?.value

        // Then
        assertEquals(BasePlan, firstSubscription)
        assertEquals(FreePlan, secondSubscription)
        coVerify(exactly = 1) {
            getCurrentSubscription(userId)
            getCurrentSubscription(secondUserId)
        }
    }

    @Test
    fun `should not call the underlying UC when the cache entry is available`() = runTest {
        // Given
        val userId = UserSample.Primary.userId
        coEvery { getCurrentSubscription(userId) } returns mockSubscription

        // When
        getSubscriptionName(userId).getOrNull()?.value
        val name = getSubscriptionName(userId).getOrNull()?.value

        // Then
        assertEquals(BasePlan, name)
        coVerify(exactly = 1) { getCurrentSubscription(userId) }
    }

    @Test
    fun `should return free when the subscription does not exist`() = runTest {
        // Given
        val userId = UserSample.Primary.userId
        coEvery { getCurrentSubscription(userId) } returns emptySubscription

        // When
        val name = getSubscriptionName(userId).getOrNull()

        // Then
        assertEquals(FreePlan, name?.value)
    }

    @Test
    fun `should return an error when the underlying UC throws`() = runTest {
        // Given
        val userId = UserSample.Primary.userId
        coEvery { getCurrentSubscription(userId) } throws Exception()

        // When
        val value = getSubscriptionName(userId)

        // Then
        assertIs<GetSubscriptionName.GetSubscriptionNameError>(value.leftOrNull())
    }

    private companion object {

        const val BasePlan = "mail2022"
        const val FreePlan = "free"
    }
}
