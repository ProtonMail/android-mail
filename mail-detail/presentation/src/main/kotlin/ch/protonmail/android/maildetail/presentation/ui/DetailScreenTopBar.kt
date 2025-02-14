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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.maildetail.presentation.R.color
import ch.protonmail.android.maildetail.presentation.R.drawable
import ch.protonmail.android.maildetail.presentation.R.plurals
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreview
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreviewProvider
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.headlineNorm
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String,
    isStarred: Boolean?,
    messageCount: Int?,
    actions: DetailScreenTopBar.Actions,
    subjectHeaderSizeCallback: (Int) -> Unit,
    topAppBarState: TopAppBarState
) {
    ProtonTheme3 {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {

            // We will start changing alpha values of subject text when the top app bar is near to collapsed state.
            val alphaChangeThresholdFraction = 1f - MailDimens.MinOffsetForSubjectAlphaChange.dpToPx() /
                topAppBarState.heightOffsetLimit.absoluteValue
            val subjectAlpha = remember(topAppBarState.collapsedFraction) {
                calculateSubjectAlpha(topAppBarState.collapsedFraction, alphaChangeThresholdFraction)
            }

            CustomSingleLineTopAppBar(
                actions = actions,
                messageCount = messageCount,
                title = title,
                isStarred = isStarred,
                subjectLineAlpha = 1f - subjectAlpha,
                modifier = modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = -topAppBarState.heightOffset
                    }
            )

            // Subject text should not exceed the screen height otherwise it will not be possible
            // to scroll message(s). So the subject text will cover at most 60% of the screen height
            val maxSubjectHeightDp = with(LocalDensity.current) {
                LocalContext.current.resources.displayMetrics.heightPixels / density * 0.6f
            }

            SubjectHeader(
                modifier = modifier
                    .background(ProtonTheme.colors.backgroundNorm)
                    .onGloballyPositioned { layoutCoordinates ->
                        subjectHeaderSizeCallback(layoutCoordinates.size.height)
                    }
                    .heightIn(
                        min = MailDimens.SubjectHeaderMinHeight,
                        max = maxSubjectHeightDp.dp
                    ),
                subject = title,
                subjectTextAlpha = subjectAlpha
            )
        }
    }
}

// Calculate alpha value based on collapsedFraction which is a value between 0 and 1 based on the
// heightOffset and collapsedHeight. But we should not change alpha value until the subject header
// is near to fully collapsed state.
fun calculateSubjectAlpha(headerCollapsedFraction: Float, alphaChangeThresholdFraction: Float): Float {

    val newRatio = if (headerCollapsedFraction >= alphaChangeThresholdFraction) {
        1.0f - (headerCollapsedFraction - alphaChangeThresholdFraction) / (1.0f - alphaChangeThresholdFraction)
    } else {
        1.0f
    }

    return newRatio.coerceIn(0.0f, 1.0f)
}

@Composable
fun CustomSingleLineTopAppBar(
    modifier: Modifier = Modifier,
    actions: DetailScreenTopBar.Actions,
    messageCount: Int?,
    title: String,
    isStarred: Boolean?,
    subjectLineAlpha: Float
) {
    Row(
        modifier = modifier
            .testTag(DetailScreenTopBarTestTags.RootItem)
            .background(ProtonTheme.colors.backgroundNorm)
            .height(MailDimens.SingleLineTopAppBarHeight)
            .padding(ProtonDimens.ExtraSmallSpacing)
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            onClick = actions.onBackClick
        ) {
            Icon(
                modifier = Modifier
                    .testTag(DetailScreenTopBarTestTags.BackButton),
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = string.presentation_back),
                tint = ProtonTheme.colors.textNorm
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .wrapContentHeight(align = Alignment.CenterVertically)
        ) {
            messageCount?.let { count ->
                Text(
                    modifier = Modifier
                        .testTag(DetailScreenTopBarTestTags.MessageCount)
                        .fillMaxWidth(),
                    text = pluralStringResource(plurals.message_count_label_text, count, count),
                    style = ProtonTheme.typography.captionNorm,
                    textAlign = TextAlign.Center
                )
            }
            SelectionContainer(modifier = Modifier.testTag(DetailScreenTopBarTestTags.Subject)) {
                Text(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = subjectLineAlpha
                        }
                        .fillMaxWidth(),
                    maxLines = 1,
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    style = ProtonTheme.typography.defaultStrongNorm,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isStarred != null) {
            val onStarIconClick = {
                when (isStarred) {
                    true -> actions.onUnStarClick()
                    false -> actions.onStarClick()
                }
            }
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                onClick = onStarIconClick
            ) {
                Icon(
                    modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                    painter = getStarredIcon(isStarred),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = getStarredIconColor(isStarred)
                )
            }
        }
    }
}

@Composable
private fun SubjectHeader(
    subject: String,
    modifier: Modifier = Modifier,
    subjectTextAlpha: Float
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ProtonDimens.DefaultSpacing,
                vertical = ProtonDimens.SmallSpacing
            )
    ) {
        SelectionContainer(modifier = Modifier.testTag(DetailScreenTopBarTestTags.Subject)) {
            Text(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subjectTextAlpha
                    }
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = subject,
                overflow = TextOverflow.Ellipsis,
                style = ProtonTheme.typography.headlineNorm,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun getStarredIconColor(isStarred: Boolean) = if (isStarred) {
    colorResource(id = color.notification_warning)
} else {
    ProtonTheme.colors.textNorm
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
    ProtonTheme3 {
        val initialHeightOffset = if (preview.isExpanded) 0f else -Float.MAX_VALUE
        val state = rememberTopAppBarState(initialHeightOffset = initialHeightOffset)
        DetailScreenTopBar(
            title = preview.title,
            isStarred = preview.isStarred,
            messageCount = preview.messageCount,
            actions = DetailScreenTopBar.Actions.Empty,
            subjectHeaderSizeCallback = {},
            topAppBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = state).state
        )
    }
}

object DetailScreenTopBarTestTags {

    const val RootItem = "DetailScreenTopBarRootItem"
    const val BackButton = "BackButton"
    const val MessageCount = "MessageCount"
    const val Subject = "Subject"
}
