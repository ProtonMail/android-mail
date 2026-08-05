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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.runtime.Composable
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.component.ProtonAlertDialogButton
import ch.protonmail.android.design.compose.component.ProtonAlertDialogText
import ch.protonmail.android.mailcontentsearch.presentation.R

@Composable
internal fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ProtonAlertDialog(
        onDismissRequest = onDismiss,
        titleResId = R.string.content_search_delete_dialog_title,
        text = { ProtonAlertDialogText(textResId = R.string.content_search_delete_dialog_message) },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.content_search_delete_dialog_confirm,
                onClick = onConfirm
            )
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.content_search_delete_dialog_cancel,
                onClick = onDismiss
            )
        }
    )
}
