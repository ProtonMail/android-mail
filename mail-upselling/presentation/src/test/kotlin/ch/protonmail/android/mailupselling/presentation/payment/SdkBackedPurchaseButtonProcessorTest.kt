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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.android.core.payment.presentation.component.PurchaseButtonAction
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessorNative
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.component.PurchaseButtonState
import me.proton.android.core.payment.presentation.model.Product
import me.proton.android.payment.billing.model.StorePurchase
import me.proton.android.payment.billing.model.StorePurchaseState
import me.proton.android.payment.billing.usecase.GetActivePurchases
import me.proton.android.payment.exception.PaymentException
import me.proton.android.payment.exception.PaymentExceptionCode
import me.proton.android.payment.purchase.model.ReconciledPurchase
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct
import me.proton.android.payment.purchase.usecase.ReconcileOutstandingPurchases
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SdkBackedPurchaseButtonProcessorTest {

    private val legacyProcessor = mockk<PurchaseButtonProcessorNative>()
    private val purchaseProduct = mockk<PurchaseProduct>()
    private val observeSessionState = mockk<ObserveSessionState>()
    private val reconcileOutstandingPurchases = mockk<ReconcileOutstandingPurchases> {
        coEvery { this@mockk.invoke() } returns Result.success(Unit)
    }
    private val getActivePurchases = mockk<GetActivePurchases>()
    private val accountEventBroadcaster = mockk<AccountEventBroadcaster>(relaxed = true)
    private val flag = mockk<FeatureFlag<Boolean>>()
    private val context = mockk<Context> {
        every { getString(R.string.payment_error_title) } returns ERROR_MESSAGE
    }

    private val subject = subject(Optional.empty())

    private val product = Product.test

    private fun withActivePurchases() = subject(Optional.of(getActivePurchases))

    private fun subject(activePurchases: Optional<GetActivePurchases>) = SdkBackedPurchaseButtonProcessor(
        context = context,
        legacyProcessor = legacyProcessor,
        purchaseProduct = Lazy { purchaseProduct },
        observeSessionState = Lazy { observeSessionState },
        reconcileOutstandingPurchases = Lazy { reconcileOutstandingPurchases },
        getActivePurchases = activePurchases,
        accountEventBroadcaster = accountEventBroadcaster,
        sdkPurchaseEnabled = flag
    )

    @Test
    fun `Payment SDK off - delegates to the legacy processor`() = runTest {
        coEvery { flag.get() } returns false
        every { legacyProcessor.onAction(PurchaseButtonAction.Init) } returns flowOf(PurchaseButtonState.Idle)

        val states = subject.onAction(PurchaseButtonAction.Init).toList()

        assertEquals(listOf(PurchaseButtonState.Idle), states)
        verify { legacyProcessor.onAction(PurchaseButtonAction.Init) }
    }

    @Test
    fun `Payment SDK - a non-purchase action emits Idle without touching the legacy processor`() = runTest {
        coEvery { flag.get() } returns true

        val states = subject.onAction(PurchaseButtonAction.Init).toList()

        assertEquals(listOf(PurchaseButtonState.Idle), states)
        verify(exactly = 0) { legacyProcessor.onAction(any()) }
        coVerify(exactly = 0) { reconcileOutstandingPurchases.invoke() }
    }

    @Test
    fun `Payment SDK - loading the button reports a purchase the app never received`() = runTest {
        coEvery { flag.get() } returns true
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(unacknowledgedPurchase))
        every { observeSessionState.invoke() } returns
            flowOf(SessionState.Reconciling.Terminal.Success(reconciledPurchase))

        val states = withActivePurchases().onAction(PurchaseButtonAction.Load(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify { reconcileOutstandingPurchases.invoke() }
    }

    @Test
    fun `Payment SDK - loading the button waits for a recovery another caller started`() = runTest {
        val sessionInProgress = PaymentException.CoreError(
            PaymentExceptionCode.CORE_SESSION_IN_PROGRESS,
            RuntimeException("session in progress")
        )
        coEvery { flag.get() } returns true
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(unacknowledgedPurchase))
        coEvery { reconcileOutstandingPurchases.invoke() } returns Result.failure(sessionInProgress)
        every { observeSessionState.invoke() } returns
            flowOf(SessionState.Reconciling.Terminal.Success(reconciledPurchase))

        val states = withActivePurchases().onAction(PurchaseButtonAction.Load(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
    }

    @Test
    fun `Payment SDK - loading the button gives up when the recovery fails`() = runTest {
        coEvery { flag.get() } returns true
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(unacknowledgedPurchase))
        coEvery { reconcileOutstandingPurchases.invoke() } returns Result.failure(alreadyOwned)
        every { observeSessionState.invoke() } returns MutableSharedFlow()

        val states = withActivePurchases().onAction(PurchaseButtonAction.Load(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - loading the button with nothing outstanding stays Idle and reconciles nothing`() = runTest {
        coEvery { flag.get() } returns true
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(acknowledgedPurchase))

        val states = withActivePurchases().onAction(PurchaseButtonAction.Load(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { reconcileOutstandingPurchases.invoke() }
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a successful purchase emits Loading then Success and broadcasts the purchase`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns
            flowOf(SessionState.Reconciling.Terminal.Success(reconciledPurchase))
        coEvery { purchaseProduct.invoke(any(), any()) } returns
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
    fun `Payment SDK - a reconciliation failure emits Loading then Error and does not broadcast`() = runTest {
        val failure = SessionState.Reconciling.Terminal.Failure(
            PaymentException.StoreError(PaymentExceptionCode.STORE_PURCHASE, RuntimeException("nope"))
        )
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(failure)
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.ReadyToReconcile)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Error(ERROR_MESSAGE)), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a store-phase failure emits Loading then Error`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns MutableSharedFlow()
        coEvery { purchaseProduct.invoke(any(), any()) } returns Result.failure(RuntimeException("store boom"))

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Error(ERROR_MESSAGE)), states)
    }

    @Test
    fun `Payment SDK - a refused launch on an acknowledged purchase emits Success`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns MutableSharedFlow()
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(acknowledgedPurchase))
        coEvery { purchaseProduct.invoke(any(), any()) } returns Result.failure(alreadyOwned)

        val states = withActivePurchases().onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify(exactly = 1) {
            accountEventBroadcaster.emit(
                match { it is AccountEvent.PurchaseCompleted && it.orderId == acknowledgedPurchase.orderId }
            )
        }
    }

    @Test
    fun `Payment SDK - a refused launch with nothing in the store emits Error`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns MutableSharedFlow()
        coEvery { getActivePurchases.invoke() } returns Result.success(emptyList())
        coEvery { purchaseProduct.invoke(any(), any()) } returns Result.failure(alreadyOwned)

        val states = withActivePurchases().onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Error(ERROR_MESSAGE)), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - an acknowledged purchase of another product is bought normally`() = runTest {
        val otherProduct = acknowledgedPurchase.copy(productId = "giapmail_other_12_renewing")
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns
            flowOf(SessionState.Reconciling.Terminal.Success(reconciledPurchase))
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(otherProduct))
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.ReadyToReconcile)

        val states = withActivePurchases().onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify { purchaseProduct.invoke(any(), any()) }
    }

    @Test
    fun `Payment SDK - the same order is broadcast only once`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns MutableSharedFlow()
        coEvery { getActivePurchases.invoke() } returns Result.success(listOf(acknowledgedPurchase))
        coEvery { purchaseProduct.invoke(any(), any()) } returns Result.failure(alreadyOwned)
        val processor = withActivePurchases()

        processor.onAction(PurchaseButtonAction.Purchase(product)).toList()
        processor.onAction(PurchaseButtonAction.Purchase(product)).toList()

        coVerify(exactly = 1) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a session succeeding for another plan is ignored until this one ends`() = runTest {
        val otherPlan = reconciledPurchase.copy(planId = "other2022")
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns
            flowOf(SessionState.Reconciling.Terminal.Success(otherPlan), SessionState.Idle)
        coEvery { getActivePurchases.invoke() } returns Result.success(emptyList())
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt)

        val states = withActivePurchases().onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a rejected relaunch waits for the in-flight purchase instead of failing`() = runTest {
        val sessionInProgress = PaymentException.CoreError(
            PaymentExceptionCode.CORE_SESSION_IN_PROGRESS,
            RuntimeException("session in progress")
        )
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            SessionState.Reconciling.Terminal.Success(reconciledPurchase)
        )
        coEvery { purchaseProduct.invoke(any(), any()) } returns Result.failure(sessionInProgress)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify {
            accountEventBroadcaster.emit(
                match { it is AccountEvent.PurchaseCompleted && it.productId == product.productId }
            )
        }
    }

    @Test
    fun `Payment SDK - a purchase interrupted by the screen going away still ends in Success`() = runTest {
        val sessionStates = MutableSharedFlow<SessionState>(replay = 1)
        val sessionInProgress = PaymentException.CoreError(
            PaymentExceptionCode.CORE_SESSION_IN_PROGRESS,
            RuntimeException("session in progress")
        )
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns sessionStates
        coEvery { getActivePurchases.invoke() } returns Result.success(emptyList())
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt) andThen
            Result.failure(sessionInProgress)
        val processor = withActivePurchases()

        val offScreen = launch { processor.onAction(PurchaseButtonAction.Purchase(product)).toList() }
        runCurrent()
        sessionStates.emit(SessionState.Purchasing.Terminal.AwaitingReceipt)
        offScreen.cancelAndJoin()

        val states = mutableListOf<PurchaseButtonState>()
        val resumed = launch { processor.onAction(PurchaseButtonAction.Purchase(product)).toList(states) }
        runCurrent()
        sessionStates.emit(SessionState.Reconciling.Terminal.Success(reconciledPurchase))
        resumed.join()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Success(product)), states)
        coVerify(exactly = 1) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a user cancellation resets the button to Idle without broadcasting`() = runTest {
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            SessionState.Idle
        )
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    @Test
    fun `Payment SDK - a store failure on the session stream resets to Idle without broadcasting`() = runTest {
        val failure = SessionState.Purchasing.Terminal.Failure(
            PaymentException.StoreError(PaymentExceptionCode.STORE_PURCHASE, RuntimeException("declined"))
        )
        coEvery { flag.get() } returns true
        every { observeSessionState.invoke() } returns flowOf(
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            failure
        )
        coEvery { purchaseProduct.invoke(any(), any()) } returns
            Result.success(SessionState.Purchasing.Terminal.AwaitingReceipt)

        val states = subject.onAction(PurchaseButtonAction.Purchase(product)).toList()

        assertEquals(listOf(PurchaseButtonState.Loading, PurchaseButtonState.Idle), states)
        coVerify(exactly = 0) { accountEventBroadcaster.emit(any()) }
    }

    private companion object {
        const val ERROR_MESSAGE = "Something went wrong"
        val alreadyOwned = PaymentException.StoreError(
            PaymentExceptionCode.STORE_PURCHASE,
            RuntimeException("item already owned")
        )
        val acknowledgedPurchase = StorePurchase(
            productId = Product.test.productId,
            packageName = "ch.protonmail.android",
            purchaseState = StorePurchaseState.PURCHASED,
            orderId = "GPA.1234-5678-9012-34567",
            userId = "e9a1c0f4-0b3e-4a6d-9f2c-1d7b8e5a3c21",
            acknowledged = true
        )
        val unacknowledgedPurchase = acknowledgedPurchase.copy(acknowledged = false)
        val reconciledPurchase = ReconciledPurchase(
            orderId = "order-1",
            planId = Product.test.planName,
            title = "Mail Plus"
        )
    }
}
