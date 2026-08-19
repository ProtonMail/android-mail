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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
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
import me.proton.android.payment.purchase.extension.transacting
import me.proton.android.payment.purchase.model.PendingPurchase
import me.proton.android.payment.purchase.model.SessionState
import me.proton.android.payment.purchase.usecase.ObserveSessionState
import me.proton.android.payment.purchase.usecase.PurchaseProduct

@Singleton
class SdkBackedPurchaseButtonProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val legacyProcessor: PurchaseButtonProcessorNative,
    private val purchaseProduct: Lazy<PurchaseProduct>, // defer so FF OFF doesn't touch the SDK
    private val observeSessionState: Lazy<ObserveSessionState>,
    private val getActivePurchases: Optional<GetActivePurchases>,
    private val accountEventBroadcaster: AccountEventBroadcaster,
    @IsSdkUpgradesPurchaseEnabled private val sdkPurchaseEnabled: FeatureFlag<Boolean>
) : PurchaseButtonProcessor {

    override fun onAction(action: PurchaseButtonAction): Flow<PurchaseButtonState> = flow {
        if (!sdkPurchaseEnabled.get()) {
            emitAll(legacyProcessor.onAction(action))
            return@flow
        }
        when (action) {
            is PurchaseButtonAction.Purchase -> emitAll(purchase(action.product))
            is PurchaseButtonAction.Load,
            PurchaseButtonAction.Init -> emit(PurchaseButtonState.Idle)
        }
    }

    private fun purchase(product: Product): Flow<PurchaseButtonState> = flow {
        emit(PurchaseButtonState.Loading)
        emit(runPurchase(product))
    }

    private suspend fun runPurchase(product: Product): PurchaseButtonState = coroutineScope {
        val outcome = async(start = CoroutineStart.UNDISPATCHED) {
            observeSessionState.get().invoke()
                .dropWhile { !it.transacting() }
                .mapNotNull { it.toButtonState(product) }
                .first()
        }

        val storeStep = purchaseProduct.get()
            .invoke(PendingPurchase(product.productId, product.offerToken.value))
            .getOrElse {
                outcome.cancel()
                return@coroutineScope errorState()
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

    private suspend fun SessionState.toButtonState(product: Product): PurchaseButtonState? = when (this) {
        is SessionState.Reconciling.Terminal.Success -> {
            broadcastPurchaseCompleted(product)
            PurchaseButtonState.Success(product)
        }
        is SessionState.Reconciling.Terminal.Failure -> errorState()
        is SessionState.Purchasing.Terminal.Failure,
        SessionState.Idle -> PurchaseButtonState.Idle
        else -> null
    }

    private fun errorState() = PurchaseButtonState.Error(context.getString(R.string.payment_error_title))

    private suspend fun broadcastPurchaseCompleted(product: Product) {
        val orderId = getActivePurchases.orElse(null)
            ?.invoke()
            ?.getOrNull()
            ?.firstOrNull { it.productId == product.productId }
            ?.orderId
            .orEmpty()
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
