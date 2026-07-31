/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import javax.inject.Inject

class LegacyAccountWelcomeProvider @Inject constructor() : AccountWelcomeProvider {

    @Composable
    override fun Welcome(
        onAddAccount: () -> Unit,
        signedInContent: @Composable () -> Unit
    ) {
        // Legacy flow: there is no welcome screen, delegate to the auth orchestrator's add-account workflow.
        val currentOnAddAccount by rememberUpdatedState(onAddAccount)
        LaunchedEffect(Unit) { currentOnAddAccount() }
    }
}
