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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultWeak

@Composable
fun StorageLimitDialogs(
    storageLimitState: StorageLimitState,
    actions: StorageLimitDialogs.Actions,
    modifier: Modifier = Modifier
) {
    val quotaOverDialogState = remember { mutableStateOf(false) }

    if (storageLimitState is StorageLimitState.Notifiable) {
        ConsumableLaunchedEffect(effect = storageLimitState.showWarning) {
            quotaOverDialogState.value = true
        }
    }

    if (quotaOverDialogState.value) {
        StorageQuotaOverWarningDialog(
            onConfirmButtonClicked = {
                actions.dialogConfirmed
                quotaOverDialogState.value = false
            },
            modifier = modifier
        )
    }
}

@Composable
private fun StorageQuotaOverWarningDialog(modifier: Modifier = Modifier, onConfirmButtonClicked: () -> Unit = {}) {
    ProtonAlertDialog(
        modifier = modifier.testTag(StorageLimitWarningTestTags.QuotaOverDialog),
        titleResId = R.string.storage_quota_over_dialog_title,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.storage_quota_over_dialog_text),
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
            }
        },
        dismissButton = {},
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.storage_quota_over_dialog_confirm_button,
                modifier = Modifier.testTag(StorageLimitWarningTestTags.QuotaOverDialogOkButton)
            ) { onConfirmButtonClicked() }
        },
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun StorageQuotaOverWarningDialogPreview() {
    ProtonTheme3 {
        ProtonTheme {
            StorageQuotaOverWarningDialog(
                onConfirmButtonClicked = {}
            )
        }
    }
}

object StorageLimitDialogs {

    data class Actions(
        val dialogConfirmed: () -> Unit
    )
}

object StorageLimitWarningTestTags {

    const val QuotaOverDialog = "QuotaOverDialog"
    const val QuotaOverDialogOkButton = "QuotaOverDialogOkButton"
}
