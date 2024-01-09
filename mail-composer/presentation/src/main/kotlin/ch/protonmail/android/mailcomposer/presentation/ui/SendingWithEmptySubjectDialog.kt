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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultWeak

@Composable
fun SendingWithEmptySubjectDialog(
    onConfirmClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonAlertDialog(
        modifier = modifier.testTag(ComposerTestTags.SendWithEmptySubjectDialog),
        titleResId = R.string.composer_send_without_subject_dialog_title,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.composer_send_without_subject_dialog_text),
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.composer_send_without_subject_dialog_reject_button,
                modifier = Modifier.testTag(ComposerTestTags.SendWithEmptySubjectDialogDismiss)
            ) { onDismissClicked() }
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.composer_send_without_subject_dialog_confirm_button,
                modifier = Modifier.testTag(ComposerTestTags.SendWithEmptySubjectDialogConfirm)
            ) { onConfirmClicked() }
        },
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun SendingWithEmptySubjectDialogPreview() {
    ProtonTheme3 {
        ProtonTheme {
            SendingWithEmptySubjectDialog(onConfirmClicked = {}, onDismissClicked = {})
        }
    }
}
