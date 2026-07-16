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

import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResolveUpsellVariantTest {

    private val userId = UserId("user-id")
    private val upsellEligibilityRepository = mockk<UpsellEligibilityRepository>()
    private val resolveUpsellVariant = ResolveUpsellVariant(upsellEligibilityRepository)

    @Test
    fun `returns the eligible plan when the user is eligible for Unlimited`() = runTest {
        coEvery { upsellEligibilityRepository.getEligibleUpsellPlan(userId) } returns UpsellVariantPlan.UNLIMITED
        assertEquals(UpsellVariantPlan.UNLIMITED, resolveUpsellVariant(userId))
    }

    @Test
    fun `returns the eligible plan when the user is eligible for Mail Plus`() = runTest {
        coEvery { upsellEligibilityRepository.getEligibleUpsellPlan(userId) } returns UpsellVariantPlan.MAIL_PLUS
        assertEquals(UpsellVariantPlan.MAIL_PLUS, resolveUpsellVariant(userId))
    }

    @Test
    fun `falls back to the default plan when the user is not eligible`() = runTest {
        coEvery { upsellEligibilityRepository.getEligibleUpsellPlan(userId) } returns null
        assertEquals(UpsellVariantPlan.Default, resolveUpsellVariant(userId))
    }
}
