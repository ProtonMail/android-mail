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

package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailupselling.domain.model.BlackFridayPhase
import ch.protonmail.android.mailupselling.domain.model.SpringPromoPhase
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentBlackFridayPhase
import ch.protonmail.android.mailupselling.domain.usecase.GetCurrentSpringPromoPhase
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class PlanUpgradeMapperTest(
    @Suppress("unused") private val testName: String,
    val testInput: TestInput
) {

    private val getCurrentBlackFridayPhase = mockk<GetCurrentBlackFridayPhase>()
    private val getCurrentSpringPromoPhase = mockk<GetCurrentSpringPromoPhase>()
    private lateinit var planUpgradeMapper: PlanUpgradeMapper

    @BeforeTest
    fun setup() {
        planUpgradeMapper = PlanUpgradeMapper(
            getCurrentBlackFridayPhase,
            getCurrentSpringPromoPhase
        )
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should map to the correct variant`() = runTest {
        // Given
        coEvery { getCurrentBlackFridayPhase() } returns testInput.blackFridayPhase
        coEvery { getCurrentSpringPromoPhase() } returns testInput.springPromoPhase

        // When
        val actual = planUpgradeMapper.resolveVariant(
            testInput.monthlyInstance,
            testInput.yearlyInstance,
            testInput.entryPoint
        )

        // Then
        assertEquals(testInput.expectedVariant, actual)
    }

    companion object {

        data class TestInput(
            val monthlyInstance: ProductOfferDetail,
            val yearlyInstance: ProductOfferDetail,
            val entryPoint: UpsellingEntryPoint,
            val blackFridayPhase: BlackFridayPhase,
            val springPromoPhase: SpringPromoPhase,
            val expectedVariant: PlanUpgradeVariant
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "should return normal variant on non-promotional instances",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Navbar,
                    blackFridayPhase = BlackFridayPhase.None,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.Normal
                )
            ),
            arrayOf(
                "should return BF variant on promo instances for suitable entry points and active wave",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyPromoAndBFProductDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Navbar,
                    blackFridayPhase = BlackFridayPhase.Active.Wave2,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.BlackFriday.Wave2
                )
            ),
            arrayOf(
                "should return BF variant on promo instances for suitable entry points and active wave",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyPromoAndBFProductDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Sidebar,
                    blackFridayPhase = BlackFridayPhase.Active.Wave1,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.BlackFriday.Wave1
                )
            ),
            arrayOf(
                "should return intro variant on promo instances for suitable entry points and no active wave",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyPromoAndBFProductDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Navbar,
                    blackFridayPhase = BlackFridayPhase.None,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.IntroductoryPrice
                )
            ),
            arrayOf(
                "should return intro variant on intro-only promo instance",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyPromoProductOfferDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.MobileSignature,
                    blackFridayPhase = BlackFridayPhase.None,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.IntroductoryPrice
                )
            ),
            arrayOf(
                "should return intro variant on BF active but no compatible served offers for relevant entrypoints",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyPromoProductOfferDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Navbar,
                    blackFridayPhase = BlackFridayPhase.Active.Wave2,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.IntroductoryPrice
                )
            ),
            arrayOf(
                "should return normal variant on BF and spring promo not active for relevant entrypoints",
                TestInput(
                    monthlyInstance = UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                    yearlyInstance = UpsellingTestData.MailPlusProducts.YearlyProductOfferDetail,
                    entryPoint = UpsellingEntryPoint.Feature.Navbar,
                    blackFridayPhase = BlackFridayPhase.None,
                    springPromoPhase = SpringPromoPhase.None,
                    expectedVariant = PlanUpgradeVariant.Normal
                )
            )
        )
    }
}
