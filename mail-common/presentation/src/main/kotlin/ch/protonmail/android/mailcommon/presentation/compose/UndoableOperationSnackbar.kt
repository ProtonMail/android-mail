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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.viewmodel.UndoOperationViewModel
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import timber.log.Timber

@Composable
fun UndoableOperationSnackbar(
    modifier: Modifier = Modifier,
    snackbarHostState: ProtonSnackbarHostState,
    actionEffect: Effect<ActionResult>,
    viewModel: UndoOperationViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val undoActionLabel = stringResource(id = R.string.undo_button_label)
    val state = viewModel.state.collectAsState()

    val undoSuccessMessage = stringResource(id = R.string.undo_success_message)
    suspend fun showUndoSuccess() {
        snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, undoSuccessMessage)
    }

    val undoFailureMessage = stringResource(id = R.string.undo_failure_message)
    suspend fun showUndoFailure() {
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, undoFailureMessage)
    }

    ConsumableLaunchedEffect(effect = state.value.undoSucceeded) { showUndoSuccess() }
    ConsumableLaunchedEffect(effect = state.value.undoFailed) { showUndoFailure() }

    actionEffect.consume()?.let {
        val message = it.message.string()

        coroutineScope.launch {
            if (it is ActionResult.UndoableActionResult) {
                val result = snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message, undoActionLabel)
                if (result == SnackbarResult.ActionPerformed) {
                    Timber.d("Undo action performed - $it")
                    viewModel.submitUndo()
                }
            } else {
                snackbarHostState.showSnackbar(message = message, type = ProtonSnackbarType.NORM)
            }
        }
    }

}
