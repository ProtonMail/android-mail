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

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.Completed
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.CompletedWithUpgrade
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState.UserUpgradeCheckState.Pending
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.core.accountmanager.domain.SessionManager
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
    private val observePrimaryUser: ObservePrimaryUser,
    private val sessionManager: SessionManager,
    private val userUpgradeState: UserUpgradeState,
    private val observeCurrentPurchasesState: ObserveCurrentPurchasesState
) {

    suspend fun start() {
        observePrimaryUser()
            .filterNotNull()
            .map { it to it.hasSubscriptionForMail() }
            .distinctUntilChangedBy { it.second }
            .collectLatest { (user, hasSubscription) ->
                if (hasSubscription) {
                    userUpgradeState.updateState(CompletedWithUpgrade(emptyList()))
                } else {
                    val sessionId = sessionManager.getSessionId(user.userId)
                    observeCurrentPurchasesState(sessionId).collectLatest {
                        when (it) {
                            CurrentPurchasesState.Pending -> {
                                userUpgradeState.updateState(Pending)
                            }
                            CurrentPurchasesState.Deleted -> {
                                userUpgradeState.updateState(Pending)
                                delay(TIMEOUT)
                                userUpgradeState.updateState(Completed)
                            }
                            is CurrentPurchasesState.AcknowledgedOrSubscribed -> {
                                userUpgradeState.updateState(CompletedWithUpgrade(upgradedPlanNames = it.planNames))
                                delay(TIMEOUT)
                                userUpgradeState.updateState(Completed)
                            }
                            CurrentPurchasesState.NotApplicable -> {
                                userUpgradeState.updateState(Completed)
                            }
                        }
                    }
                }
            }
    }

    private companion object {

        val TIMEOUT = 30.seconds
    }
}
