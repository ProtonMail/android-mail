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
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesPurchaseEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.component.PurchaseButtonAction
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessor
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessorNative
import me.proton.android.core.payment.presentation.component.PurchaseButtonState
import me.proton.android.core.payment.presentation.model.Product
import me.proton.android.payment.billing.usecase.GetActivePurchases
import me.proton.android.payment.exception.PaymentException
import me.proton.android.payment.exception.PaymentExceptionCode
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct
import me.proton.android.payment.purchase.usecase.ReconcileOutstandingPurchases

@Singleton
class SdkBackedPurchaseButtonProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val legacyProcessor: PurchaseButtonProcessorNative,
    private val purchaseProduct: Lazy<PurchaseProduct>, // defer so FF OFF doesn't touch the SDK
    private val observeSessionState: Lazy<ObserveSessionState>,
    private val reconcileOutstandingPurchases: Lazy<ReconcileOutstandingPurchases>,
    private val getActivePurchases: Optional<GetActivePurchases>,
    private val accountEventBroadcaster: AccountEventBroadcaster,
    @IsSdkUpgradesPurchaseEnabled private val sdkPurchaseEnabled: FeatureFlag<Boolean>
) : PurchaseButtonProcessor {

    private val broadcastOrderIds = ConcurrentHashMap.newKeySet<String>()

    override fun onAction(action: PurchaseButtonAction): Flow<PurchaseButtonState> = flow {
        if (!sdkPurchaseEnabled.get()) {
            emitAll(legacyProcessor.onAction(action))
            return@flow
        }
        when (action) {
            is PurchaseButtonAction.Purchase -> emitAll(purchase(action.product))
            is PurchaseButtonAction.Load -> emitAll(load(action.product))
            PurchaseButtonAction.Init -> emit(PurchaseButtonState.Idle)
        }
    }

    // a purchase whose receipt never reached the app is only picked up here
    private fun load(product: Product): Flow<PurchaseButtonState> = flow {
        if (activePurchases(product).all { it.acknowledged }) {
            emit(PurchaseButtonState.Idle)
            return@flow
        }
        emit(PurchaseButtonState.Loading)
        emit(finishOutstandingPurchase(product))
    }

    private suspend fun finishOutstandingPurchase(product: Product): PurchaseButtonState = coroutineScope {
        val outcome = sessionOutcome(product)
        val reconcile = reconcileOutstandingPurchases.get().invoke()
        when {
            // a second button for the same plan, or the SDK itself, may already be reconciling it
            reconcile.isSuccess || reconcile.exceptionOrNull().isSessionInProgress() -> outcome.await()
            else -> {
                outcome.cancel()
                PurchaseButtonState.Idle
            }
        }
    }

    private fun purchase(product: Product): Flow<PurchaseButtonState> = flow {
        emit(PurchaseButtonState.Loading)
        emit(runPurchase(product))
    }

    private suspend fun runPurchase(product: Product): PurchaseButtonState = coroutineScope {
        val outcome = sessionOutcome(product)

        val storeStep = purchaseProduct.get()
            .invoke(productId = product.productId, offerToken = product.offerToken.value)
            .getOrElse {
                // this action re-runs when the screen comes back, on a purchase already under way
                if (it.isSessionInProgress()) return@coroutineScope outcome.await()
                outcome.cancel()
                val completed = completedPurchase(product)
                return@coroutineScope when {
                    completed != null -> successState(product, completed.orderId)
                    else -> errorState()
                }
            }

        when (storeStep) {
            is SessionState.Purchasing.Terminal.Failure -> {
                outcome.cancel()
                PurchaseButtonState.Idle
            }
            SessionState.Purchasing.Terminal.AwaitingReceipt,
            SessionState.Purchasing.Terminal.ReadyToReconcile -> outcome.await()
        }
    }

    private fun CoroutineScope.sessionOutcome(product: Product): Deferred<PurchaseButtonState> =
        // subscribe before the SDK call, otherwise a terminal state is immediately replaced by Idle and never seen
        async(start = CoroutineStart.UNDISPATCHED) {
            observeSessionState.get().invoke()
                .dropWhile { it == SessionState.Idle } // state replays last value before a purchase starts (Idle)
                .mapNotNull { it.toButtonState(product) }
                .first()
        }

    private suspend fun SessionState.toButtonState(product: Product): PurchaseButtonState? = when (this) {
        // a button only claims a purchase that is for its own plan
        is SessionState.Reconciling.Terminal.Success -> when (purchase.planId) {
            product.planName -> successState(product, purchase.orderId)
            else -> null
        }
        is SessionState.Reconciling.Terminal.Failure -> errorState()
        is SessionState.Purchasing.Terminal.Failure,
        SessionState.Idle -> PurchaseButtonState.Idle
        else -> null
    }

    private fun Throwable?.isSessionInProgress() =
        (this as? PaymentException)?.code == PaymentExceptionCode.CORE_SESSION_IN_PROGRESS

    private fun errorState() = PurchaseButtonState.Error(context.getString(R.string.payment_error_title))

    private suspend fun completedPurchase(product: Product) = activePurchases(product)
        .firstOrNull { it.acknowledged }

    private suspend fun activePurchases(product: Product) = getActivePurchases.orElse(null)
        ?.invoke()
        ?.getOrNull()
        ?.filter { it.productId == product.productId }
        .orEmpty()

    private suspend fun successState(product: Product, orderId: String?): PurchaseButtonState {
        broadcastPurchaseCompleted(product, orderId.orEmpty())
        return PurchaseButtonState.Success(product)
    }

    private suspend fun broadcastPurchaseCompleted(product: Product, orderId: String) {
        if (orderId.isNotBlank() && !broadcastOrderIds.add(orderId)) return
        accountEventBroadcaster.emit(
            AccountEvent.PurchaseCompleted(
                productId = product.productId,
                planName = product.planName,
                cycle = product.cycle,
                amount = product.amount,
                currency = product.currency,
                orderId = orderId
            )
        )
    }
}
