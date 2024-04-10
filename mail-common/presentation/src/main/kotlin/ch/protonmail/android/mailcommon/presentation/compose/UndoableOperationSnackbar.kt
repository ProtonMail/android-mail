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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.string
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import timber.log.Timber

@Composable
fun UndoableOperationSnackbar(
    modifier: Modifier = Modifier,
    snackbarHostState: ProtonSnackbarHostState,
    actionEffect: Effect<ActionResult>
) {
    val coroutineScope = rememberCoroutineScope()
    val undoActionLabel = stringResource(id = R.string.undo_button_label)

    actionEffect.consume()?.let {
        val message = it.message.string()

        coroutineScope.launch {
            if (it.isUndoable) {
                val result = snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message, undoActionLabel)
                if (result == SnackbarResult.ActionPerformed) {
                    Timber.d("Undo action performed")
                }
            } else {
                snackbarHostState.showSnackbar(message = message, type = ProtonSnackbarType.NORM)
            }
        }
    }

}
