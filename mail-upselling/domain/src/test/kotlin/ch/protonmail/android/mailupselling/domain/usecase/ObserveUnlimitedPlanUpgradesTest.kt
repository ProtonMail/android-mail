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

import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.UnlimitedMailProduct.MonthlyProductOfferDetail
import ch.protonmail.android.testdata.upselling.UpsellingTestData.UnlimitedMailProduct.YearlyProductDetail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveUnlimitedPlanUpgradesTest {

    private val userId = UserId("user-id")

    private val availableUpgradesCache = mockk<AvailableUpgradesCache>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        coEvery { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeUnlimitedPlanUpgrades = ObserveUnlimitedPlanUpgrades(
        availableUpgradesCache,
        observePrimaryUserId
    )

    @Test
    fun `should return filtered plans with unlimited only ids when invoked`() = runTest {
        // Given
        val expectedOffers = listOf(MonthlyProductOfferDetail, YearlyProductDetail)

        every { observePrimaryUserId() } returns flowOf(userId)

        coEvery { availableUpgradesCache.observe(userId) } returns flowOf(
            buildList {
                add(UpsellingTestData.MailPlusProducts.MonthlyProductOfferList)
                add(UpsellingTestData.MailPlusProducts.YearlyProductOfferList)
                add(UpsellingTestData.UnlimitedMailProduct.MonthlyProductOfferList)
                add(UpsellingTestData.UnlimitedMailProduct.YearlyProductOfferList)
            }
        )

        // When
        val actualPlans = observeUnlimitedPlanUpgrades().first()

        // Then
        assertEquals(expectedOffers, actualPlans)
    }
}
