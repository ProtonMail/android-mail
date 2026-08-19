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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.core.payment.domain.LogTag
import me.proton.android.core.payment.domain.PaymentException
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.getBaseOffer
import me.proton.android.core.payment.domain.PaymentErrorCode
import me.proton.android.core.payment.domain.usecase.GetAvailableUpgrades
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.model.Product
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAvailableUpgrades: GetAvailableUpgrades
) : BaseViewModel<ProductListAction, ProductListState>(
    initialAction = ProductListAction.Load(),
    initialState = ProductListState.Loading
) {

    private val res = context.resources

    override fun onAction(action: ProductListAction): Flow<ProductListState> = flow {
        when (action) {
            is ProductListAction.Load -> emitAll(onLoad())
        }
    }

    private fun onLoad() = flow {
        emit(ProductListState.Loading)

        val upgrades = getAvailableUpgrades()

        val details = upgrades.filter { it.offers.firstOrNull { offer -> offer.isBaseOffer } != null }
            .mapNotNull { it.getBaseOffer() }

        val list = details.map {
            Product(
                planName = it.metadata.planName,
                productId = it.metadata.productId,
                accountId = requireNotNull(it.offer.current.customerId),
                cycle = it.offer.current.cycle,
                offerToken = it.offer.token,
                header = it.header,
                entitlements = it.metadata.entitlements,
                renewalText = it.getRenewalText(),
                amount = it.offer.current.amount,
                currency = it.offer.current.currency
            )
        }
        emit(ProductListState.Data(list))
    }.catch { exception ->
        val exceptionMessage = exception.message ?: "Error occurred whilst fetching available plans."
        CoreLogger.e(LogTag.GET_PLANS, exceptionMessage)

        when ((exception as? PaymentException)?.errorCode) {
            PaymentErrorCode.BILLING_UNAVAILABLE.value -> {
                emit(ProductListState.Data(emptyList()))
            }

            else -> {
                emit(ProductListState.Error(exceptionMessage))
            }
        }
    }

    override suspend fun FlowCollector<ProductListState>.onError(throwable: Throwable) {
        emit(ProductListState.Error(throwable.message ?: "Unknown error"))
    }

    @Suppress("MagicNumber")
    private fun ProductOfferDetail.getRenewalText(): String? = when {
        offer.isBaseOffer -> null
        else -> when (offer.current.cycle) {
            1 -> res.getString(R.string.payment_welcome_offer_renew_monthly, offer.renew.formatted)
            12 -> res.getString(R.string.payment_welcome_offer_renew_annually, offer.renew.formatted)
            24 -> res.getString(R.string.payment_welcome_offer_renew_biennially, offer.renew.formatted)
            else -> res.getQuantityString(
                R.plurals.payment_welcome_offer_renew_other,
                offer.renew.cycle,
                offer.renew.formatted
            )
        }
    }
}
