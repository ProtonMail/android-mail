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

import app.cash.turbine.test
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpsellingButtonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recordUpsellButtonTapped = mockk<RecordUpsellButtonTapped>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns MutableStateFlow<UserId?>(UserIdTestData.userId)
    }
    private val observeVisibilityUseCase = mockk<ObserveUpsellingVisibility>()

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should propagate the use case visibility (normal)`() = runTest {
        // Given
        every {
            observeVisibilityUseCase(UpsellingEntryPoint.Feature.MobileSignature)
        } returns flowOf(UpsellingVisibility.Normal.MailPlus)
        val viewModel = UpsellingButtonViewModel(
            UpsellingEntryPoint.Feature.MobileSignature,
            recordUpsellButtonTapped,
            observePrimaryUserId,
            observeVisibilityUseCase
        )

        // When
        viewModel.state.test {
            // Then
            assertEquals(UpsellingState(UpsellingVisibility.Normal.MailPlus), awaitItem())
        }
    }

    @Test
    fun `should propagate the use case visibility (promo)`() = runTest {
        // Given
        every {
            observeVisibilityUseCase(UpsellingEntryPoint.Feature.MobileSignature)
        } returns flowOf(UpsellingVisibility.Promotional.IntroductoryPrice)
        val viewModel = UpsellingButtonViewModel(
            UpsellingEntryPoint.Feature.MobileSignature,
            recordUpsellButtonTapped,
            observePrimaryUserId,
            observeVisibilityUseCase
        )

        // When
        viewModel.state.test {
            // Then
            assertEquals(UpsellingState(UpsellingVisibility.Promotional.IntroductoryPrice), awaitItem())
        }
    }

    @Test
    fun `should propagate the use case visibility (hidden)`() = runTest {
        // Given
        every {
            observeVisibilityUseCase(UpsellingEntryPoint.Feature.MobileSignature)
        } returns flowOf(UpsellingVisibility.Hidden)
        val viewModel = UpsellingButtonViewModel(
            UpsellingEntryPoint.Feature.MobileSignature,
            recordUpsellButtonTapped,
            observePrimaryUserId,
            observeVisibilityUseCase
        )

        // When
        viewModel.state.test {
            // Then
            assertEquals(UpsellingState(UpsellingVisibility.Hidden), awaitItem())
        }
    }

    @Test
    fun `should record upsell button tapped`() = runTest {
        // Given
        every {
            observeVisibilityUseCase(UpsellingEntryPoint.Feature.Navbar)
        } returns flowOf(UpsellingVisibility.Normal.MailPlus)
        val viewModel = UpsellingButtonViewModel(
            UpsellingEntryPoint.Feature.Navbar,
            recordUpsellButtonTapped,
            observePrimaryUserId,
            observeVisibilityUseCase
        )
        coEvery {
            recordUpsellButtonTapped(
                UserIdTestData.userId,
                GeneralDimensions(
                    upsellEntryPoint = UpsellEntryPoint.NAVBAR_UPSELL,
                    planBeforeUpgrade = "Free plan",
                    modalVariant = UpsellModalVariant.COMPARISON_PLUS,
                    upsellFeatureFlags = UpsellFeatureFlags(
                        parentFlagName = "MailAndroidV7UnlimitedPlanPlacementRegions",
                        childFlagName = "MailAndroidV7UnlimitedPlanPlacementExperiment"
                    )
                )
            )
        } just runs

        // When
        viewModel.recordUpsellButtonTapped(UpsellEntryPoint.NAVBAR_UPSELL, UpsellModalVariant.COMPARISON_PLUS)

        // Then
        coVerify(exactly = 1) {
            recordUpsellButtonTapped(
                UserIdTestData.userId,
                GeneralDimensions(
                    upsellEntryPoint = UpsellEntryPoint.NAVBAR_UPSELL,
                    planBeforeUpgrade = "Free plan",
                    modalVariant = UpsellModalVariant.COMPARISON_PLUS,
                    upsellFeatureFlags = UpsellFeatureFlags(
                        parentFlagName = "MailAndroidV7UnlimitedPlanPlacementRegions",
                        childFlagName = "MailAndroidV7UnlimitedPlanPlacementExperiment"
                    )
                )
            )
        }
    }
}
