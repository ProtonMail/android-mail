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

@file:OptIn(ExperimentalMaterialApi::class)

package ch.protonmail.android.uicomponents.snackbar

import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarData
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState

@Composable
fun DismissableSnackbarHost(
    modifier: Modifier = Modifier,
    protonSnackbarHostState: ProtonSnackbarHostState,
    customSnackbar: (@Composable (SnackbarData) -> Unit)? = null
) {
    val dismissSnackbarState = rememberDismissState(confirmStateChange = { value ->
        if (value != DismissValue.Default) {
            protonSnackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
            return@rememberDismissState true
        }

        false
    })

    LaunchedEffect(dismissSnackbarState.currentValue) {
        if (dismissSnackbarState.currentValue != DismissValue.Default) {
            dismissSnackbarState.reset()
        }
    }

    SwipeToDismiss(
        state = dismissSnackbarState,
        background = {},
        dismissContent = {
            if (customSnackbar != null) {
                ProtonSnackbarHost(
                    modifier = modifier,
                    hostState = protonSnackbarHostState,
                    snackbar = customSnackbar
                )
            } else {
                ProtonSnackbarHost(
                    modifier = modifier,
                    hostState = protonSnackbarHostState
                )
            }
        }
    )
}

fun SnackbarData.shouldGoInTwoRows(): Boolean {
    return actionLabel.orEmpty().length > THRESHOLD_LONG_ACTION ||
        message.length > THRESHOLD_LONG_MESSAGE
}

private const val THRESHOLD_LONG_MESSAGE = 60
private const val THRESHOLD_LONG_ACTION = 25
