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

import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState
import javax.inject.Inject

class UserHasPendingPurchases @Inject constructor(
    private val sessionManager: SessionManager
) {

    suspend operator fun invoke(purchases: List<Purchase>, userId: UserId): Boolean {
        if (purchases.isEmpty()) return false

        val sessionId = sessionManager.getSessionId(userId)
        return purchases.any {
            it.sessionId == sessionId && it.purchaseState in listOf(PurchaseState.Purchased, PurchaseState.Subscribed)
        }
    }
}
