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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PurchaseFlow
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellExperimentFlag
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeAttempt
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeCancelledByUser
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeError
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeSuccess
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.usecase.ObserveMonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.usecase.RecordMonthlyToYearlyOptOut
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.domain.usecase.UpsellVariantPlan
import ch.protonmail.android.mailupselling.presentation.viewmodel.MonthlyToYearlyUpsellSheetViewModel.Companion.EntryPointKey
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MonthlyToYearlyUpsellSheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(EntryPointKey) } returns UpsellEntryPoint.SETTINGS_UPSELL.name
    }
    private val primaryUserId = MutableStateFlow<UserId?>(UserIdTestData.userId)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns primaryUserId
    }
    private val observeMonthlyToYearlyOffer = mockk<ObserveMonthlyToYearlyOffer>()
    private val recordMonthlyToYearlyOptOut = mockk<RecordMonthlyToYearlyOptOut> {
        coEvery { this@mockk(UserIdTestData.userId) } just Runs
    }
    private val eventLoopRepository = mockk<EventLoopRepository> {
        coEvery { triggerAndWait(UserIdTestData.userId) } just Runs
    }
    private val resetPlanUpgradesCache = mockk<ResetPlanUpgradesCache> {
        every { this@mockk() } just Runs
    }
    private val recordUpsellButtonTapped = mockk<RecordUpsellButtonTapped> {
        coEvery { this@mockk(any(), any()) } just Runs
    }
    private val recordUpgradeAttempt = mockk<RecordUpgradeAttempt> {
        coEvery { this@mockk(any(), any(), any()) } just Runs
    }
    private val recordUpgradeCancelledByUser = mockk<RecordUpgradeCancelledByUser> {
        coEvery { this@mockk(any(), any(), any()) } just Runs
    }
    private val recordUpgradeError = mockk<RecordUpgradeError> {
        coEvery { this@mockk(any(), any(), any()) } just Runs
    }
    private val recordUpgradeSuccess = mockk<RecordUpgradeSuccess> {
        coEvery { this@mockk(any(), any(), any()) } just Runs
    }
    private val appScope = CoroutineScope(mainDispatcherRule.testDispatcher)
    private val offer = mockk<MonthlyToYearlyOffer> {
        every { plan } returns UpsellVariantPlan.MAIL_PLUS
    }

    private fun viewModel() = MonthlyToYearlyUpsellSheetViewModel(
        savedStateHandle,
        observePrimaryUserId,
        observeMonthlyToYearlyOffer,
        recordMonthlyToYearlyOptOut,
        eventLoopRepository,
        resetPlanUpgradesCache,
        recordUpsellButtonTapped,
        recordUpgradeAttempt,
        recordUpgradeCancelledByUser,
        recordUpgradeError,
        recordUpgradeSuccess,
        appScope
    )

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `loads the offer and records the tap with the entry point from the route`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right())

        // When / Then
        viewModel().offer.test {
            assertEquals(offer, awaitItem())
        }
        coVerify {
            recordUpsellButtonTapped(UserIdTestData.userId, generalDimensions(UpsellEntryPoint.SETTINGS_UPSELL))
        }
    }

    @Test
    fun `keeps the offer it opened with when the eligibility later drops`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right(), null.right())

        // When / Then
        viewModel().offer.test {
            assertEquals(offer, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `waits through a failed check for the offer`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns
            flowOf(DataError.Local.Unknown.left(), offer.right())

        // When / Then
        viewModel().offer.test {
            assertEquals(offer, awaitItem())
        }
    }

    @Test
    fun `falls back to the navbar entry point when the route carries none`() = runTest {
        // Given
        every { savedStateHandle.get<String>(EntryPointKey) } returns null
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right())

        // When
        viewModel().offer.test { awaitItem() }

        // Then
        coVerify {
            recordUpsellButtonTapped(UserIdTestData.userId, generalDimensions(UpsellEntryPoint.NAVBAR_UPSELL))
        }
    }

    @Test
    fun `records the opt-out and reloads the upgrades`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right())

        // When
        viewModel().recordDoNotShowAgain()

        // Then
        coVerify { recordMonthlyToYearlyOptOut(UserIdTestData.userId) }
        verify { resetPlanUpgradesCache() }
    }

    @Test
    fun `refreshes the subscription and reloads the upgrades after switching to yearly`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right())

        // When
        viewModel().onSwitchedToYearly()

        // Then
        coVerify { eventLoopRepository.triggerAndWait(UserIdTestData.userId) }
        verify { resetPlanUpgradesCache() }
    }

    @Test
    fun `records the upgrade attempt with the yearly plan dimensions`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns flowOf(offer.right())

        // When
        viewModel().recordUpgradeAttempt()

        // Then
        coVerify {
            recordUpgradeAttempt(
                UserIdTestData.userId,
                generalDimensions(UpsellEntryPoint.SETTINGS_UPSELL),
                PlanSpecificDimensions(
                    selectedPlan = "mail2022",
                    selectedCycle = "Yearly",
                    upsellIsPromotional = false,
                    purchaseFlow = PurchaseFlow.SDK
                )
            )
        }
    }

    @Test
    fun `records nothing before the offer is loaded`() = runTest {
        // Given
        every { observeMonthlyToYearlyOffer(UserIdTestData.userId) } returns emptyFlow()

        // When
        viewModel().recordUpgradeCancelledByUser()

        // Then
        coVerify(exactly = 0) { recordUpgradeCancelledByUser(any(), any(), any()) }
    }

    private fun generalDimensions(entryPoint: UpsellEntryPoint) = GeneralDimensions(
        upsellEntryPoint = entryPoint,
        planBeforeUpgrade = "mail2022",
        modalVariant = UpsellModalVariant.MONTHLY_TO_YEARLY_PLUS,
        upsellExperimentFlag = UpsellExperimentFlag(flagName = "MailMobileMonthlyToYearlyUpsell"),
        promoCampaign = null
    )
}
