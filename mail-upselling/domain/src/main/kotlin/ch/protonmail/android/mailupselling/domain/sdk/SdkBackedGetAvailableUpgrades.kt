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

package ch.protonmail.android.mailupselling.domain.sdk

import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesReadEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.usecase.GetAvailableUpgrades
import me.proton.android.payment.product.usecase.GetProducts

@Singleton
class SdkBackedGetAvailableUpgrades @Inject constructor(
    // Note: only used to fetch customerId for the current purchase flow.
    // Remove once SDK purchases fully enabled, as customerId not needed there.
    private val legacyGetAvailableUpgrades: GetAvailableUpgrades,
    private val getProducts: Lazy<GetProducts>, // defer this so FF OFF doesn't touch the SDK
    private val mapper: SdkProductOfferMapper,
    @IsSdkUpgradesReadEnabled private val sdkUpgradesReadEnabled: FeatureFlag<Boolean>
) {

    suspend operator fun invoke(): List<ProductOfferList> {
        if (!sdkUpgradesReadEnabled.get()) {
            return legacyGetAvailableUpgrades()
        }

        return coroutineScope {
            val legacyDeferred = async { legacyGetAvailableUpgrades() }
            val sdkDeferred = async { getProducts.get().invoke().getOrThrow() }
            val legacy = legacyDeferred.await()
            val sdkProducts = sdkDeferred.await()
            mapper.mapToProductOfferList(
                sdkProducts = sdkProducts,
                legacyByProductId = legacy.associateBy { it.metadata.productId }
            )
        }
    }
}
