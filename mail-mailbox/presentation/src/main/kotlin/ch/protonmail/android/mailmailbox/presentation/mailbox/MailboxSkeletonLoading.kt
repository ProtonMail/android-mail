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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens.MailboxSkeletonRowHeight

@Composable
fun MailboxSkeletonLoading(modifier: Modifier = Modifier) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val rowHeight = MailboxSkeletonRowHeight
    val rowCount = remember(screenHeight, rowHeight) {
        (screenHeight / rowHeight).toInt() + 1
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mailbox_skeleton_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_TARGET_VALUE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_ANIMATION_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val baseColor = ProtonTheme.colors.shade20
    val highlightColor = ProtonTheme.colors.shade10

    Column(
        modifier = modifier
            .padding(top = ProtonDimens.Spacing.Large)
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false),
        verticalArrangement = spacedBy(ProtonDimens.Spacing.ExtraLarge),
        horizontalAlignment = Alignment.Start
    ) {
        repeat(rowCount) {
            SkeletonRow(
                shimmerOffset = shimmerOffset,
                baseColor = baseColor,
                highlightColor = highlightColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = ProtonDimens.Spacing.Large)
            )
        }
    }
}

@Composable
private fun SkeletonRow(
    shimmerOffset: Float,
    baseColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(shimmerOffset - SHIMMER_HIGHLIGHT_WIDTH, 0f),
        end = Offset(shimmerOffset, 0f)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerPlaceholder(
            brush = shimmerBrush,
            modifier = Modifier
                .size(MailDimens.AvatarSize)
                .clip(RoundedCornerShape(ShimmerCornerRadius))
        )

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Large))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = spacedBy(ProtonDimens.Spacing.Compact)
        ) {
            ShimmerPlaceholder(
                brush = shimmerBrush,
                modifier = Modifier.size(width = PrimaryTextWidth, height = TextHeight)
            )
            ShimmerPlaceholder(
                brush = shimmerBrush,
                modifier = Modifier.size(width = SecondaryTextWidth, height = TextHeight)
            )
        }

        ShimmerPlaceholder(
            brush = shimmerBrush,
            modifier = Modifier
                .align(Alignment.Bottom)
                .size(width = StarSize, height = TextHeight)
        )
    }
}

@Composable
private fun ShimmerPlaceholder(brush: Brush, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ShimmerCornerRadius))
            .background(brush)
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewMailboxSkeletonLoading() {
    ProtonTheme {
        MailboxSkeletonLoading()
    }
}

private val ShimmerCornerRadius = 12.dp
private val PrimaryTextWidth = 140.dp
private val SecondaryTextWidth = 220.dp
private val TextHeight = 16.dp
private val StarSize = 16.dp

private const val SHIMMER_ANIMATION_DURATION = 1200
private const val SHIMMER_TARGET_VALUE = 1000f
private const val SHIMMER_HIGHLIGHT_WIDTH = 200f
