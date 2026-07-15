/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.maildetail.presentation.ui.localcomposition.LocalHighlightQuery
import ch.protonmail.android.uicomponents.text.MultiWordHighlightedText

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ConversationSubjectHeader(
    subject: String,
    modifier: Modifier = Modifier,
    isDirectionForwards: () -> Boolean
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm)
            .padding(
                start = ProtonDimens.Spacing.Large,
                end = ProtonDimens.Spacing.Large,
                bottom = ProtonDimens.Spacing.ExtraLarge
            )
    ) {
        AnimatedContent(
            targetState = subject,
            transitionSpec = {
                if (isDirectionForwards()) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { currentSubject ->
            val highlightQuery = LocalHighlightQuery.current
            SelectionContainer {
                if (highlightQuery.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = currentSubject,
                        overflow = TextOverflow.Ellipsis,
                        style = ProtonTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                } else {
                    MultiWordHighlightedText(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = currentSubject,
                        highlight = highlightQuery,
                        highlightTextColor = ProtonTheme.colors.searchHighlightText,
                        highlightBackgroundColor = ProtonTheme.colors.searchHighlightBackground,
                        maxLines = Int.MAX_VALUE,
                        style = ProtonTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
