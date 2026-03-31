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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailupselling.presentation.model.UpsellingActions
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingViewModel

@Composable
fun UpsellingScreen(upsellingActions: UpsellingScreen.Actions, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<UpsellingViewModel>()

    val actions = upsellingActions.copy(
        onUpgradeAttempt = { upsellingTelemetryPayload ->
            viewModel.onPurchaseClicked()
            upsellingActions.onUpgradeAttempt(upsellingTelemetryPayload)
            viewModel.recordUpgradeAttempt(upsellingTelemetryPayload)
        },
        onSuccess = { upsellingTelemetryPayload ->
            viewModel.overrideUpsellingVisibility()
            upsellingActions.onSuccess(upsellingTelemetryPayload)
            viewModel.recordUpgradeSuccess(upsellingTelemetryPayload)
        },
        onUpgradeErrored = { upsellingTelemetryPayload ->
            viewModel.recordUpgradeError(upsellingTelemetryPayload)
        },
        onUpgradeCancelled = { upsellingTelemetryPayload ->
            viewModel.recordUpgradeCancelledByUser(upsellingTelemetryPayload)
        }
    )

    when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
        UpsellingScreenContentState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
        is UpsellingScreenContentState.Data -> when (state.plans.variant) {
            is PlanUpgradeVariant.Normal,
            is PlanUpgradeVariant.IntroductoryPrice,
            is PlanUpgradeVariant.SocialProof -> UpsellingScreenContent(modifier, state, actions)
            is PlanUpgradeVariant.BlackFriday -> UpsellingScreenContentBlackFriday(modifier, state, actions)
            is PlanUpgradeVariant.SpringPromo -> UpsellingScreenContentSpringPromo(modifier, state, actions)
        }

        is UpsellingScreenContentState.Error -> UpsellingScreenContentError(state = state, actions)
    }
}

object UpsellingScreen {

    data class Actions(
        override val onError: (String) -> Unit,
        override val onUpgradeAttempt: (UpsellingTelemetryPayload) -> Unit,
        override val onUpgradeCancelled: (UpsellingTelemetryPayload) -> Unit,
        override val onUpgradeErrored: (UpsellingTelemetryPayload) -> Unit,
        override val onSuccess: (UpsellingTelemetryPayload) -> Unit,
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

    const val UpsellingEntryPointKey = "UpsellingEntryPointKey"
    const val UpsellingTypeKey = "UpsellingTypeKey"
}

@AdaptivePreviews
@Composable
private fun BottomSheetPreview() {
    ProtonTheme {
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
