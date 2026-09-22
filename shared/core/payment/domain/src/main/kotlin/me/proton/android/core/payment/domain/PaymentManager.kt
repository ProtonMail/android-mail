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

package me.proton.android.core.payment.domain

import kotlinx.coroutines.flow.Flow
import me.proton.android.core.payment.domain.model.PlayStoreProductOfferList
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.Purchase

/**
 * Get [ProductOfferDetail] from App Store (e.g. Google Play Store).
 */
interface PaymentManager {

    /**
     * Get corresponding [PlayStoreProductOfferList] from Store.
     *
     * @throws PaymentException
     */
    suspend fun getStoreProducts(ids: List<String>): List<PlayStoreProductOfferList>

    /**
     * Get existing [Purchase] from Store.
     *
     * @throws PaymentException
     */
    suspend fun getStorePurchases(): List<Purchase>

    /**
     * Observe [Purchase] from Store.
     *
     * @throws PaymentException
     */
    fun observeStorePurchases(): Flow<List<Purchase>>
}
