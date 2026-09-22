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

package me.proton.android.core.payment.google.data

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import me.proton.android.core.payment.domain.PaymentManager
import me.proton.android.core.payment.domain.model.PlayStoreProductMetadata
import me.proton.android.core.payment.domain.model.PlayStoreProductOfferList
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.domain.model.Purchase
import me.proton.android.core.payment.google.data.extension.getOrThrow
import me.proton.android.core.payment.google.data.extension.getProductDetails
import me.proton.android.core.payment.google.data.extension.getProductPurchasesAsync
import me.proton.android.core.payment.google.data.extension.withConnection
import me.proton.android.core.payment.google.data.model.toProductPriceOffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentManagerImpl @Inject constructor(
    @ApplicationContext val context: Context,
    private val purchaseStoreListener: PurchaseStoreListener
) : PaymentManager {

    // Return a new instance (multiple simultaneous instances are supported).
    private fun newClient() = BillingClient
        .newBuilder(context)
        .setListener(purchaseStoreListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    override suspend fun getStoreProducts(ids: List<String>): List<PlayStoreProductOfferList> =
        newClient().withConnection {
            getProductDetails(ids).getOrThrow()?.flatMap { detail ->
                buildList {
                    val offers = detail.subscriptionOfferDetails.orEmpty()

                    val productOffers = offers.filterNotNull().mapNotNull { offer ->

                        val pricingPhases = offer.pricingPhases.pricingPhaseList
                        val current = pricingPhases.getOrNull(0) ?: return@mapNotNull null
                        val renew = pricingPhases.getOrNull(1)

                        val isBaseOffer = pricingPhases.size == 1 &&
                            current.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING

                        ProductOffer(
                            isBaseOffer = isBaseOffer,
                            tags = ProductOfferTags(offer.offerTags.toSet()),
                            token = ProductOfferToken(offer.offerToken),
                            current = current.toProductPriceOffer(detail.productId),
                            renew = renew?.toProductPriceOffer(detail.productId)
                                ?: current.toProductPriceOffer(detail.productId)
                        )
                    }

                    val metadata = PlayStoreProductMetadata(
                        productId = detail.productId,
                        planName = detail.name
                    )

                    val offerList = PlayStoreProductOfferList(
                        metadata = metadata,
                        offers = productOffers
                    )

                    add(offerList)
                }
            }.orEmpty()
        }

    override suspend fun getStorePurchases(): List<Purchase> = newClient().withConnection {
        purchaseStoreListener.purchases
            .onSubscription { getProductPurchasesAsync(purchaseStoreListener) }
            .first()
    }

    override fun observeStorePurchases(): Flow<List<Purchase>> = callbackFlow {
        val job = launch { purchaseStoreListener.purchases.collect { send(it) } }
        newClient().withConnection {
            getProductPurchasesAsync(purchaseStoreListener)
            job.join()
        }
        awaitClose { job.cancel() }
    }
}
