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

package ch.protonmail.android.mailmessage.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.AttachmentUiModel
import ch.protonmail.android.mailmessage.presentation.model.getContentDescriptionForMimeType
import ch.protonmail.android.mailmessage.presentation.model.getDrawableForMimeType
import ch.protonmail.android.mailmessage.presentation.sample.AttachmentUiModelSample
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionHint
import me.proton.core.compose.theme.defaultSmall

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AttachmentItem(
    modifier: Modifier = Modifier,
    attachmentUiModel: AttachmentUiModel,
    onAttachmentItemClicked: (attachmentId: AttachmentId) -> Unit,
    onAttachmentItemDeleteClicked: (attachmentId: AttachmentId) -> Unit
) {
    val currentContext = LocalContext.current
    val shouldShowPermissionRationaleDialog = remember { mutableStateOf(false) }
    val externalStoragePermission = rememberPermissionState(
        permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        onPermissionResult = { result ->
            if (result) {
                onAttachmentItemClicked(AttachmentId(attachmentUiModel.attachmentId))
            } else {
                shouldShowPermissionRationaleDialog.value = true
            }
        }
    )

    if (shouldShowPermissionRationaleDialog.value) {
        ProtonAlertDialog(
            title = stringResource(id = R.string.attachment_permission_dialog_title),
            text = { ProtonAlertDialogText(R.string.attachment_permission_dialog_message) },
            dismissButton = {
                ProtonAlertDialogButton(R.string.attachment_permission_dialog_dismiss_button) {
                    shouldShowPermissionRationaleDialog.value = false
                }
            },
            confirmButton = {
                ProtonAlertDialogButton(R.string.attachment_permission_dialog_action_button) {
                    shouldShowPermissionRationaleDialog.value = false
                    currentContext.startActivity(
                        Intent().apply {
                            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", currentContext.packageName, null)
                        }
                    )
                }
            },
            onDismissRequest = { shouldShowPermissionRationaleDialog.value = false }
        )
    }

    Row(
        modifier = modifier
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing)
            .padding(horizontal = ProtonDimens.ExtraSmallSpacing)
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.interactionWeakNorm,
                shape = ProtonTheme.shapes.large
            )
            .clickable {
                // For now the deletable flag indicates the usage in composer.
                // Since currently opening an attachment in the the composer is not supported,
                // the click shouldn't do anything.
                if (attachmentUiModel.deletable.not()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || externalStoragePermission.status.isGranted) {
                        onAttachmentItemClicked(AttachmentId(attachmentUiModel.attachmentId))
                    } else {
                        externalStoragePermission.launchPermissionRequest()
                    }
                }
            }
            .padding(ProtonDimens.SmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (attachmentUiModel.status == AttachmentWorkerStatus.Running) {
            CircularProgressIndicator(
                modifier = Modifier
                    .testTag(AttachmentItemTestTags.Loader)
                    .size(ProtonDimens.DefaultIconSize)
            )
        } else {
            Image(
                modifier = Modifier.testTag(AttachmentItemTestTags.Icon),
                painter = painterResource(id = getDrawableForMimeType(attachmentUiModel.mimeType)),
                contentDescription = stringResource(
                    id = R.string.attachment_type_description,
                    stringResource(id = getContentDescriptionForMimeType(attachmentUiModel.mimeType))
                )
            )
        }
        Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
        Text(
            modifier = Modifier
                .testTag(AttachmentItemTestTags.Name)
                .weight(1f, fill = false),
            style = ProtonTheme.typography.defaultSmall,
            text = attachmentUiModel.fileName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier
                .testTag(AttachmentItemTestTags.Extension)
                .padding(end = ProtonDimens.SmallSpacing),
            text = ".${attachmentUiModel.extension}",
            style = ProtonTheme.typography.defaultSmall
        )
        Text(
            modifier = Modifier.testTag(AttachmentItemTestTags.Size),
            text = Formatter.formatShortFileSize(LocalContext.current, attachmentUiModel.size),
            style = ProtonTheme.typography.captionHint
        )
        if (attachmentUiModel.deletable) {
            Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
            Image(
                modifier = Modifier
                    .clickable { onAttachmentItemDeleteClicked(AttachmentId(attachmentUiModel.attachmentId)) }
                    .testTag(AttachmentItemTestTags.Delete),
                painter = painterResource(id = R.drawable.ic_proton_cross_small),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun AttachmentItemPreview() {
    ProtonTheme {
        AttachmentItem(
            attachmentUiModel = AttachmentUiModelSample.invoice,
            onAttachmentItemClicked = {},
            onAttachmentItemDeleteClicked = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun AttachmentItemTruncationPreview() {
    ProtonTheme {
        AttachmentItem(
            attachmentUiModel = AttachmentUiModelSample.documentWithReallyLongFileName,
            onAttachmentItemClicked = {},
            onAttachmentItemDeleteClicked = {}
        )
    }
}

object AttachmentItemTestTags {

    const val Loader = "AttachmentLoader"
    const val Icon = "AttachmentIcon"
    const val Name = "AttachmentName"
    const val Extension = "AttachmentExtension"
    const val Size = "AttachmentSize"
    const val Delete = "AttachmentDelete"
}
