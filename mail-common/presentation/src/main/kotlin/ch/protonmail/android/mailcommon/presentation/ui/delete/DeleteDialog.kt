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

package ch.protonmail.android.mailcommon.presentation.ui.delete

import androidx.compose.runtime.Composable
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.string
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText

@Composable
fun DeleteDialog(
    state: DeleteDialogState,
    confirm: () -> Unit,
    dismiss: () -> Unit
) {
    if (state is DeleteDialogState.Shown) {
        ProtonAlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                ProtonAlertDialogButton(R.string.mailbox_action_delete_dialog_button_delete) {
                    confirm()
                }
            },
            dismissButton = {
                ProtonAlertDialogButton(R.string.mailbox_action_delete_dialog_button_cancel) {
                    dismiss()
                }
            },
            title = state.title.string(),
            text = { ProtonAlertDialogText(state.message.string()) }
        )
    }
}
