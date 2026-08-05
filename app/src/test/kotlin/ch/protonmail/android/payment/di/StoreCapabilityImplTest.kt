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

package ch.protonmail.android.payment.di

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.PaymentManager
import me.proton.android.payment.billing.exception.StoreBillingUnavailableException
import me.proton.android.payment.billing.model.StoreProduct
import me.proton.android.payment.billing.usecase.AcknowledgePurchase
import me.proton.android.payment.billing.usecase.PurchaseStoreProduct
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoreCapabilityImplTest {

    private val paymentManager = mockk<PaymentManager>()

    private val purchaseStoreProduct = mockk<PurchaseStoreProduct>()
    private val acknowledgePurchase = mockk<AcknowledgePurchase>()

    // purchase/acknowledge use cases are absent (no :payment-billing-google).
    private val subject = StoreCapabilityImpl(
        acknowledgePurchase = Optional.empty(),
        purchaseStoreProduct = Optional.empty(),
        paymentManager = paymentManager
    )

    // purchase/acknowledge use cases are present (:payment-billing-google on the classpath).
    private val subjectWithBilling = StoreCapabilityImpl(
        acknowledgePurchase = Optional.of(acknowledgePurchase),
        purchaseStoreProduct = Optional.of(purchaseStoreProduct),
        paymentManager = paymentManager
    )

    @Test
    fun `getProducts returns SDK store products from legacy billing`() = runTest {
        val products = listOf<StoreProduct>(mockk(), mockk())
        coEvery { paymentManager.getSdkStoreProducts(listOf("a", "b")) } returns products

        val result = subject.getProducts(listOf("a", "b"))

        assertEquals(products, result.getOrNull())
    }

    @Test
    fun `getProducts wraps a thrown error in a failure result`() = runTest {
        coEvery { paymentManager.getSdkStoreProducts(any()) } throws RuntimeException("boom")

        val result = subject.getProducts(listOf("a"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `purchase reports billing unavailable while the SDK billing module is absent`() = runTest {
        val result = subject.purchase(productId = "id", offerToken = "token", userId = null)

        assertTrue(result.exceptionOrNull() is StoreBillingUnavailableException)
    }

    @Test
    fun `acknowledge reports billing unavailable while the SDK billing module is absent`() = runTest {
        val result = subject.acknowledge(orderId = "order")

        assertTrue(result.exceptionOrNull() is StoreBillingUnavailableException)
    }

    @Test
    fun `purchase delegates to PurchaseStoreProduct when billing is available`() = runTest {
        coEvery { purchaseStoreProduct.invoke("id", "token", null) } returns Result.success(Unit)

        val result = subjectWithBilling.purchase(productId = "id", offerToken = "token", userId = null)

        assertTrue(result.isSuccess)
        coVerify { purchaseStoreProduct.invoke("id", "token", null) }
    }

    @Test
    fun `acknowledge delegates to AcknowledgePurchase when billing is available`() = runTest {
        coEvery { acknowledgePurchase.invoke("order") } returns Result.success(Unit)

        val result = subjectWithBilling.acknowledge(orderId = "order")

        assertTrue(result.isSuccess)
        coVerify { acknowledgePurchase.invoke("order") }
    }
}
