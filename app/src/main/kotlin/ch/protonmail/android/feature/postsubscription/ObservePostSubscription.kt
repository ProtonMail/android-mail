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

package ch.protonmail.android.feature.postsubscription

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.PostSubscriptionActivity
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.PurchaseState
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.hasSubscriptionForMail
import javax.inject.Inject

class ObservePostSubscription @Inject constructor(
    @AppScope private val coroutineScope: CoroutineScope,
    private val isPostSubscriptionFlowEnabled: IsPostSubscriptionFlowEnabled,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val purchaseManager: PurchaseManager,
    private val sessionManager: SessionManager,
    private val userManager: UserManager
) {

    fun start(activity: AppCompatActivity) = coroutineScope.launch {
        observePrimaryUserId().filterNotNull().distinctUntilChanged().collectLatest { userId ->
            // Cancel observation if the user already has a subscription.
            if (userManager.getUser(userId).hasSubscriptionForMail()) return@collectLatest

            if (!isPostSubscriptionFlowEnabled(userId)) return@collectLatest

            val sessionId = sessionManager.getSessionId(userId)

            purchaseManager.observePurchases().distinctUntilChanged().collectLatest purchases@{ purchases ->
                // Make sure the purchase was completed from the app
                val currentSessionPurchases = purchases.filter { it.sessionId == sessionId }
                val isMailPlusCompletedPurchaseFound = currentSessionPurchases.any {
                    it.planName == MAIL_PLUS_PLAN_NAME && it.purchaseState == PurchaseState.Acknowledged
                }

                if (isMailPlusCompletedPurchaseFound) {
                    val intent = Intent(activity, PostSubscriptionActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val MAIL_PLUS_PLAN_NAME = "mail2022"
    }
}
