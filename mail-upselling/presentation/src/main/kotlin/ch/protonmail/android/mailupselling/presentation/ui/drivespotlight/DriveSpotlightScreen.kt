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

package ch.protonmail.android.mailupselling.presentation.ui.drivespotlight

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightUIState
import ch.protonmail.android.mailupselling.presentation.viewmodel.DriveSpotlightViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun DriveSpotlightScreen(actions: DriveSpotlightScreen.Actions) {
    val viewmodel = hiltViewModel<DriveSpotlightViewModel>()
    val state = viewmodel.state.collectAsStateWithLifecycle().value
    when (state) {
        is DriveSpotlightUIState.Data -> DriveSpotlightBottomSheet(
            copy = if (state.storageGB != null) {
                TextUiModel.TextResWithArgs(R.string.drive_spotlight_description_with_gb, listOf(state.storageGB))
            } else {
                TextUiModel.TextRes(R.string.drive_spotlight_description)
            },
            onDismiss = actions.onDismiss,
            onEvent = viewmodel::submit
        )
        is DriveSpotlightUIState.Error -> {
            ConsumableTextEffect(effect = state.error) { message ->
                actions.onError(message)
            }
            actions.onDismiss()
        }
        DriveSpotlightUIState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
    }
}

object DriveSpotlightScreen {

    data class Actions(
        val onError: (String) -> Unit,
        val onDismiss: () -> Unit
    )
}
