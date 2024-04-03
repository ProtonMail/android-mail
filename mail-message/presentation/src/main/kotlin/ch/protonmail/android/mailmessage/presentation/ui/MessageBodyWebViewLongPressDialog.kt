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

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailmessage.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageWebViewLongPressDialog(actions: MessageWebViewLongPressDialog.Actions, linkUri: Uri) {
    BasicAlertDialog(onDismissRequest = actions.onDismissed) {
        Box(
            modifier = Modifier
                .background(ProtonTheme.colors.backgroundNorm)
                .clip(ProtonTheme.shapes.medium)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DialogLinkRow(
                    text = stringResource(id = R.string.message_link_long_click_copy),
                    uriAction = actions.onCopyClicked,
                    linkUri = linkUri
                )

                MailDivider()

                DialogLinkRow(
                    text = stringResource(id = R.string.message_link_long_click_share),
                    uriAction = actions.onShareClicked,
                    linkUri = linkUri
                )
            }
        }
    }
}

object MessageWebViewLongPressDialog {
    data class Actions(
        val onCopyClicked: (Uri) -> Unit,
        val onShareClicked: (Uri) -> Unit,
        val onDismissed: () -> Unit
    )
}

@Composable
private fun DialogLinkRow(
    text: String,
    uriAction: (Uri) -> Unit,
    linkUri: Uri
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriAction(linkUri) }
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Text(text = text, style = ProtonTheme.typography.defaultNorm())
    }
}


@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun test() {
    ProtonTheme {
        MessageWebViewLongPressDialog(
            MessageWebViewLongPressDialog.Actions(
                onCopyClicked = {},
                onShareClicked = {},
                onDismissed = {}
            ),
            linkUri = Uri.EMPTY
        )
    }
}
