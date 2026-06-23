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

package ch.protonmail.android.mailcategory.presentation

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcategory.presentation.design.UnseenBadgeColor
import ch.protonmail.android.mailcategory.presentation.design.activeCategoryColor
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.sample.CategoryItemUiModelSample
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens

@Composable
fun CategoryPill(
    item: CategoryItemUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)

    val targetContentColor =
        if (item.isActive) Color.White else ProtonTheme.colors.textWeak

    val targetBackgroundColor =
        if (item.isActive) item.systemLabel.activeCategoryColor()
        else ProtonTheme.colors.interactionWeakNorm

    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        label = "CategoryPillContentColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        label = "CategoryPillBackgroundColor"
    )

    val showUnseenBadge = item.hasUnseen && !item.isActive

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = MailDimens.CategoryView.ItemHeight)
            .padding(
                start = ProtonDimens.Spacing.ModeratelyLarge,
                top = ProtonDimens.Spacing.MediumLight,
                end = ProtonDimens.Spacing.ModeratelyLarge,
                bottom = ProtonDimens.Spacing.MediumLight
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        CategoryIcon(
            iconRes = item.iconRes,
            tint = contentColor,
            showUnseenBadge = showUnseenBadge
        )

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.MediumLight))

        Text(
            text = stringResource(id = item.titleRes),
            style = ProtonTheme.typography.labelLarge.copy(color = contentColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryIcon(
    iconRes: Int,
    tint: Color,
    showUnseenBadge: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(MailDimens.CategoryView.IconSize)) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .matchParentSize()
                .then(if (showUnseenBadge) Modifier.unseenBadgeCutout() else Modifier)
        )

        if (showUnseenBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = UnseenDotRadius - UnseenBadgeCenterInsetEnd,
                        y = UnseenBadgeCenterInsetTop - UnseenDotRadius
                    )
                    .size(UnseenDotSize)
                    .clip(CircleShape)
                    .background(UnseenBadgeColor)
            )
        }
    }
}

private fun Modifier.unseenBadgeCutout(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawCircle(
            color = Color.Black,
            radius = UnseenDotWithBorderRadius.toPx(),
            center = Offset(
                x = size.width - UnseenBadgeCenterInsetEnd.toPx(),
                y = UnseenBadgeCenterInsetTop.toPx()
            ),
            blendMode = BlendMode.Clear
        )
    }

private val UnseenDotRadius = 3.dp
private val UnseenDotWithBorderRadius = 5.dp
private val UnseenDotSize = UnseenDotRadius * 2
private val UnseenBadgeCenterInsetEnd = 2.dp
private val UnseenBadgeCenterInsetTop = 3.dp

@Preview
@Composable
private fun ActiveCategoryPillPreview() {
    ProtonTheme {
        CategoryPill(
            item = CategoryItemUiModelSample.primary,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun InactiveCategoryPillNoUnseenPreview() {
    ProtonTheme {
        CategoryPill(
            item = CategoryItemUiModelSample.primary.copy(
                isActive = false,
                hasUnseen = false
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun InactiveCategoryPillHasUnseenPreview() {
    ProtonTheme {
        CategoryPill(
            item = CategoryItemUiModelSample.primary.copy(
                isActive = false,
                hasUnseen = true
            ),
            onClick = {}
        )
    }
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CategoryPillsWithUnseenBadgePreview() {
    ProtonTheme {
        Column(
            modifier = Modifier.padding(ProtonDimens.Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
        ) {
            CategoryItemUiModelSample.all.forEach { item ->
                CategoryPill(
                    item = item.copy(isActive = false, hasUnseen = true),
                    onClick = {}
                )
            }
        }
    }
}
