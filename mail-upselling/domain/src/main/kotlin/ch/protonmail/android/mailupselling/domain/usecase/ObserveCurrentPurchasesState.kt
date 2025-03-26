/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState
import javax.inject.Inject

internal class ObserveCurrentPurchasesState @Inject constructor(
    private val purchaseManager: PurchaseManager
) {

    operator fun invoke(sessionId: SessionId?): Flow<CurrentPurchasesState> {
        return purchaseManager.observePurchases().map { it.reduceToEvent(sessionId) }
            .distinctUntilChanged()
    }

    private fun List<Purchase>.reduceToEvent(sessionId: SessionId?): CurrentPurchasesState {
        val userIdPurchases = filter { it.sessionId == sessionId }
        if (userIdPurchases.isEmpty()) return CurrentPurchasesState.NotApplicable
        val hasPendingPurchases = userIdPurchases.any {
            it.purchaseState in listOf(
                PurchaseState.Pending,
                PurchaseState.Purchased
            )
        }
        // Deleted with no failure.
        val hasDeletedPurchases = userIdPurchases.any {
            it.purchaseState == PurchaseState.Deleted && it.purchaseFailure == null
        }

        val acknowledgedOrSubscribedPurchases = userIdPurchases.filter {
            it.purchaseState in listOf(
                PurchaseState.Acknowledged,
                PurchaseState.Subscribed
            )
        }
        val hasAcknowledgedOrSubscribedPurchases = acknowledgedOrSubscribedPurchases.isNotEmpty()
        return when {
            hasPendingPurchases -> CurrentPurchasesState.Pending
            hasAcknowledgedOrSubscribedPurchases -> {
                val planNames = acknowledgedOrSubscribedPurchases.map { it.planName }
                CurrentPurchasesState.AcknowledgedOrSubscribed(planNames)
            }
            hasDeletedPurchases -> CurrentPurchasesState.Deleted
            else -> CurrentPurchasesState.NotApplicable
        }
    }
}

internal sealed class CurrentPurchasesState { data object Pending : CurrentPurchasesState()
    data class AcknowledgedOrSubscribed(val planNames: List<String>) : CurrentPurchasesState()
    data object Deleted : CurrentPurchasesState()
    data object NotApplicable : CurrentPurchasesState()
}
