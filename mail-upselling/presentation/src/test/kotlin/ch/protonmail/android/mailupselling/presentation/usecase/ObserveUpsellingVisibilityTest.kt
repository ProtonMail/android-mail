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

package ch.protonmail.android.mailupselling.presentation.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsession.domain.usecase.ObserveUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlanUpgrades
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveUpsellingVisibilityTest {

    private val resolveUpsellingVisibilityForPlans = mockk<ResolveUpsellingVisibilityForPlans>()
    private val observePlanUpgrades = mockk<ObservePlanUpgrades>()
    private val observeUser = mockk<ObserveUser>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()

    private val isUpsellEnabled = mockk<FeatureFlag<Boolean>>()

    private val playServicesAvailable = mockk<Provider<Boolean>> {
        every { this@mockk.get() } returns true
    }

    private lateinit var observeUpselling: ObserveUpsellingVisibility

    @BeforeTest
    fun setup() {
        every { observePrimaryUserId() } returns flowOf(UserIdTestData.userId)

        observeUpselling = ObserveUpsellingVisibility(
            resolveUpsellingVisibilityForPlans,
            observePlanUpgrades,
            observeUser,
            observePrimaryUserId,
            playServicesAvailable,
            isUpsellEnabled
        )
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return hidden when play services are unavailable`() = runTest {
        // Given
        every { playServicesAvailable.get() } returns false

        // When
        observeUpselling(UpsellingEntryPoint.Feature.Navbar).test {
            // Then
            assertEquals(UpsellingVisibility.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden when FF is off`() = runTest {
        // Given
        coEvery { isUpsellEnabled.get() } returns false

        // When
        observeUpselling(UpsellingEntryPoint.Feature.Navbar).test {
            // Then
            assertEquals(UpsellingVisibility.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should proxy getPromotionStatus result - hidden due to no plans`() = runTest {
        // Given
        every { observeUser(userId = UserIdTestData.userId) } returns flowOf(UserTestData.freeUser.right())
        coEvery { observePlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(listOf())
        coEvery { resolveUpsellingVisibilityForPlans(any()) } returns UpsellingVisibility.Hidden
        coEvery { isUpsellEnabled.get() } returns true

        // When
        observeUpselling(UpsellingEntryPoint.Feature.Navbar).test {
            // Then
            assertEquals(UpsellingVisibility.Hidden, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should proxy getPromotionStatus result when hide signal returns false - normal MailPlus`() = runTest {
        // Given
        every { observeUser(userId = UserIdTestData.userId) } returns flowOf(UserTestData.freeUser.right())
        val expectedPlans = listOf(mockk<ProductOfferDetail>())
        coEvery { observePlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedPlans)
        coEvery { resolveUpsellingVisibilityForPlans(expectedPlans) } returns UpsellingVisibility.Normal.MailPlus
        coEvery { isUpsellEnabled.get() } returns true

        // When
        observeUpselling(UpsellingEntryPoint.Feature.Navbar).test {
            // Then
            assertEquals(UpsellingVisibility.Normal.MailPlus, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should proxy getPromotionStatus result when hide signal returns false - promo`() = runTest {
        // Given
        every { observeUser(userId = UserIdTestData.userId) } returns flowOf(UserTestData.freeUser.right())
        val expectedPlans = listOf(mockk<ProductOfferDetail>())
        coEvery { observePlanUpgrades(UpsellingEntryPoint.Feature.Navbar) } returns flowOf(expectedPlans)
        coEvery {
            resolveUpsellingVisibilityForPlans(expectedPlans)
        } returns UpsellingVisibility.Promotional.IntroductoryPrice
        coEvery { isUpsellEnabled.get() } returns true

        // When
        observeUpselling(UpsellingEntryPoint.Feature.Navbar).test {
            // Then
            assertEquals(UpsellingVisibility.Promotional.IntroductoryPrice, awaitItem())
            awaitComplete()
        }
    }
}
