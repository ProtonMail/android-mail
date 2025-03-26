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

import app.cash.turbine.test
import ch.protonmail.android.mailupselling.domain.usecase.CurrentPurchasesState
import ch.protonmail.android.mailupselling.domain.usecase.ObserveCurrentPurchasesState
import ch.protonmail.upselling.domain.UpsellingTestData.BasePurchase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveCurrentPurchasesStateTest {

    private val purchaseManager: PurchaseManager = mockk()

    private val testDispatcher: TestDispatcher by lazy {
        StandardTestDispatcher()
    }

    private val sut by lazy {
        ObserveCurrentPurchasesState(
            purchaseManager = purchaseManager
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emit not applicable if neither purchases nor status are applicable`() = runTest {
        // Given
        expectedPurchases(flowOf(emptyList()))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.NotApplicable, firstItem)
            awaitComplete()
        }
    }

    @Test
    fun `emit pending if there are pending purchases`() = runTest {
        // Given
        expectedPurchases(flowOf(listOf(pendingPurchase)))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.Pending, firstItem)
            awaitComplete()
        }
    }

    @Test
    fun `emit NotApplicable if the purchase failed`() = runTest {
        // Given
        expectedPurchases(flowOf(listOf(failedPurchase)))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.NotApplicable, firstItem)
            awaitComplete()
        }
    }

    @Test
    fun `emit deleted if the purchase got deleted`() = runTest {
        // Given
        expectedPurchases(flowOf(listOf(deletedPurchase)))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.Deleted, firstItem)
            awaitComplete()
        }
    }

    @Test
    fun `emit AcknowledgedOrSubscribed if there is an ack purchase`() = runTest {
        // Given
        expectedPurchases(flowOf(listOf(ackPurchase)))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")), firstItem)
            awaitComplete()
        }
    }

    @Test
    fun `emit AcknowledgedOrSubscribed if there is a subscribed purchase`() = runTest {
        // Given
        expectedPurchases(flowOf(listOf(subscribedPurchase)))

        // When
        sut.invoke(SessionId).test {
            // Then
            val firstItem = awaitItem()
            assertEquals(CurrentPurchasesState.AcknowledgedOrSubscribed(listOf("plan-name")), firstItem)
            awaitComplete()
        }
    }

    private fun expectedPurchases(flow: Flow<List<Purchase>>) {
        every { purchaseManager.observePurchases() } returns flow
    }

    companion object {

        private val SessionId = SessionId("session-id")
        private val Purchase = BasePurchase.copy(sessionId = SessionId, planName = "plan-name")
        private val ackPurchase = Purchase.copy(purchaseState = PurchaseState.Acknowledged)
        private val subscribedPurchase = Purchase.copy(purchaseState = PurchaseState.Subscribed)
        private val pendingPurchase = Purchase.copy(purchaseState = PurchaseState.Purchased)
        private val failedPurchase = Purchase.copy(purchaseState = PurchaseState.Failed, purchaseFailure = "failure")
        private val deletedPurchase = Purchase.copy(purchaseState = PurchaseState.Deleted, purchaseFailure = null)
    }
}
