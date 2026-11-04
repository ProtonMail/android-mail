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

package ch.protonmail.android.mailfeatureflags.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailfeatureflags.presentation.model.FeatureFlagOverridesState
import ch.protonmail.android.mailfeatureflags.presentation.viewmodel.FeatureFlagOverridesViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun FeatureFlagOverridesScreen(onBack: () -> Unit, viewModel: FeatureFlagOverridesViewModel = hiltViewModel()) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    val screenActions = FeatureFlagOverridesScreen.Actions.Empty.copy(
        onBack = onBack,
        onToggled = { featureKey -> viewModel.toggleKey(featureKey) },
        onVariantSelected = { featureKey, variant -> viewModel.selectVariant(featureKey, variant) },
        onResetAll = { viewModel.resetAll() }
    )

    when (val currentState = state) {
        is FeatureFlagOverridesState.Loading -> ProtonCenteredProgress()
        is FeatureFlagOverridesState.Loaded -> FeatureFlagOverridesScreenContent(currentState, screenActions)
    }
}

object FeatureFlagOverridesScreen {
    data class Actions(
        val onBack: () -> Unit,
        val onToggled: (key: String) -> Unit,
        val onVariantSelected: (key: String, variant: String?) -> Unit,
        val onResetAll: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBack = {},
                onToggled = {},
                onVariantSelected = { _, _ -> },
                onResetAll = {}
            )
        }
    }
}
