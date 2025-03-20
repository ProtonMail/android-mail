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

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.UserHasAvailablePlans
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingContactGroupsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingFoldersEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsUpsellingLabelsEnabled
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.ObserveOneClickUpsellingEnabled
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveUpsellingVisibility
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
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResolveUpsellingVisibilityTest {

    private val userHasAvailablePlans = mockk<UserHasAvailablePlans>()
    private val userHasPendingPurchases = mockk<UserHasPendingPurchases>()
    private val canUpgradeFromMobile = mockk<CanUpgradeFromMobile>()
    private val provideUpsellingMobileSignatureEnabled = mockk<Provider<Boolean>>()
    private val isUpsellingLabelsEnabled = mockk<IsUpsellingLabelsEnabled>()
    private val isUpsellingFoldersEnabled = mockk<IsUpsellingFoldersEnabled>()
    private val isUpsellingContactGroupsEnabled = mockk<IsUpsellingContactGroupsEnabled>()
    private val observeOneClickUpsellingEnabled = mockk<ObserveOneClickUpsellingEnabled>()
    private val provideUpsellingAutoDeleteEnabled = mockk<Provider<Boolean>>()
    private val sut: ResolveUpsellingVisibility
        get() = ResolveUpsellingVisibility(
            canUpgradeFromMobile,
            userHasAvailablePlans,
            userHasPendingPurchases,
            provideUpsellingMobileSignatureEnabled.get(),
            isUpsellingLabelsEnabled,
            isUpsellingFoldersEnabled,
            isUpsellingContactGroupsEnabled,
            observeOneClickUpsellingEnabled,
            provideUpsellingAutoDeleteEnabled.get()
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.MobileSignature, false)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.AutoDelete, false)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false if user can not upgrade from mobile`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, false)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.ContactGroups, true)

        // When + Then
        val actual = sut(UserSample.Primary, emptyList(), UpsellingEntryPoint.Feature.ContactGroups)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if user has pending purchases`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.ContactGroups, true)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.ContactGroups)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if user has no available plans`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, false)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.ContactGroups, true)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.ContactGroups)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if ContactGroups FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.ContactGroups, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.ContactGroups)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if Mailbox FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.Mailbox, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.Mailbox)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if Labels FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.Labels, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.Labels)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if Folders FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.Folders, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.Folders)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if MobileSignature FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.MobileSignature, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.MobileSignature)
        assertEquals(false, actual)
    }

    @Test
    fun `should return false if AutoDelete FF is disabled`() = runTest {
        // Given
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)
        expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.AutoDelete, false)

        // When + Then
        val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.AutoDelete)
        assertEquals(false, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `should return true if user has no pending purchases, can upgrade from mobile, has available plans and FF is enabled`() =
        runTest {
            // Given
            expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
            expectPendingPurchasesValue(UserSample.Primary.userId, false)
            expectUserHasAvailablePlans(UserSample.Primary.userId, true)
            expectUpsellingFeatureFlag(UpsellingEntryPoint.Feature.ContactGroups, true)

            // When + Then
            val actual = sut(UserSample.Primary, listOf(mockk<Purchase>()), UpsellingEntryPoint.Feature.ContactGroups)
            assertEquals(true, actual)
        }

    private fun expectUserHasAvailablePlans(userId: UserId, value: Boolean) {
        coEvery { userHasAvailablePlans(userId) } returns value
    }

    private fun expectPendingPurchasesValue(userId: UserId, value: Boolean) {
        coEvery { userHasPendingPurchases(any(), userId) } returns value
    }

    private fun expectCanUpgradeFromMobile(userId: UserId, value: Boolean) {
        coEvery { canUpgradeFromMobile(userId) } returns value
    }

    private fun expectUpsellingFeatureFlag(upsellingEntryPoint: UpsellingEntryPoint.Feature, value: Boolean) {
        when (upsellingEntryPoint) {
            UpsellingEntryPoint.Feature.ContactGroups -> every {
                isUpsellingContactGroupsEnabled.invoke()
            } returns value

            UpsellingEntryPoint.Feature.Folders -> every { isUpsellingFoldersEnabled.invoke() } returns value

            UpsellingEntryPoint.Feature.Labels -> every { isUpsellingLabelsEnabled.invoke() } returns value
            UpsellingEntryPoint.Feature.Mailbox,
            UpsellingEntryPoint.Feature.MailboxPromo,
            UpsellingEntryPoint.Feature.Navbar -> {
                val featureFlag = FeatureFlag(
                    userId = null,
                    featureId = mockk(),
                    scope = mockk(),
                    defaultValue = false,
                    value = value
                )
                every { observeOneClickUpsellingEnabled(null) } returns flowOf(featureFlag)
            }
            UpsellingEntryPoint.Feature.MobileSignature -> every {
                provideUpsellingMobileSignatureEnabled.get()
            } returns value

            UpsellingEntryPoint.Feature.AutoDelete -> every {
                provideUpsellingAutoDeleteEnabled.get()
            } returns value
        }
    }
}
