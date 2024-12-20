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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import me.proton.core.compose.theme.ProtonDimens
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.presentation.model.EnablePushNotificationsUiModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm

@Composable
fun EnablePushNotificationsDialog(uiModel: EnablePushNotificationsUiModel) {
    Dialog(
        onDismissRequest = {}
    ) {
        Card(
            shape = RoundedCornerShape(MailDimens.DialogCardRadius),
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    text = uiModel.title.string(),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultStrongNorm
                )

                Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))

                Text(
                    modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                    text = uiModel.message.string(),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultSmallWeak
                )

                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))

                ProtonSolidButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ProtonDimens.LargerSpacing)
                        .padding(horizontal = ProtonDimens.LargeSpacing),
                    onClick = {}
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
                            onClick = {}
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
        uiModel = EnablePushNotificationsUiModel(
            title = TextUiModel.TextRes(R.string.notification_permission_dialog_title),
            message = TextUiModel.TextRes(R.string.notification_permission_dialog_message)
        )
    )
}
