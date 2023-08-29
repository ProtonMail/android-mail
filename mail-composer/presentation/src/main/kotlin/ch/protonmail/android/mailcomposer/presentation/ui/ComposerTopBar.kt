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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonTheme

@Composable
@Suppress("UseComposableActions")
internal fun ComposerTopBar(
    isAddAttachmentsButtonVisible: Boolean,
    onAddAttachmentsClick: () -> Unit,
    onCloseComposerClick: () -> Unit,
    onSendMessageComposerClick: () -> Unit,
    isSendMessageButtonEnabled: Boolean
) {
    ProtonTopAppBar(
        modifier = Modifier.testTag(ComposerTestTags.TopAppBar),
        title = {},
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(ComposerTestTags.CloseButton),
                onClick = onCloseComposerClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.close_composer_content_description)
                )
            }
        },
        actions = {
            if (isAddAttachmentsButtonVisible) {
                IconButton(
                    onClick = onAddAttachmentsClick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_paper_clip),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.composer_add_attachments_content_description)
                    )
                }
            }
            IconButton(
                modifier = Modifier.testTag(ComposerTestTags.SendButton),
                onClick = onSendMessageComposerClick,
                enabled = isSendMessageButtonEnabled
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_paper_plane),
                    tint = if (isSendMessageButtonEnabled) {
                        ProtonTheme.colors.iconNorm
                    } else {
                        ProtonTheme.colors.iconDisabled
                    },
                    contentDescription = stringResource(R.string.send_message_content_description)
                )
            }
        }
    )
}
