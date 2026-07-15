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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumHint
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.R.drawable
import ch.protonmail.android.maildetail.presentation.R.plurals
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreview
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreviewProvider
import ch.protonmail.android.uicomponents.text.MultiWordHighlightedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String,
    isStarred: Boolean?,
    messageCount: Int?,
    actions: DetailScreenTopBar.Actions,
    subjectAlpha: Float = 0f,
    highlightQuery: String = ""
) {
    Box(
        modifier = modifier
            .background(ProtonTheme.colors.backgroundNorm)
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .height(MailDimens.SingleLineTopAppBarHeight)
            .testTag(DetailScreenTopBarTestTags.RootItem)
    ) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = actions.onBackClick
        ) {
            Icon(
                modifier = Modifier.testTag(DetailScreenTopBarTestTags.BackButton),
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = string.presentation_back),
                tint = ProtonTheme.colors.textNorm
            )
        }

        messageCount?.let { count ->
            Text(
                modifier = Modifier
                    .testTag(DetailScreenTopBarTestTags.MessageCount)
                    .align(Alignment.Center)
                    .fillMaxWidth(fraction = 0.7f)
                    .padding(horizontal = ProtonDimens.Spacing.Jumbo)
                    .alpha(1 - subjectAlpha),
                text = pluralStringResource(plurals.message_count_label_text, count, count),
                style = ProtonTheme.typography.bodyMediumHint,
                textAlign = TextAlign.Center
            )
        }

        val titleModifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(fraction = 0.7f)
            .padding(horizontal = ProtonDimens.Spacing.Jumbo)
            .alpha(subjectAlpha)
            .wrapContentHeight(align = Alignment.CenterVertically)

        if (highlightQuery.isEmpty()) {
            Text(
                modifier = titleModifier,
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ProtonTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        } else {
            MultiWordHighlightedText(
                modifier = titleModifier,
                text = title,
                highlight = highlightQuery,
                highlightTextColor = ProtonTheme.colors.searchHighlightText,
                highlightBackgroundColor = ProtonTheme.colors.searchHighlightBackground,
                maxLines = 1,
                style = ProtonTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isStarred != null) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = if (isStarred) actions.onUnStarClick else actions.onStarClick
            ) {
                Icon(
                    modifier = Modifier.size(ProtonDimens.IconSize.Default),
                    painter = getStarredIcon(isStarred),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = getStarredIconColor(isStarred)
                )
            }
        }
    }
}

@Composable
private fun getStarredIconColor(isStarred: Boolean) = if (isStarred) {
    ProtonTheme.colors.starSelected
} else {
    ProtonTheme.colors.starDefault
}

@Composable
private fun getStarredIcon(isStarred: Boolean) = painterResource(
    id = if (isStarred) {
        drawable.ic_proton_star_filled
    } else {
        drawable.ic_proton_star
    }
)

object DetailScreenTopBar {

    /**
     * Using an empty String for a Text inside LargeTopAppBar causes a crash.
     */
    const val NoTitle = " "

    data class Actions(
        val onBackClick: () -> Unit,
        val onStarClick: () -> Unit,
        val onUnStarClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onStarClick = {},
                onUnStarClick = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
@OptIn(ExperimentalMaterial3Api::class)
private fun DetailScreenTopBarPreview(
    @PreviewParameter(DetailsScreenTopBarPreviewProvider::class) preview: DetailsScreenTopBarPreview
) {
    ProtonTheme {
        DetailScreenTopBar(
            title = preview.title,
            isStarred = preview.isStarred,
            messageCount = preview.messageCount,
            actions = DetailScreenTopBar.Actions.Empty
        )
    }
}

object DetailScreenTopBarTestTags {

    const val RootItem = "DetailScreenTopBarRootItem"
    const val BackButton = "BackButton"
    const val MessageCount = "MessageCount"
    const val Subject = "Subject"
}
