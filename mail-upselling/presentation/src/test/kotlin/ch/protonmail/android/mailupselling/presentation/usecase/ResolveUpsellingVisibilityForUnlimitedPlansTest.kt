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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveUpsellingVisibilityForUnlimitedPlansTest {

    private val resolveUpsellingVisibilityUnlimitedPlans = ResolveUpsellingVisibilityForUnlimitedPlans()

    @Test
    fun `should return hidden when plans size is less than 2`() = runTest {
        // Given
        val plans = listOf<ProductOfferDetail>(mockk())

        // When
        val actual = resolveUpsellingVisibilityUnlimitedPlans(plans)

        // Then
        assertEquals(UpsellingVisibility.Hidden, actual)
    }

    @Test
    fun `should return normal when offer contains 2 plans`() = runTest {
        // Given
        val plans = listOf(
            UpsellingTestData.UnlimitedMailProduct.MonthlyProductOfferDetail,
            UpsellingTestData.UnlimitedMailProduct.YearlyProductDetail
        )

        // When
        val actual = resolveUpsellingVisibilityUnlimitedPlans(plans)

        // Then
        assertEquals(UpsellingVisibility.Normal.Unlimited, actual)
    }
}
