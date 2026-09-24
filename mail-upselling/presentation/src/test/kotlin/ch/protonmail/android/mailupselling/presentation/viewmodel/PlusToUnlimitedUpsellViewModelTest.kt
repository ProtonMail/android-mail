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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellExperimentFlag
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlusToUnlimitedEligibility
import ch.protonmail.android.mailupselling.domain.usecase.ResolvePlanBeforeUpgrade
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveActiveUpsellTheme
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlusToUnlimitedUpsellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns MutableStateFlow<UserId?>(UserIdTestData.userId)
    }
    private val observePlusToUnlimitedEligibility = mockk<ObservePlusToUnlimitedEligibility>()
    private val resolveActiveUpsellTheme = mockk<ResolveActiveUpsellTheme> {
        every { this@mockk(UserIdTestData.userId) } returns UpsellContentTheme.Drive
    }

    private val resolvePlanBeforeUpgrade = mockk<ResolvePlanBeforeUpgrade> {
        coEvery { this@mockk() } returns "mail2022"
    }
    private val recordUpsellButtonTapped = mockk<RecordUpsellButtonTapped> {
        coEvery { this@mockk(any(), any()) } just Runs
    }

    private fun viewModel() = PlusToUnlimitedUpsellViewModel(
        observePrimaryUserId,
        observePlusToUnlimitedEligibility,
        resolveActiveUpsellTheme,
        resolvePlanBeforeUpgrade,
        recordUpsellButtonTapped
    )

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `resolves the active theme when the user is eligible`() = runTest {
        // Given
        every { observePlusToUnlimitedEligibility(UserIdTestData.userId) } returns flowOf(true.right())

        // When / Then
        viewModel().theme.test {
            assertEquals(UpsellContentTheme.Drive, awaitItem())
        }
    }

    @Test
    fun `has no theme when the user is not eligible`() = runTest {
        // Given
        every { observePlusToUnlimitedEligibility(UserIdTestData.userId) } returns flowOf(false.right())

        // When / Then
        viewModel().theme.test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `keeps the last known verdict when the eligibility check fails`() = runTest {
        // Given
        every { observePlusToUnlimitedEligibility(UserIdTestData.userId) } returns
            flowOf(true.right(), DataError.Local.Unknown.left())

        // When / Then
        viewModel().theme.test {
            assertEquals(UpsellContentTheme.Drive, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `records the upsell button tap with the plus to unlimited dimensions`() = runTest {
        // Given
        every { observePlusToUnlimitedEligibility(UserIdTestData.userId) } returns flowOf(true.right())
        val viewModel = viewModel()

        // When
        viewModel.theme.test {
            awaitItem()
            viewModel.recordUpsellButtonTapped()
        }

        // Then
        coVerify {
            recordUpsellButtonTapped(
                userId = UserIdTestData.userId,
                generalDimensions = GeneralDimensions(
                    upsellEntryPoint = UpsellEntryPoint.NAVBAR_UPSELL,
                    planBeforeUpgrade = "mail2022",
                    modalVariant = UpsellModalVariant.COMPARISON_UNLIMITED,
                    upsellExperimentFlag = UpsellExperimentFlag(flagName = "MailAndroidV7PlusToUnlimitedUpsell"),
                    promoCampaign = "drive"
                )
            )
        }
    }
}
