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

package ch.protonmail.android.mailupselling.presentation.payment

import android.content.Context
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.android.core.payment.presentation.component.PurchaseButtonAction
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessorNative
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.component.PurchaseButtonState
import me.proton.android.core.payment.presentation.model.Product
import me.proton.android.payment.common.exception.PaymentException
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SdkBackedPurchaseButtonProcessorTest {

    private val legacyProcessor = mockk<PurchaseButtonProcessorNative>()
    private val purchaseProduct = mockk<PurchaseProduct>()
    private val observeSessionState = mockk<ObserveSessionState>()
    private val accountEventBroadcaster = mockk<AccountEventBroadcaster>(relaxed = true)
    private val flag = mockk<FeatureFlag<Boolean>>()
    private val context = mockk<Context> {
        every { getString(R.string.payment_error_title) } returns ERROR_MESSAGE
    }

    private val subject = SdkBackedPurchaseButtonProcessor(
        context = context,
        legacyProcessor = legacyProcessor,
        purchaseProduct = Lazy { purchaseProduct },
        observeSessionState = Lazy { observeSessionState },
        getActivePurchases = Optional.empty(),
        accountEventBroadcaster = accountEventBroadcaster,
        sdkPurchaseEnabled = flag
    )

    private val product = Product.test

    @Test
    fun `when FF is OFF delegates to the legacy processor`() = runTest {
        coEvery { flag.get() } returns false
        every { legacyProcessor.onAction(PurchaseButtonAction.Init) } returns flowOf(PurchaseButtonState.Idle)

        val states = subject.onAction(PurchaseButtonAction.Init).toList()

        assertEquals(listOf(PurchaseButtonState.Idle), states)
        verify { legacyProcessor.onAction(PurchaseButtonAction.Init) }
    }

    @Test
    fun `when FF is ON a non-purchase action emits Idle without touching the legacy processor`() = runTest {
        coEvery { flag.get() } returns true

        val states = subject.onAction(PurchaseButtonAction.Init).toList()

        assertEquals(listOf(PurchaseButtonState.Idle), states)
        verify(exactly = 0) { legacyProcessor.onAction(any()) }
    }

    @Test
    fun `when FF is ON a successful purchase emits Loading then Success and broadcasts the purchase`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(SessionState.Reconciling.Terminal.Success)
        coEvery { purchaseProduct.invoke(any()) } returns
            Result.success(SessionState.Purchasing.Terminal.ReadyToReconcile)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify {
            accountEventBroadcaster.emit(
                match { it is AccountEvent.PurchaseCompleted && it.productId == product.productId }
            )
        }
    }

    @Test
    fun `when FF is ON a reconciliation failure emits Loading then Error and does not broadcast`() = runTest {
        val failure = SessionState.Reconciling.Terminal.Failure(PaymentException.StoreError(RuntimeException("nope")))
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(failure)
        coEvery { purchaseProduct.invoke(any()) } returns
            Result.success(SessionState.Purchasing.Terminal.ReadyToReconcile)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Error(ERROR_MESSAGE)), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `when FF is ON a store-phase failure emits Loading then Error`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns MutableSharedFlow()
        coEvery { purchaseProduct.invoke(any()) } returns Result.failure(RuntimeException("store boom"))

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Error(ERROR_MESSAGE)), states)
    }

    @Test
    fun `when FF is ON a user cancellation resets the button to Idle without broadcasting`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            SessionState.Idle
        )
        coEvery { purchaseProduct.invoke(any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `when FF is ON a store failure on the session stream resets to Idle without broadcasting`() = runTest {
        val failure = SessionState.Purchasing.Terminal.Failure(
            PaymentException.StoreError(RuntimeException("declined"))
        )
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            failure
        )
        coEvery { purchaseProduct.invoke(any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    private companion object {
        const val ERROR_MESSAGE = "Something went wrong"
    }
}
