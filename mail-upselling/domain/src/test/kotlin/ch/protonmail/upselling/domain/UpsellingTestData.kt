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

package ch.protonmail.upselling.domain

import ch.protonmail.android.mailcommon.domain.sample.UserSample
import io.mockk.mockk
import me.proton.core.network.domain.session.SessionId
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.payment.domain.entity.PurchaseState

internal object UpsellingTestData {
    val userId = UserSample.Primary.userId

    val BasePurchase = Purchase(
        sessionId = SessionId(userId.id),
        planName = "Plan",
        planCycle = 1,
        purchaseState = PurchaseState.Acknowledged,
        purchaseFailure = null,
        paymentProvider = mockk(),
        paymentOrderId = null,
        paymentToken = null,
        paymentCurrency = mockk(),
        paymentAmount = 1L
    )

    val FakeFailedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Failed)
    val FakePurchasedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Purchased)
    val FakeSubscribedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Subscribed)
    val FakeAcknowledgedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Acknowledged)
    val FakePendingPurchase = BasePurchase.copy(purchaseState = PurchaseState.Pending)
    val FakeCancelledPurchase = BasePurchase.copy(purchaseState = PurchaseState.Cancelled)
    val FakeDeletedPurchase = BasePurchase.copy(purchaseState = PurchaseState.Deleted)
}
