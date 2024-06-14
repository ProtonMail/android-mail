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

package ch.protonmail.android.mailupselling.presentation.ui.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingBottomSheetViewModel

@Composable
fun UpsellingBottomSheet(modifier: Modifier = Modifier, bottomSheetActions: UpsellingBottomSheet.Actions) {
    val viewModel: UpsellingBottomSheetViewModel = hiltViewModel()
    val actions = bottomSheetActions.copy(
        onDisplayed = { viewModel.updateLastSeenTimestamp() },
        onPlanSelected = { viewModel.trackUpgradeAttempt(it) },
        onUpgradeCancelled = { viewModel.trackUpgradeCancelled(it) },
        onSuccess = { viewModel.trackPurchaseCompleted(it) }
    )

    when (val state = viewModel.state.collectAsState().value) {
        UpsellingBottomSheetContentState.Loading -> Unit
        is UpsellingBottomSheetContentState.Data -> UpsellingBottomSheetContent(modifier, state, actions)
        is UpsellingBottomSheetContentState.Error -> UpsellingBottomSheetError(state = state, actions)
    }
}

object UpsellingBottomSheet {
    data class Actions(
        val onDisplayed: suspend () -> Unit,
        val onError: (String) -> Unit,
        val onPlanSelected: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        val onUpgradeCancelled: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        val onSuccess: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        val onUpgrade: (String) -> Unit,
        val onDismiss: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onDisplayed = {},
                onPlanSelected = {},
                onError = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onSuccess = {},
                onDismiss = {}
            )
        }
    }
}
