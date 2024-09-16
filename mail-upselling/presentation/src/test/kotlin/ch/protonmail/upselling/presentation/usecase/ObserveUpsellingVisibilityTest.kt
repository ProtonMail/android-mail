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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.UserHasAvailablePlans
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingContactGroupsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingFoldersEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingLabelsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingPostOnboardingEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.ObserveOneClickUpsellingEnabled
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
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

internal class ObserveUpsellingVisibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val userHasAvailablePlans = mockk<UserHasAvailablePlans>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val userHasPendingPurchases = mockk<UserHasPendingPurchases>()
    private val canUpgradeFromMobile = mockk<CanUpgradeFromMobile>()
    private val provideUpsellingMobileSignatureEnabled = mockk<Provider<Boolean>>()
    private val isUpsellingLabelsEnabled = mockk<IsUpsellingLabelsEnabled>()
    private val isUpsellingFoldersEnabled = mockk<IsUpsellingFoldersEnabled>()
    private val isUpsellingContactGroupsEnabled = mockk<IsUpsellingContactGroupsEnabled>()
    private val isUpsellingPostOnboardingEnabled = mockk<IsUpsellingPostOnboardingEnabled>()
    private val observeOneClickUpsellingEnabled = mockk<ObserveOneClickUpsellingEnabled>()
    private val sut: ObserveUpsellingVisibility
        get() = ObserveUpsellingVisibility(
            observePrimaryUser,
            purchaseManager,
            canUpgradeFromMobile,
            userHasAvailablePlans,
            userHasPendingPurchases,
            provideUpsellingMobileSignatureEnabled.get(),
            isUpsellingLabelsEnabled,
            isUpsellingFoldersEnabled,
            isUpsellingContactGroupsEnabled,
            observeOneClickUpsellingEnabled,
            isUpsellingPostOnboardingEnabled
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.MobileSignature, false)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false if observed user is null`() = runTest {
        // Given
        expectedUser(null)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, true)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user can not upgrade from mobile`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(false)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, true)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user has pending purchases`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, true)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user has no available plans`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, false)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, true)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if ContactGroups FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, false)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if Mailbox FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.Mailbox, false)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.Mailbox).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if Labels FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.Labels, false)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.Labels).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if Folders FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.Folders, false)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.Folders).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if MobileSignature FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.MobileSignature, false)

        // When + Then
        sut(UpsellingEntryPoint.BottomSheet.MobileSignature).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if PostOnboarding FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.PostOnboarding, false)

        // When + Then
        sut(UpsellingEntryPoint.PostOnboarding).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `should return true if user has no pending purchases, can upgrade from mobile, has available plans and FF is enabled`() =
        runTest {
            // Given
            expectedUser(UserSample.Primary)
            expectPurchases(listOf(mockk<Purchase>()))
            expectCanUpgradeFromMobile(true)
            expectPendingPurchasesValue(UserSample.Primary.userId, false)
            expectUserHasAvailablePlans(UserSample.Primary.userId, true)
            expectUpsellingFeatureFlag(UpsellingEntryPoint.BottomSheet.ContactGroups, true)

            // When + Then
            sut(UpsellingEntryPoint.BottomSheet.ContactGroups).test {
                assertEquals(true, awaitItem())
                awaitComplete()
            }
        }

    private fun expectedUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectPurchases(list: List<Purchase>) {
        every { purchaseManager.observePurchases() } returns flowOf(list)
    }

    private fun expectUserHasAvailablePlans(userId: UserId, value: Boolean) {
        coEvery { userHasAvailablePlans(userId) } returns value
    }

    private fun expectPendingPurchasesValue(userId: UserId, value: Boolean) {
        coEvery { userHasPendingPurchases(any(), userId) } returns value
    }

    private fun expectCanUpgradeFromMobile(value: Boolean) {
        coEvery { canUpgradeFromMobile() } returns value
    }

    private fun expectUpsellingFeatureFlag(upsellingEntryPoint: UpsellingEntryPoint, value: Boolean) {
        when (upsellingEntryPoint) {
            UpsellingEntryPoint.BottomSheet.ContactGroups -> every {
                isUpsellingContactGroupsEnabled.invoke()
            } returns value

            UpsellingEntryPoint.BottomSheet.Folders -> every { isUpsellingFoldersEnabled.invoke() } returns value

            UpsellingEntryPoint.BottomSheet.Labels -> every { isUpsellingLabelsEnabled.invoke() } returns value
            UpsellingEntryPoint.BottomSheet.Mailbox -> {
                val featureFlag = FeatureFlag(
                    userId = null,
                    featureId = mockk(),
                    scope = mockk(),
                    defaultValue = false,
                    value = value
                )
                every { observeOneClickUpsellingEnabled(null) } returns flowOf(featureFlag)
            }
            UpsellingEntryPoint.BottomSheet.MobileSignature -> every {
                provideUpsellingMobileSignatureEnabled.get()
            } returns value
            UpsellingEntryPoint.PostOnboarding -> every { isUpsellingPostOnboardingEnabled.invoke() } returns value
        }
    }


}
