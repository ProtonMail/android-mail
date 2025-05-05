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

package ch.protonmail.upselling.presentation.viewmodel

import java.time.Instant
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType.Upgrade
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.domain.usecase.FilterDynamicPlansByUserSubscription
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanUiMapper
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.reducer.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.usecase.UpdateUpsellingOneClickLastTimestamp
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlans
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.user.domain.entity.User
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class UpsellingViewModelTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val getDynamicPlansAdjustedPrices = mockk<GetDynamicPlansAdjustedPrices>()
    private val filterDynamicPlansByUserSubscription = mockk<FilterDynamicPlansByUserSubscription>()
    private val dynamicPlanUiMapper = mockk<DynamicPlanUiMapper>(relaxed = true)
    private val upsellingContentReducer = UpsellingContentReducer(dynamicPlanUiMapper)
    private val updateLastSeenUpsellingTimestamp = mockk<UpdateUpsellingOneClickLastTimestamp>(relaxUnitFun = true)
    private val upsellingTelemetryRepository = mockk<UpsellingTelemetryRepository>(relaxUnitFun = true)
    private val expectedUpsellingEntryPoint = UpsellingEntryPoint.Feature.ContactGroups

    private val viewModel: UpsellingViewModel by lazy {
        UpsellingViewModel(
            expectedUpsellingEntryPoint,
            observePrimaryUser,
            getDynamicPlansAdjustedPrices,
            filterDynamicPlansByUserSubscription,
            updateLastSeenUpsellingTimestamp,
            upsellingTelemetryRepository,
            upsellingContentReducer
        )
    }

    private val user = UserSample.Primary
    private val dynamicPlans = mockk<DynamicPlans>()
    private val expectedDynamicPlan = mockk<DynamicPlan>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Instant::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit loading state at start`() = runTest {
        every { observePrimaryUser() } returns flowOf()

        // When + Then
        viewModel.state.test {
            assertEquals(UpsellingScreenContentState.Loading, awaitItem())
        }
    }

    @Test
    fun `should emit error when user is null`() = runTest {
        // Given
        expectPrimaryUser(null)

        // When + Then
        viewModel.state.test {
            assertIs<UpsellingScreenContentState.Error>(awaitItem())
        }
    }

    @Test
    fun `should emit error when dynamic plans fail to be fetched`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlansError(user.userId)

        // When + Then
        viewModel.state.test {
            assertIs<UpsellingScreenContentState.Error>(awaitItem())
        }
    }

    @Test
    fun `should emit error when there are no subscriptions`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        expectEmptySubscriptionOptions(user.userId, dynamicPlans)

        // When + Then
        viewModel.state.test {
            assertIs<UpsellingScreenContentState.Error>(awaitItem())
        }
    }

    @Test
    fun `should emit data when there are subscriptions`() = runTest {
        // Given
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        expectSubscriptionOptions(user.userId, dynamicPlans, listOf(expectedDynamicPlan))

        // When + Then
        viewModel.state.test {
            assertIs<UpsellingScreenContentState.Data>(awaitItem())
        }
    }

    @Test
    fun `should call the UC with the expected value when updating the last seen timestamp`() = runTest {
        // Given
        val expectedInstantLongValue = 1L
        mockInstant(expectedInstantLongValue)
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        expectSubscriptionOptions(user.userId, dynamicPlans, listOf(expectedDynamicPlan))

        // When
        viewModel.updateLastSeenTimestamp()

        // Then
        coVerify { updateLastSeenUpsellingTimestamp.invoke(expectedInstantLongValue) }
    }

    @Test
    fun `should call the UC with the expected event when tracking the upgrade attempt`() = runTest {
        // Given
        val expectedInstantLongValue = 1L
        mockInstant(expectedInstantLongValue)
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        expectSubscriptionOptions(user.userId, dynamicPlans, listOf(expectedDynamicPlan))
        val expectedPayload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1, isPromotional = false,
            isVariantB = false, isSocialProofVariant = false
        )

        // When
        viewModel.trackUpgradeAttempt(expectedPayload)

        // Then
        coVerify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(
                Upgrade.UpgradeAttempt(expectedPayload),
                expectedUpsellingEntryPoint
            )
        }
    }

    @Test
    fun `should call the UC with the expected event when tracking the purchase complete event`() = runTest {
        // Given
        val expectedInstantLongValue = 1L
        mockInstant(expectedInstantLongValue)
        expectPrimaryUser(user)
        expectDynamicPlans(user.userId, dynamicPlans)
        expectSubscriptionOptions(user.userId, dynamicPlans, listOf(expectedDynamicPlan))
        val expectedPayload = UpsellingTelemetryTargetPlanPayload(
            "plan", 1, isPromotional = false,
            isVariantB = false, isSocialProofVariant = true
        )

        // When
        viewModel.trackPurchaseCompleted(expectedPayload)

        // Then
        coVerify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(
                Upgrade.PurchaseCompleted(expectedPayload),
                expectedUpsellingEntryPoint
            )
        }
    }

    private fun expectPrimaryUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectDynamicPlans(userId: UserId, dynamicPlans: DynamicPlans) {
        coEvery { getDynamicPlansAdjustedPrices(userId) } returns dynamicPlans
    }

    private fun expectDynamicPlansError(userId: UserId) {
        coEvery { getDynamicPlansAdjustedPrices(userId) } throws Exception()
    }

    private fun expectEmptySubscriptionOptions(userId: UserId, plans: DynamicPlans) {
        coEvery { filterDynamicPlansByUserSubscription(userId, plans) } returns emptyList()
    }

    private fun expectSubscriptionOptions(
        userId: UserId,
        plans: DynamicPlans,
        expectedSubs: List<DynamicPlan>
    ) {
        coEvery { filterDynamicPlansByUserSubscription(userId, plans) } returns expectedSubs
    }

    private fun mockInstant(instant: Long) {
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns instant }
    }
}
