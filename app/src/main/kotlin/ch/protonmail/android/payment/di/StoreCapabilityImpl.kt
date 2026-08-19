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

import me.proton.android.payment.billing.extension.invokeOrBillingUnavailable
import me.proton.android.payment.billing.model.StoreProduct
import me.proton.android.payment.billing.usecase.AcknowledgePurchase
import me.proton.android.payment.billing.usecase.GetStoreProducts
import me.proton.android.payment.billing.usecase.PurchaseStoreProduct
import me.proton.android.payment.capability.StoreCapability
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreCapabilityImpl @Inject constructor(
    private val getStoreProducts: Optional<GetStoreProducts>,
    private val acknowledgePurchase: Optional<AcknowledgePurchase>,
    private val purchaseStoreProduct: Optional<PurchaseStoreProduct>
) : StoreCapability {

    override suspend fun getProducts(ids: List<String>): Result<List<StoreProduct>> =
        getStoreProducts.invokeOrBillingUnavailable { it(ids) }

    override suspend fun purchase(
        productId: String,
        offerToken: String,
        userId: String?
    ): Result<Unit> = purchaseStoreProduct.invokeOrBillingUnavailable { it(productId, offerToken, userId) }

    override suspend fun acknowledge(orderId: String): Result<Unit> =
        acknowledgePurchase.invokeOrBillingUnavailable { it(orderId) }
}
