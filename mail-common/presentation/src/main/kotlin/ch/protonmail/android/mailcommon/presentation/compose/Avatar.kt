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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.R.string
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    avatarUiModel: AvatarUiModel
) {
    Box(
        modifier = modifier.size(MailDimens.DefaultTouchTargetSize),
        contentAlignment = Alignment.Center
    ) {
        when (avatarUiModel) {
            is AvatarUiModel.DraftIcon ->
                Box(
                    modifier = modifier
                        .sizeIn(
                            minWidth = MailDimens.AvatarMinSize,
                            minHeight = MailDimens.AvatarMinSize
                        )
                        .border(
                            width = MailDimens.DefaultBorder,
                            color = ProtonTheme.colors.interactionWeakNorm,
                            shape = ProtonTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(ProtonDimens.SmallIconSize),
                        painter = painterResource(id = R.drawable.ic_proton_pencil),
                        contentDescription = stringResource(id = string.draft_avatar_icon_description)
                    )
                }
            is AvatarUiModel.ParticipantInitial ->
                Box(
                    modifier = modifier
                        .sizeIn(
                            minWidth = MailDimens.AvatarMinSize,
                            minHeight = MailDimens.AvatarMinSize
                        )
                        .background(
                            color = ProtonTheme.colors.interactionWeakNorm,
                            shape = ProtonTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                        textAlign = TextAlign.Center,
                        text = avatarUiModel.value
                    )
                }
        }
    }
}
