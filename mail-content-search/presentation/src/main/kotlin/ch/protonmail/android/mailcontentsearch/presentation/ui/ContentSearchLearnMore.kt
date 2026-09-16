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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcontentsearch.presentation.R

/** [description] followed by a "Learn more" link to the content search support page. */
@Composable
internal fun withLearnMoreLink(description: String, onLearnMoreClick: () -> Unit): AnnotatedString {
    val learnMore = stringResource(R.string.content_search_no_results_learn_more)
    val linkColor = ProtonTheme.colors.textAccent

    return buildAnnotatedString {
        append(description)
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "learn_more",
                styles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.None)
                ),
                linkInteractionListener = { onLearnMoreClick() }
            )
        ) {
            append(learnMore)
        }
    }
}
