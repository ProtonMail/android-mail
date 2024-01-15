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
    val firstLimitOverDialogState = remember { mutableStateOf(false) }
    val secondLimitOverDialogState = remember { mutableStateOf(false) }

    if (storageLimitState is StorageLimitState.Notifiable) {
        ConsumableLaunchedEffect(effect = storageLimitState.showWarning) {
            when (storageLimitState) {
                is StorageLimitState.Notifiable.QuotaOver -> {
                    quotaOverDialogState.value = true
                }

                is StorageLimitState.Notifiable.FirstLimitOver -> {
                    firstLimitOverDialogState.value = true
                }

                is StorageLimitState.Notifiable.SecondLimitOver -> {
                    secondLimitOverDialogState.value = true
                }
            }
        }
    }

    if (quotaOverDialogState.value) {
        StorageQuotaOverWarningDialog(
            onConfirmButtonClicked = actions.dialogConfirmed,
            modifier = modifier
        )
    }

    if (firstLimitOverDialogState.value) {
        StorageLimitWarningDialog(
            warningTextResId = R.string.storage_first_limit_exceeded_warning_dialog_text,
            onDoNotRemindClicked = {
                actions.doNotRemindClicked()
                firstLimitOverDialogState.value = false
            },
            onConfirmButtonClicked = {
                actions.dialogConfirmed()
                firstLimitOverDialogState.value = false
            },
            modifier = modifier
        )
    }

    if (secondLimitOverDialogState.value) {
        StorageLimitWarningDialog(
            warningTextResId = R.string.storage_second_limit_exceeded_warning_dialog_text,
            onDoNotRemindClicked = {
                actions.doNotRemindClicked()
                secondLimitOverDialogState.value = false
            },
            onConfirmButtonClicked = {
                actions.dialogConfirmed()
                secondLimitOverDialogState.value = false
            },
            modifier = modifier
        )
    }
}

@Composable
fun StorageLimitWarningDialog(
    modifier: Modifier = Modifier,
    warningTextResId: Int,
    onDoNotRemindClicked: () -> Unit,
    onConfirmButtonClicked: () -> Unit = {}
) {
    ProtonAlertDialog(
        modifier = modifier.testTag(StorageLimitWarningTestTags.LimitWarningDialog),
        titleResId = R.string.storage_limit_warning_dialog_title,
        text = {
            Column {
                Text(
                    text = stringResource(id = warningTextResId),
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.storage_limit_warning_dialog_do_not_remind_button,
                modifier = Modifier.testTag(StorageLimitWarningTestTags.LimitWarningDialogDoNotRemindButton)
            ) { onDoNotRemindClicked() }
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.storage_limit_warning_dialog_confirm_button,
                modifier = Modifier.testTag(StorageLimitWarningTestTags.LimitWarningDialogOkButton)
            ) { onConfirmButtonClicked() }
        },
        onDismissRequest = {}
    )
}

@Composable
fun StorageQuotaOverWarningDialog(modifier: Modifier = Modifier, onConfirmButtonClicked: () -> Unit = {}) {
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
private fun FirstStorageLimitWarningDialogPreview() {
    ProtonTheme3 {
        ProtonTheme {
            StorageLimitWarningDialog(
                warningTextResId = R.string.storage_first_limit_exceeded_warning_dialog_text,
                onDoNotRemindClicked = {},
                onConfirmButtonClicked = {}
            )
        }
    }
}

@Preview
@Composable
private fun SecondStorageLimitWarningDialogPreview() {
    ProtonTheme3 {
        ProtonTheme {
            StorageLimitWarningDialog(
                warningTextResId = R.string.storage_second_limit_exceeded_warning_dialog_text,
                onDoNotRemindClicked = {},
                onConfirmButtonClicked = {}
            )
        }
    }
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
        val dialogConfirmed: () -> Unit,
        val doNotRemindClicked: () -> Unit
    )
}

object StorageLimitWarningTestTags {

    const val LimitWarningDialog = "LimitWarningDialog"
    const val LimitWarningDialogOkButton = "LimitWarningDialogOkButton"
    const val LimitWarningDialogDoNotRemindButton = "LimitWarningDialogDoNotRemindButton"
    const val QuotaOverDialog = "QuotaOverDialog"
    const val QuotaOverDialogOkButton = "QuotaOverDialogOkButton"
}
