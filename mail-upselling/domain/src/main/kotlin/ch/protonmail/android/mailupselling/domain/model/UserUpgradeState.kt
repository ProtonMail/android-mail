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

package ch.protonmail.android.mailupselling.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entity that exposes the subscription upgrade state for the currently active (primary) account.
 */
@Singleton
class UserUpgradeState @Inject constructor() {

    private val _userUpgradeCheckState = MutableStateFlow<UserUpgradeCheckState>(UserUpgradeCheckState.Initial)

    /**
     * Allows subscribers to observe the current `UserUpgradeCheckState`.
     *
     * Note that currently it only supports Free -> Paid plan transitions.
     */
    val userUpgradeCheckState: Flow<UserUpgradeCheckState> = _userUpgradeCheckState.asStateFlow()

    /**
     * Returns whether the current user is upgrading their subscription.
     *
     * Note that currently it only supports Free -> Paid plan transitions.
     */
    val isUserPendingUpgrade: Boolean
        get() = _userUpgradeCheckState.value == UserUpgradeCheckState.Pending

    internal fun updateState(state: UserUpgradeCheckState) {
        _userUpgradeCheckState.value = state
    }

    /**
     * Indicates whether the user subscription is being upgraded or not.
     */
    sealed interface UserUpgradeCheckState {

        data object Initial : UserUpgradeCheckState
        data object Completed : UserUpgradeCheckState
        data class CompletedWithUpgrade(val upgradedPlanNames: List<String>) : UserUpgradeCheckState
        data object Pending : UserUpgradeCheckState
    }
}
