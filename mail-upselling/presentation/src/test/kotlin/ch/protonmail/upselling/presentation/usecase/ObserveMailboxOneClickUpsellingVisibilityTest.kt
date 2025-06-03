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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.usecase.GetPromotionStatus
import ch.protonmail.android.mailupselling.domain.usecase.PromoStatus
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.ObserveOneClickUpsellingEnabled
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveDriveSpotlightVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveMailboxOneClickUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingOneClickOnCooldown
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import me.proton.core.user.domain.entity.User
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveMailboxOneClickUpsellingVisibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val getPromotionStatus = mockk<GetPromotionStatus>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val observeOneClickUpsellingEnabled = mockk<ObserveOneClickUpsellingEnabled>()
    private val userHasPendingPurchases = mockk<UserHasPendingPurchases>()
    private val canUpgradeFromMobile = mockk<CanUpgradeFromMobile>()
    private val observeUpsellingOneClickOnCooldown = mockk<ObserveUpsellingOneClickOnCooldown>()
    private val alwaysShowOneClickUpselling = mockk<Provider<Boolean>>()
    private val observeDriveSpotlightVisibility = mockk<ObserveDriveSpotlightVisibility>()
    private val sut: ObserveMailboxOneClickUpsellingVisibility
        get() = ObserveMailboxOneClickUpsellingVisibility(
            observePrimaryUser,
            purchaseManager,
            observeOneClickUpsellingEnabled,
            observeUpsellingOneClickOnCooldown,
            canUpgradeFromMobile,
            getPromotionStatus,
            userHasPendingPurchases,
            alwaysShowOneClickUpselling.get(),
            observeDriveSpotlightVisibility
        )


    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { observeDriveSpotlightVisibility.invoke(any()) } returns flowOf(false)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return hidden if observed user is null`() = runTest {
        // Given
        expectedUser(null)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden is one click upselling is not enabled`() = runTest {
        // Given
        expectedUser(null)
        expectOneClickUpsellingEnabledValue(false)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden if one click upselling value is null`() = runTest {
        // Given
        expectedUser(null)
        expectOneClickUpsellingEnabledValue(null)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden if user can not upgrade from mobile`() = runTest {
        // Given
        expectedUser(null)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(UserSample.Primary.userId, false)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden if user has pending purchases`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden if user has no available plans`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NO_PLANS)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return normal if user has available plans, no pending subs and FF are enabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NORMAL)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.NORMAL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return normal if visibility logic is on cooldown and FF is force enabled to always show`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOnCooldown()
        expectOneClickButtonAlwaysShown(true)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NORMAL)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.NORMAL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return hidden if visibility logic is on cooldown and FF isnt force enabled to always show`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOnCooldown()
        expectOneClickButtonAlwaysShown(false)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NORMAL)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.HIDDEN, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return normal if visibility logic is not cooldown and FF is force enabled to always show`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectOneClickUpsellingEnabledValue(true)
        expectLastSeenThresholdOffCooldown()
        expectOneClickButtonAlwaysShown(true)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NORMAL)

        // When + Then
        sut().test {
            assertEquals(UpsellingVisibility.NORMAL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return normal if visibility logic is not on cooldown and FF is not force enabled to always show`() =
        runTest {
            // Given
            expectedUser(UserSample.Primary)
            expectOneClickUpsellingEnabledValue(true)
            expectLastSeenThresholdOffCooldown()
            expectOneClickButtonAlwaysShown(false)
            expectPurchases(listOf(mockk<Purchase>()))
            expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
            expectPendingPurchasesValue(UserSample.Primary.userId, false)
            expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.NORMAL)

            // When + Then
            sut().test {
                assertEquals(UpsellingVisibility.NORMAL, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `should return promo if visibility logic is not on cooldown and FF is not force enabled, for promo`() =
        runTest {
            // Given
            expectedUser(UserSample.Primary)
            expectOneClickUpsellingEnabledValue(true)
            expectLastSeenThresholdOffCooldown()
            expectOneClickButtonAlwaysShown(false)
            expectPurchases(listOf(mockk<Purchase>()))
            expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
            expectPendingPurchasesValue(UserSample.Primary.userId, false)
            expectUsePromoStatus(UserSample.Primary.userId, PromoStatus.PROMO)

            // When + Then
            sut().test {
                assertEquals(UpsellingVisibility.PROMO, awaitItem())
                awaitComplete()
            }
        }

    private fun expectedUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectPurchases(list: List<Purchase>) {
        every { purchaseManager.observePurchases() } returns flowOf(list)
    }

    private fun expectLastSeenThresholdOnCooldown() {
        every { observeUpsellingOneClickOnCooldown() } returns flowOf(true)
    }

    private fun expectLastSeenThresholdOffCooldown() {
        every { observeUpsellingOneClickOnCooldown() } returns flowOf(false)
    }

    private fun expectOneClickUpsellingEnabledValue(value: Boolean?) {
        val featureFlag = value?.let {
            FeatureFlag(
                userId = null,
                featureId = mockk(),
                scope = mockk(),
                defaultValue = false,
                value = it
            )
        }
        every { observeOneClickUpsellingEnabled(null) } returns flowOf(featureFlag)
    }

    private fun expectUsePromoStatus(userId: UserId, value: PromoStatus) {
        coEvery { getPromotionStatus(userId) } returns value
    }

    private fun expectPendingPurchasesValue(userId: UserId, value: Boolean) {
        coEvery { userHasPendingPurchases(any(), userId) } returns value
    }

    private fun expectCanUpgradeFromMobile(userId: UserId, value: Boolean) {
        coEvery { canUpgradeFromMobile(userId) } returns value
    }

    private fun expectOneClickButtonAlwaysShown(value: Boolean) {
        every { alwaysShowOneClickUpselling.get() } returns value
    }
}
