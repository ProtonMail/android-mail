/*
 * Copyright (c) 2026 Proton Technologies AG
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.titleMediumNorm
import ch.protonmail.android.mailcommon.presentation.model.asDisplayText
import ch.protonmail.android.mailcommon.presentation.model.isEmpty
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState

@Composable
internal fun BottomUnreadFilterButton(
    state: UnreadFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state !is UnreadFilterState.Data) return

    val isActive = state.isFilterEnabled

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) ProtonTheme.colors.brandNorm else ProtonTheme.colors.interactionFabNorm,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "unreadBg"
    )

    val contentColor = if (isActive) ProtonTheme.colors.textInverted else ProtonTheme.colors.textNorm

    Surface(
        modifier = modifier.height(UnreadHeight),
        shape = RoundedCornerShape(percent = 50),
        shadowElevation = ProtonDimens.ShadowElevation.Mini,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    if (isActive) onFilterDisabled() else onFilterEnabled()
                }
                .padding(
                    start = ProtonDimens.Spacing.ModeratelyLarger,
                    end = if (isActive) ProtonDimens.Spacing.Medium else ProtonDimens.Spacing.ModeratelyLarger
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.filter_unread_button_text))

                    if (!state.unreadCount.isEmpty()) {
                        append(" ")
                        append(state.unreadCount.asDisplayText())
                    }
                },
                style = ProtonTheme.typography.titleMediumNorm,
                color = contentColor
            )

            if (isActive) {
                Icon(
                    modifier = Modifier
                        .padding(start = ProtonDimens.Spacing.Small)
                        .size(ProtonDimens.IconSize.Medium),
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

private val UnreadHeight = 56.dp
