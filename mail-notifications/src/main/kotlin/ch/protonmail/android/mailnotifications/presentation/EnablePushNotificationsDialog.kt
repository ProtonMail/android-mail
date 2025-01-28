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

package ch.protonmail.android.mailnotifications.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import me.proton.core.compose.theme.ProtonDimens
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogType
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm

@Composable
fun EnablePushNotificationsDialog(
    state: NotificationPermissionDialogState.Shown,
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(MailDimens.DialogCardRadius),
            backgroundColor = ProtonTheme.colors.backgroundNorm,
            modifier = Modifier.fillMaxWidth()
        ) {
            val (title, message) = when (state.type) {
                is NotificationPermissionDialogType.PostOnboarding ->
                    Pair(
                        R.string.notification_permission_dialog_title,
                        R.string.notification_permission_dialog_message
                    )
                is NotificationPermissionDialogType.PostSending ->
                    Pair(
                        R.string.notification_permission_dialog_post_send_title,
                        R.string.notification_permission_dialog_post_send_message
                    )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = ProtonTheme.colors.backgroundSecondary)
                        .padding(ProtonDimens.LargeSpacing),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier.size(width = ImageWidth, height = ImageHeight),
                        painter = painterResource(id = R.drawable.ic_email_reminder),
                        contentDescription = NO_CONTENT_DESCRIPTION
                    )
                }

                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))

                Text(
                    modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                    text = stringResource(id = title),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultStrongNorm
                )

                Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))

                Text(
                    modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                    text = stringResource(id = message),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultSmallWeak
                )

                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))

                ProtonSolidButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ProtonDimens.LargerSpacing)
                        .padding(horizontal = ProtonDimens.LargeSpacing),
                    onClick = onEnable
                ) {
                    Text(
                        text = stringResource(id = R.string.notification_permission_dialog_button_enable),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ProtonDimens.LargeSpacing)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onDismiss
                        ),
                    text = stringResource(id = R.string.notification_permission_dialog_button_dismiss),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(
                        color = ProtonTheme.colors.brandNorm
                    )
                )

                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            }
        }
    }
}

private val ImageWidth = 98.dp
private val ImageHeight = 94.dp

@Composable
@Preview(showBackground = true)
private fun EnablePushNotificationsDialogPreview() {
    EnablePushNotificationsDialog(
        state = NotificationPermissionDialogState.Shown(
            type = NotificationPermissionDialogType.PostOnboarding
        ),
        onEnable = {},
        onDismiss = {}
    )
}
