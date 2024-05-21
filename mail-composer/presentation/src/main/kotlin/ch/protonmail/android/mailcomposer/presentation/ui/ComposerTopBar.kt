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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.uicomponents.chips.thenIf
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongInverted

@Composable
@Suppress("UseComposableActions")
internal fun ComposerTopBar(
    attachmentsCount: Int,
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
            AttachmentsButton(attachmentsCount = attachmentsCount, onClick = onAddAttachmentsClick)
            IconButton(
                modifier = Modifier
                    .testTag(ComposerTestTags.SendButton)
                    .thenIf(!isSendMessageButtonEnabled) { semantics { disabled() } },
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

@Composable
private fun AttachmentsButton(
    attachmentsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-ProtonDimens.SmallSpacing.value).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (attachmentsCount > 0) {
            AttachmentsNumber(attachmentsCount)
        }
        IconButton(
            modifier = Modifier.testTag(ComposerTestTags.AttachmentsButton),
            onClick = onClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_paper_clip),
                contentDescription = stringResource(id = R.string.composer_add_attachments_content_description),
                tint = ProtonTheme.colors.iconNorm
            )
        }
    }
}

@Composable
private fun AttachmentsNumber(attachmentsCount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(ProtonTheme.colors.interactionNorm, CircleShape)
            .border(Dp.Hairline, ProtonTheme.colors.backgroundNorm, CircleShape)
            .padding(vertical = ProtonDimens.ExtraSmallSpacing, horizontal = ProtonDimens.SmallSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = attachmentsCount.toString(),
            style = ProtonTheme.typography.defaultSmallStrongInverted
        )
    }
}
