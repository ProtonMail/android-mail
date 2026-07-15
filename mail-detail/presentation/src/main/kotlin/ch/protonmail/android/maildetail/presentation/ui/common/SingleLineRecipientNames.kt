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

package ch.protonmail.android.maildetail.presentation.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.ui.localcomposition.LocalHighlightQuery
import ch.protonmail.android.uicomponents.text.MultiWordHighlightedText
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SingleLineRecipientNames(
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    recipients: ImmutableList<ParticipantUiModel>,
    hasUndisclosedRecipients: Boolean = false
) {
    val recipientMeText = stringResource(id = R.string.recipient_me)
    val toRecipientsLine = if (hasUndisclosedRecipients) {
        stringResource(R.string.undisclosed_recipients)
    } else {
        recipients.joinToString(separator = ", ") {
            if (it.isPrimaryUser) {
                recipientMeText
            } else {
                it.participantName.ifBlank { it.participantAddress }
            }
        }
    }

    val highlightQuery = LocalHighlightQuery.current
    if (highlightQuery.isEmpty() || hasUndisclosedRecipients) {
        Text(
            modifier = modifier,
            text = toRecipientsLine,
            style = textStyle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    } else {
        MultiWordHighlightedText(
            modifier = modifier,
            text = toRecipientsLine,
            highlight = highlightQuery,
            highlightTextColor = ProtonTheme.colors.searchHighlightText,
            highlightBackgroundColor = ProtonTheme.colors.searchHighlightBackground,
            maxLines = 1,
            style = textStyle,
            overflow = TextOverflow.Ellipsis
        )
    }
}
