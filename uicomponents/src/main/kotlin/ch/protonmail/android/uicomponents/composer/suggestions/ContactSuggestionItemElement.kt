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

package ch.protonmail.android.uicomponents.composer.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.uicomponents.R
import ch.protonmail.android.uicomponents.text.HighlightedText
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.util.kotlin.takeIfNotBlank

@Composable
fun ContactSuggestionItemElement(
    currentText: String,
    item: ContactSuggestionItem,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .clickable {
                when (item) {
                    is ContactSuggestionItem.Group ->
                        item.emails
                            .joinToString(separator = "\n")
                            .takeIfNotBlank()
                            ?.let { onClick(it) }

                    is ContactSuggestionItem.Contact -> onClick(item.email)
                }
            }
            .fillMaxSize()
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(vertical = ProtonDimens.SmallSpacing)
    ) {
        when (item) {
            is ContactSuggestionItem.Group -> ContactSuggestionGroupEntry(currentText, item)
            is ContactSuggestionItem.Contact -> ContactSuggestionEntry(currentText, item)
        }
    }
}

@Composable
private fun ContactSuggestionEntry(currentText: String, item: ContactSuggestionItem.Contact) {
    Row {
        Box(
            modifier = Modifier
                .size(ProtonDimens.LargeSpacing)
                .background(
                    color = ProtonTheme.colors.backgroundSecondary,
                    shape = ProtonTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = item.initials
            )
        }

        Spacer(Modifier.width(ProtonDimens.DefaultSpacing))

        Column {
            HighlightedText(
                text = item.header,
                highlight = currentText,
                maxLines = 1,
                style = ProtonTheme.typography.defaultNorm,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
            HighlightedText(
                text = item.subheader,
                highlight = currentText,
                maxLines = 1,
                style = ProtonTheme.typography.defaultSmallWeak,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactSuggestionGroupEntry(currentText: String, item: ContactSuggestionItem.Group) {
    Row {
        Box(
            modifier = Modifier
                .size(ProtonDimens.LargeSpacing)
                .background(
                    color = item.backgroundColor,
                    shape = ProtonTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                imageVector = ImageVector.vectorResource(R.drawable.ic_proton_users_filled),
                tint = Color.White,
                contentDescription = null
            )
        }

        Spacer(Modifier.width(ProtonDimens.DefaultSpacing))

        Column {
            HighlightedText(
                text = item.header,
                highlight = currentText,
                maxLines = 1,
                style = ProtonTheme.typography.defaultNorm,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
            Text(
                text = item.subheader,
                maxLines = 1,
                style = ProtonTheme.typography.defaultSmallWeak,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
