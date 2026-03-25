/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.presentation.component

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.transformLatest
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.android.core.payment.domain.LogTag
import me.proton.android.core.payment.domain.PaymentManager
import me.proton.android.core.payment.domain.SubscriptionManager
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.Purchase
import me.proton.android.core.payment.domain.model.PurchaseStatus
import me.proton.android.core.payment.presentation.PurchaseOrchestrator
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.model.Product
import me.proton.core.presentation.app.ActivityProvider
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class PurchaseButtonProcessorNative @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paymentManager: PaymentManager,
    private val subscriptionManager: SubscriptionManager,
    private val purchaseOrchestrator: PurchaseOrchestrator,
    private val activityProvider: ActivityProvider,
    private val accountEventBroadcaster: AccountEventBroadcaster
) : PurchaseButtonProcessor {

    private val res = context.resources
    private val productNotFound = res.getString(R.string.payment_purchase_button_product_not_found)

    override fun onAction(action: PurchaseButtonAction): Flow<PurchaseButtonState> = flow {
        when (action) {
            is PurchaseButtonAction.Init -> emit(PurchaseButtonState.Idle)
            is PurchaseButtonAction.Load -> emitAll(onLoad(action.product))
            is PurchaseButtonAction.Purchase -> emitAll(onPurchase(action.product))
        }
    }

    private fun onLoad(product: Product) = flow {
        val storeProducts = paymentManager.getStoreProducts(listOf(product.productId))
        val storeProduct = storeProducts.firstOrNull()

        when {
            storeProduct == null -> emit(PurchaseButtonState.Error(productNotFound, enabled = false))
            else -> {
                // Reconvene the offer by using the provided Offer token
                val requestedOffer = storeProduct.offers.find { storeOffer ->
                    storeOffer.token == product.offerToken
                } ?: return@flow emit(PurchaseButtonState.Error(productNotFound, enabled = false))

                val productMetadata = ProductMetadata(
                    productId = storeProduct.metadata.productId,
                    customerId = product.accountId,
                    planName = product.planName,
                    entitlements = product.entitlements
                )

                val productOfferDetail = ProductOfferDetail(
                    metadata = productMetadata,
                    header = product.header,
                    offer = requestedOffer
                )

                emitAll(observePurchases(product, productOfferDetail))
            }
        }
    }.catch {
        emit(PurchaseButtonState.Error(it.message ?: "Error on onLoad"))
    }

    private fun observePurchases(product: Product, detail: ProductOfferDetail) =
        paymentManager.observeStorePurchases().transformLatest { list ->
            val purchase = list.firstOrNull { it.productId == product.productId }
            when {
                purchase != null -> when (purchase.status) {
                    PurchaseStatus.Unspecified,
                    PurchaseStatus.Purchased,
                    PurchaseStatus.Pending -> emitAll(onPending(product, detail, purchase))

                    PurchaseStatus.Acknowledged -> emitAll(onSuccess(product, detail, purchase))
                }

                else -> emit(PurchaseButtonState.Idle)
            }
        }.timeout(10.seconds).retry()

    private fun onPurchase(product: Product) = flow {
        val activity = requireNotNull(activityProvider.lastResumed)
        purchaseOrchestrator.startPurchaseWorkflow(activity, product.productId, product.offerToken, product.accountId)
        emitAll(onLoad(product))
    }.catch {
        emit(PurchaseButtonState.Error(it.message ?: "Error on startPurchaseWorkflow"))
    }

    private fun onPending(
        product: Product,
        detail: ProductOfferDetail,
        purchase: Purchase
    ) = flow<PurchaseButtonState> {
        CoreLogger.d(LogTag.STORE, "onPending: $detail")
        emit(PurchaseButtonState.Pending(detail))
        // Workaround: Override detail.planName by product.planName.
        subscriptionManager.subscribe(
            detail.copy(metadata = detail.metadata.copy(planName = product.planName)),
            purchase
        )
    }.catch { exception ->
        val exceptionMessage = exception.message ?: "Error occurred whilst subscribing."
        CoreLogger.e(LogTag.SUBSCRIBE, exceptionMessage)
        emit(PurchaseButtonState.Error(exceptionMessage))
    }

    private fun onSuccess(
        product: Product,
        detail: ProductOfferDetail,
        purchase: Purchase
    ) = flow {
        CoreLogger.d(LogTag.STORE, "onPurchased: $product")
        val price = detail.offer.current
        accountEventBroadcaster.emit(
            AccountEvent.PurchaseCompleted(
                productId = product.productId,
                planName = product.planName,
                cycle = price.cycle,
                amount = price.amount,
                currency = price.currency,
                orderId = purchase.orderId
            )
        )
        emit(PurchaseButtonState.Success(product))
    }
}
