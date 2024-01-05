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

package ch.protonmail.android.maillabel.presentation.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.mapper.UnreadCountValueMapper
import me.proton.core.compose.component.ProtonListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm

@Composable
fun SidebarItemWithCounter(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    isClickable: Boolean = true,
    isSelected: Boolean = false,
    textColor: Color = Color.Unspecified,
    iconTint: Color = LocalContentColor.current,
    count: Int? = null,
    onClick: () -> Unit = {}
) {
    ProtonListItem(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick,
        isClickable = isClickable,
        isSelected = isSelected,
        icon = { defaultModifier ->
            Icon(
                modifier = defaultModifier.then(iconModifier),
                painter = icon,
                contentDescription = null,
                tint = iconTint
            )
        },
        text = { defaultModifier ->
            Text(
                modifier = defaultModifier.then(textModifier),
                text = text,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        count = {
            count ?: return@ProtonListItem

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .defaultMinSize(ProtonDimens.CounterIconSize)
                    .background(color = ProtonTheme.colors.interactionNorm, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .testTag(SidebarItemWithCounterTestTags.Counter)
                        .padding(MailDimens.TinySpacing)
                        .padding(horizontal = MailDimens.TinySpacing),
                    text = UnreadCountValueMapper.toCappedValue(count),
                    color = Color.White,
                    style = ProtonTheme.typography.captionNorm
                )
            }
        }
    )
}

object SidebarItemWithCounterTestTags {

    const val Counter = "SidebarItemWithCounterItemCounter"
}
