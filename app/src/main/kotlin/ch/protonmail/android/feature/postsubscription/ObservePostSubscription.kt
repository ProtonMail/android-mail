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

import java.lang.ref.WeakReference
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.PostSubscriptionActivity
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.proton.core.user.domain.extension.hasSubscriptionForMail
import javax.inject.Inject

class ObservePostSubscription @Inject constructor(
    private val observePostSubscriptionFlowEnabled: ObservePostSubscriptionFlowEnabled,
    private val observePrimaryUser: ObservePrimaryUser,
    private val userUpgradeState: UserUpgradeState
) {

    suspend fun start(activity: AppCompatActivity) {
        val activityReference = WeakReference(activity)
        var startedPendingPurchase = false
        observePrimaryUser()
            .filterNotNull()
            .map { it to it.hasSubscriptionForMail() }
            .distinctUntilChangedBy { it.second }
            .collectLatest { (user, hasSubscription) ->
                if (hasSubscription) {
                    if (startedPendingPurchase) {
                        startedPendingPurchase = false
                        activityReference.showPostSubscription()
                    }
                    return@collectLatest
                }
                observePostSubscriptionFlowEnabled(user.userId)
                    .filter { it?.value == true }
                    .distinctUntilChanged()
                    .collectLatest innerCollector@{
                        userUpgradeState.userUpgradeCheckState.awaitFlowStarted() ?: return@innerCollector
                        startedPendingPurchase = true
                        val upgradeState = userUpgradeState.userUpgradeCheckState.awaitFlowComplete()
                        startedPendingPurchase = false
                        if (upgradeState == null) return@innerCollector
                        if (upgradeState.upgradedPlanNames.contains(MAIL_PLUS_PLAN_NAME)) {
                            activityReference.showPostSubscription()
                        }
                    }
            }
    }

    private fun WeakReference<AppCompatActivity>.showPostSubscription() {
        get()?.let { activity ->
            activity.startActivity(Intent(activity, PostSubscriptionActivity::class.java))
        }
    }

    private suspend fun Flow<UserUpgradeCheckState>.awaitFlowStarted() = filter {
        it == UserUpgradeCheckState.Pending
    }.firstOrNull()

    private suspend fun Flow<UserUpgradeCheckState>.awaitFlowComplete() = filter {
        it is UserUpgradeCheckState.CompletedWithUpgrade
    }.firstOrNull() as? UserUpgradeCheckState.CompletedWithUpgrade
}

private const val MAIL_PLUS_PLAN_NAME = "mail2022"
