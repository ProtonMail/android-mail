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

package ch.protonmail.android.navigation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import me.proton.core.network.domain.NetworkStatus

data class HomeState(
    val networkStatusEffect: Effect<NetworkStatus>,
    val messageSendingStatusEffect: Effect<MessageSendingStatus>,
    val navigateToEffect: Effect<String>,
    val startedFromLauncher: Boolean
) {

    companion object {

        val Initial = HomeState(
            networkStatusEffect = Effect.empty(),
            messageSendingStatusEffect = Effect.empty(),
            navigateToEffect = Effect.empty(),
            startedFromLauncher = false
        )
    }
}
