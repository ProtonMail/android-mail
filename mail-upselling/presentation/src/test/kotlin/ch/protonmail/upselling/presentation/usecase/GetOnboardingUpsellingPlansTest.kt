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

package ch.protonmail.upselling.presentation.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.upselling.UpsellingTestData.MonthlyDynamicPlanInstance
import ch.protonmail.android.testdata.upselling.UpsellingTestData.YearlyDynamicPlanInstance
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class GetOnboardingUpsellingPlansTest(testName: String, private val testInput: TestInput) {

    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices>()
    private val getOnboardingUpsellingPlans = GetOnboardingUpsellingPlans(getDynamicPlansAdjustedPrices)
    private val userId = UserTestData.freeUser.userId

    @Test
    fun test() = runTest {
        // Given
        mockDynamicPlansUseCase(testInput.shouldThrow, testInput.dynamicPlans)

        // When
        val actualState = getOnboardingUpsellingPlans(userId)

        // Then
        assertEquals(testInput.expectedResult, actualState)
    }

    private fun mockDynamicPlansUseCase(shouldThrow: Boolean, expectedPlans: DynamicPlans) {
        if (shouldThrow) {
            coEvery { getDynamicPlansAdjustedPrices(userId) } throws Exception()
        } else {
            coEvery { getDynamicPlansAdjustedPrices(userId) } returns expectedPlans
        }
    }

    companion object {

        private val threePlansList = listOf(
            UpsellingTestData.UnlimitedPlan,
            UpsellingTestData.PlusPlan,
            UpsellingTestData.PlusPlan.copy(name = "mailplus2")
        )

        private val mismatchingPlansCycleList: List<DynamicPlan>
            get() {
                val firstEntry = UpsellingTestData.PlusPlan.instances.entries.iterator().next()
                val singleCycleInstance = mapOf(firstEntry.key to firstEntry.value)

                return listOf(
                    UpsellingTestData.UnlimitedPlan,
                    UpsellingTestData.PlusPlan.copy(instances = singleCycleInstance)
                )
            }

        private val plansWithEmptyPrices: List<DynamicPlan>
            get() {
                val planWithNoPrice = UpsellingTestData.PlusPlan.copy(
                    instances = mapOf(
                        1 to MonthlyDynamicPlanInstance.copy(price = emptyMap()),
                        12 to YearlyDynamicPlanInstance
                    )
                )

                return listOf(
                    UpsellingTestData.UnlimitedPlan,
                    planWithNoPrice
                )
            }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                testName = "should return no subscriptions when there are no plans",
                dynamicPlans = DynamicPlans(defaultCycle = null, emptyList()),
                expectedResult = GetOnboardingPlansError.NoPlans.left()
            ),
            TestInput(
                testName = "should return generic error subscriptions when the underlying UC throws",
                dynamicPlans = DynamicPlans(defaultCycle = null, emptyList()),
                expectedResult = GetOnboardingPlansError.GenericError.left(),
                shouldThrow = true
            ),
            TestInput(
                testName = "should return mismatching plans when the underlying UC returns only unlimited",
                dynamicPlans = DynamicPlans(defaultCycle = null, listOf(UpsellingTestData.UnlimitedPlan)),
                expectedResult = GetOnboardingPlansError.MismatchingPlans.left()
            ),
            TestInput(
                testName = "should return mismatching plans when the underlying UC returns only plus",
                dynamicPlans = DynamicPlans(defaultCycle = null, listOf(UpsellingTestData.PlusPlan)),
                expectedResult = GetOnboardingPlansError.MismatchingPlans.left()
            ),
            TestInput(
                testName = "should return mismatching plans when there are more than 2 plans",
                dynamicPlans = DynamicPlans(defaultCycle = null, threePlansList),
                expectedResult = GetOnboardingPlansError.MismatchingPlans.left()
            ),
            TestInput(
                testName = "should return mismatching cycles when plan cycles are not as expected",
                dynamicPlans = DynamicPlans(defaultCycle = null, mismatchingPlansCycleList),
                expectedResult = GetOnboardingPlansError.MismatchingPlanCycles.left()
            ),
            TestInput(
                testName = "should return mismatching cycles when plan cycles have empty prices",
                dynamicPlans = DynamicPlans(defaultCycle = null, plansWithEmptyPrices),
                expectedResult = GetOnboardingPlansError.MismatchingPlanCycles.left()
            ),
            TestInput(
                testName = "should return the plans list when there are two instances with two cycles each",
                dynamicPlans = UpsellingTestData.DynamicPlans,
                expectedResult = UpsellingTestData.DynamicPlans.right()
            )
        ).map { arrayOf(it.testName, it) }
    }

    data class TestInput(
        val testName: String,
        val dynamicPlans: DynamicPlans,
        val expectedResult: Either<GetOnboardingPlansError, DynamicPlans>,
        val shouldThrow: Boolean = false
    )
}
