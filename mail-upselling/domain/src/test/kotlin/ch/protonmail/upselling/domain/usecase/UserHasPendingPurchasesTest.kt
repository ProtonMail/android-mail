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

package ch.protonmail.upselling.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class UserHasPendingPurchasesTest {

    private val sessionManager = mockk<SessionManager>()
    private val userHasPendingPurchases = UserHasPendingPurchases(sessionManager)

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false when purchases list is empty`() = runTest {
        // Given
        val purchases = emptyList<Purchase>()

        // When
        val actual = userHasPendingPurchases(purchases, UserId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when sessionId cannot be determined`() = runTest {
        // Given
        val purchases = listOf(FakeAcknowledgedPurchase)
        coEvery { sessionManager.getSessionId(UserId) } returns null

        // When
        val actual = userHasPendingPurchases(purchases, UserId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when there are no purchased or subscribed state purchases`() = runTest {
        // Given
        val purchases = listOf(
            FakeAcknowledgedPurchase,
            FakeFailedPurchase,
            FakePendingPurchase,
            FakeCancelledPurchase,
            FakeDeletedPurchase
        )

        coEvery { sessionManager.getSessionId(UserId) } returns SessionId(UserId.id)

        // When
        val actual = userHasPendingPurchases(purchases, UserId)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true when there are purchased state purchases`() = runTest {
        // Given
        val purchases = listOf(FakePurchasedPurchase)
        coEvery { sessionManager.getSessionId(UserId) } returns SessionId(UserId.id)

        // When
        val actual = userHasPendingPurchases(purchases, UserId)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return true when there are subscribed state purchases`() = runTest {
        // Given
        val purchases = listOf(FakeSubscribedPurchase)
        coEvery { sessionManager.getSessionId(UserId) } returns SessionId(UserId.id)

        // When
        val actual = userHasPendingPurchases(purchases, UserId)

        // Then
        assertTrue(actual)
    }

    private companion object {

        val UserId = UserSample.Primary.userId

        val BasePurchase = Purchase(
            sessionId = SessionId(UserId.id),
            planName = "Plan",
            planCycle = 1,
            purchaseState = PurchaseState.Acknowledged,
            purchaseFailure = null,
            paymentProvider = mockk(),
            paymentOrderId = null,
            paymentToken = null,
            paymentCurrency = mockk(),
            paymentAmount = 1L
        )

        val FakeFailedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Failed)
        val FakePurchasedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Purchased)
        val FakeSubscribedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Subscribed)
        val FakeAcknowledgedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Acknowledged)
        val FakePendingPurchase = BasePurchase.copy(purchaseState = PurchaseState.Pending)
        val FakeCancelledPurchase = BasePurchase.copy(purchaseState = PurchaseState.Cancelled)
        val FakeDeletedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Deleted)
    }
}
