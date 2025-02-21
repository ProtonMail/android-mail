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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.domain.model.UpsellingActions
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingViewModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.theme.ProtonTheme3

@Composable
fun UpsellingScreen(
    modifier: Modifier = Modifier,
    bottomSheetActions: UpsellingScreen.Actions,
    entryPoint: UpsellingEntryPoint.Feature
) {

    val viewModel = hiltViewModel<UpsellingViewModel, UpsellingViewModel.Factory> { factory ->
        factory.create(entryPoint)
    }

    val actions = bottomSheetActions.copy(
        onDisplayed = { viewModel.updateLastSeenTimestamp() },
        onUpgradeAttempt = { viewModel.trackUpgradeAttempt(it) },
        onUpgradeCancelled = { viewModel.trackUpgradeCancelled(it) },
        onUpgradeErrored = { viewModel.trackUpgradeErrored(it) },
        onSuccess = { viewModel.trackPurchaseCompleted(it) }
    )

    when (val state = viewModel.state.collectAsState().value) {
        UpsellingScreenContentState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
        is UpsellingScreenContentState.Data -> {
            val isStandalone = entryPoint is UpsellingEntryPoint.Standalone
            CompositionLocalProvider(LocalEntryPointIsStandalone provides isStandalone) {
                UpsellingScreenContent(modifier, state, actions)
            }
        }

        is UpsellingScreenContentState.Error -> UpsellingScreenContentError(state = state, actions)
    }
}

object UpsellingScreen {

    data class Actions(
        override val onError: (String) -> Unit,
        override val onUpgradeAttempt: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgradeCancelled: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgradeErrored: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onSuccess: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgrade: (String) -> Unit,
        override val onDismiss: () -> Unit,
        val onDisplayed: suspend () -> Unit
    ) : UpsellingActions {

        companion object {

            val Empty = Actions(
                onDisplayed = {},
                onUpgradeAttempt = {},
                onError = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {},
                onDismiss = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun BottomSheetPreview() {
    ProtonTheme3 {
        UpsellingScreenContent(
            state = UpsellingContentPreviewData.Base,
            actions = UpsellingScreen.Actions(
                onDisplayed = {},
                onDismiss = {},
                onError = {},
                onUpgradeAttempt = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {}
            )
        )
    }
}
