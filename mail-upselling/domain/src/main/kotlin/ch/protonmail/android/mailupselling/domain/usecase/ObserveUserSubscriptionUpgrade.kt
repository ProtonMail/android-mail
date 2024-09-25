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

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.PurchaseState
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.hasSubscriptionForMail
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Single entity to monitor the upgrade state for the current primary user.
 *
 * This singleton is instantiated as part of an initializer and should not be provided
 * in any other way (hence the internal modifier).
 */
@Singleton
internal class ObserveUserSubscriptionUpgrade @Inject constructor(
    private val userManager: UserManager,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val sessionManager: SessionManager,
    private val purchaseManager: PurchaseManager,
    private val userUpgradeState: UserUpgradeState,
    @AppScope private val scope: CoroutineScope
) {

    private var observationJob: Job? = null

    suspend fun start() {
        observePrimaryUserId().filterNotNull().distinctUntilChanged().collectLatest { user ->
            // Cancel observation if the user already has a subscription.
            if (userManager.getUser(user).hasSubscriptionForMail()) return@collectLatest

            val sessionId = sessionManager.getSessionId(user)

            purchaseManager.observePurchases().collectLatest purchases@{ purchases ->
                val userIdPurchases = purchases.filter { it.sessionId == sessionId }

                if (userIdPurchases.isEmpty()) {
                    userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed)
                    return@purchases
                }

                val hasPendingPurchases = userIdPurchases.any {
                    it.purchaseState in listOf(
                        PurchaseState.Pending,
                        PurchaseState.Purchased,
                        PurchaseState.Subscribed
                    )
                }

                // Either Acknowledged or Deleted with no failure.
                val hasAcknowledgedOrDeletedPurchases = userIdPurchases.any {
                    it.purchaseState == PurchaseState.Acknowledged ||
                        it.purchaseState == PurchaseState.Deleted && it.purchaseFailure == null
                }

                when {
                    hasPendingPurchases -> {
                        userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending)
                    }

                    hasAcknowledgedOrDeletedPurchases -> {
                        stopObserving()
                        startObserving(user)
                    }

                    else -> {
                        userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed)
                        return@purchases
                    }
                }
            }
        }
    }

    private fun startObserving(userId: UserId) {
        userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Pending)

        observationJob = scope.launch {
            supervisorScope {
                val job = launch innerScope@{
                    userManager.observeUser(userId).filterNotNull().collect {

                        if (it.hasSubscriptionForMail()) {
                            userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed)
                            this@innerScope.cancel()
                        }
                    }
                }

                try {
                    withTimeout(TIMEOUT) { job.join() }
                } catch (_: TimeoutCancellationException) {
                    userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Completed)
                }
            }
        }
    }

    private fun stopObserving() {
        observationJob?.cancel()
        userUpgradeState.updateState(UserUpgradeState.UserUpgradeCheckState.Initial)
    }

    private companion object {

        val TIMEOUT = 30.seconds
    }
}
