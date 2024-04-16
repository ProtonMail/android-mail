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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    avatarUiModel: AvatarUiModel,
    onClick: () -> Unit = {},
    clickable: Boolean = true,
    outerContainerSize: Dp = MailDimens.DefaultTouchTargetSize,
    avatarSize: Dp = MailDimens.AvatarMinSize,
    backgroundShape: Shape = ProtonTheme.shapes.medium
) {
    Box(
        modifier = modifier
            .testTag(AvatarTestTags.AvatarRootItem)
            .size(outerContainerSize)
            .run {
                if (clickable) {
                    clickable(onClick = onClick)
                } else {
                    this // Return the current modifier unchanged if not clickable
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (avatarUiModel) {
            is AvatarUiModel.DraftIcon ->
                Box(
                    modifier = Modifier
                        .testTag(AvatarTestTags.AvatarDraft)
                        .sizeIn(
                            minWidth = avatarSize,
                            minHeight = avatarSize
                        )
                        .border(
                            width = MailDimens.DefaultBorder,
                            color = ProtonTheme.colors.interactionWeakNorm,
                            shape = backgroundShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(ProtonDimens.SmallIconSize),
                        painter = painterResource(id = R.drawable.ic_proton_pencil),
                        contentDescription = NO_CONTENT_DESCRIPTION
                    )
                }

            is AvatarUiModel.ParticipantInitial ->
                Box(
                    modifier = Modifier
                        .sizeIn(
                            minWidth = avatarSize,
                            minHeight = avatarSize
                        )
                        .background(
                            color = ProtonTheme.colors.interactionWeakNorm,
                            shape = backgroundShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier
                            .testTag(AvatarTestTags.AvatarText),
                        textAlign = TextAlign.Center,
                        text = avatarUiModel.value
                    )
                }

            is AvatarUiModel.SelectionMode ->
                Box(
                    modifier = Modifier
                        .sizeIn(
                            minWidth = avatarSize,
                            minHeight = avatarSize
                        )
                        .border(
                            width = MailDimens.AvatarBorderLine,
                            color = ProtonTheme.colors.interactionNorm,
                            shape = backgroundShape
                        )
                        .background(
                            color = when (avatarUiModel.selected) {
                                true -> ProtonTheme.colors.interactionNorm
                                false -> ProtonTheme.colors.backgroundSecondary
                            },
                            shape = backgroundShape
                        )
                        .testTag(AvatarTestTags.AvatarSelectionMode)
                        .semantics { selected = avatarUiModel.selected },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUiModel.selected) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_proton_checkmark),
                            tint = Color.White,
                            contentDescription = NO_CONTENT_DESCRIPTION,
                            modifier = Modifier.size(MailDimens.AvatarCheckmarkSize)
                        )
                    }
                }
        }
    }
}

object AvatarTestTags {

    const val AvatarRootItem = "AvatarRootItem"
    const val AvatarText = "AvatarText"
    const val AvatarDraft = "AvatarDraft"
    const val AvatarSelectionMode = "AvatarSelectionMode"
}
