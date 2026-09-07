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

package ch.protonmail.android.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.debug.AppStateViewModel.ResetTarget
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AppStateScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: AppStateViewModel = hiltViewModel()
) {
    val resetCounts = remember { mutableStateMapOf<ResetTarget, Int>() }

    LaunchedEffect(viewModel) {
        viewModel.resets.collect { target ->
            resetCounts[target] = (resetCounts[target] ?: 0) + 1
        }
    }

    AppStateScreen(
        modifier = modifier,
        resetCounts = resetCounts,
        actions = AppStateScreen.Actions(
            onBackClick = onBackClick,
            onResetOnboarding = { viewModel.resetOnboarding() },
            onResetFeatureSpotlight = { viewModel.resetFeatureSpotlight() },
            onResetNotificationPrompt = { viewModel.resetNotificationPrompt() }
        )
    )
}

@Composable
private fun AppStateScreen(
    resetCounts: Map<ResetTarget, Int>,
    actions: AppStateScreen.Actions,
    modifier: Modifier = Modifier
) {
    DebugMenuScaffold(
        modifier = modifier,
        title = R.string.app_debug_screen_app_state_title,
        onCloseClick = actions.onBackClick
    ) {
        debugMenuSection(title = R.string.app_debug_screen_app_state_reset_section_title) {
            DebugMenuRow(
                icon = R.drawable.ic_proton_rocket,
                title = R.string.app_debug_screen_app_state_reset_onboarding_title,
                description = R.string.app_debug_screen_app_state_reset_onboarding_description,
                trailing = { ResetConfirmation(resetCounts[ResetTarget.Onboarding] ?: 0) },
                onClick = actions.onResetOnboarding
            )
            DebugMenuRow(
                icon = R.drawable.ic_proton_lightbulb,
                title = R.string.app_debug_screen_app_state_reset_spotlight_title,
                description = R.string.app_debug_screen_app_state_reset_spotlight_description,
                trailing = { ResetConfirmation(resetCounts[ResetTarget.FeatureSpotlight] ?: 0) },
                onClick = actions.onResetFeatureSpotlight
            )
            DebugMenuRow(
                icon = R.drawable.ic_proton_bell,
                title = R.string.app_debug_screen_app_state_reset_notifications_title,
                description = R.string.app_debug_screen_app_state_reset_notifications_description,
                trailing = { ResetConfirmation(resetCounts[ResetTarget.NotificationPrompt] ?: 0) },
                onClick = actions.onResetNotificationPrompt
            )
        }
    }
}

/**
 * A checkmark that appears for [ConfirmationDurationMillis] after a reset, then fades out.
 *
 * [resetCount] is the key rather than a boolean, so tapping the same row again restarts the
 * checkmark instead of leaving the first one to expire.
 */
@Composable
private fun ResetConfirmation(resetCount: Int) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(resetCount) {
        if (resetCount == 0) return@LaunchedEffect

        isVisible = true
        delay(ConfirmationDurationMillis.milliseconds)
        isVisible = false
    }

    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
        Icon(
            modifier = Modifier.size(ProtonDimens.IconSize.Medium),
            painter = painterResource(R.drawable.ic_proton_checkmark_circle_filled),
            contentDescription = null,
            tint = ProtonTheme.colors.notificationSuccess
        )
    }
}

private const val ConfirmationDurationMillis = 1_500L

object AppStateScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onResetOnboarding: () -> Unit,
        val onResetFeatureSpotlight: () -> Unit,
        val onResetNotificationPrompt: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onResetOnboarding = {},
                onResetFeatureSpotlight = {},
                onResetNotificationPrompt = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun AppStateScreenPreview() {
    ProtonTheme {
        AppStateScreen(
            resetCounts = mapOf(ResetTarget.Onboarding to 1),
            actions = AppStateScreen.Actions.Empty
        )
    }
}
