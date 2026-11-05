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

package ch.protonmail.android.mailbyoe.presentation.storage

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonSecondaryButton
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.modifier.drawHalo
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.viewmodel.hiltViewModelOrNull
import ch.protonmail.android.mailbyoe.presentation.R
import ch.protonmail.android.mailbyoe.presentation.byoeColors

/**
 * Tells the user, at the top of the mailbox, that their Gmail import is paused because they ran out of storage.
 * [onUpgrade] opens the upsell; "Not now" hides the banner.
 */
@Composable
fun ByoeStorageBanner(onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModelOrNull<ByoeStorageBannerViewModel>() ?: return
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isVisible = state is ByoeStorageBannerState.Visible

    LaunchedEffect(isVisible) { viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible)) }

    AnimatedVisibility(visible = isVisible, modifier = modifier) {
        ByoeStorageBannerContent(
            onUpgrade = {
                viewModel.submit(ByoeStorageBannerAction.UpgradeClicked)
                onUpgrade()
            },
            onNotNow = { viewModel.submit(ByoeStorageBannerAction.NotNowClicked) }
        )
    }
}

/**
 * Whether [ByoeStorageBanner] has anything to show, so a list only adds a slot for it then: an empty slot would
 * still take the list's spacing.
 */
@Composable
fun isByoeStorageBannerVisible(): Boolean {
    val viewModel = hiltViewModelOrNull<ByoeStorageBannerViewModel>() ?: return false
    val state by viewModel.state.collectAsStateWithLifecycle()
    return state is ByoeStorageBannerState.Visible
}

@Composable
private fun ByoeStorageBannerContent(
    onUpgrade: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = ProtonTheme.shapes.huge
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.Large)
            .drawHalo(
                color = ProtonTheme.colors.shadowWeak,
                blurRadiusPx = with(LocalDensity.current) { ShadowBlur.toPx() },
                shape = shape
            )
            .border(width = ProtonDimens.BorderSize.Default, color = byoeColors().bannerBorder, shape = shape)
            .background(color = ProtonTheme.colors.backgroundNorm, shape = shape)
            .padding(ProtonDimens.Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Standard),
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
        ) {
            ByoeStorageBannerIcon(modifier = Modifier.padding(top = IconTopPadding))
            Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Tiny)) {
                Text(
                    text = stringResource(R.string.byoe_storage_banner_title),
                    style = ProtonTheme.typography.bodyMedium.copy(fontSize = TitleFontSize),
                    fontWeight = FontWeight.SemiBold,
                    color = ProtonTheme.colors.textNorm
                )
                Text(
                    text = stringResource(R.string.byoe_storage_banner_body),
                    style = ProtonTheme.typography.bodySmall.copy(fontSize = BodyFontSize, lineHeight = BodyLineHeight),
                    color = ProtonTheme.colors.textNorm
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
            BannerButton(
                text = R.string.byoe_storage_banner_upgrade,
                onClick = onUpgrade,
                backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
                contentColor = ProtonTheme.colors.textAccent,
                fontWeight = FontWeight.SemiBold
            )
            BannerButton(
                text = R.string.byoe_storage_banner_not_now,
                onClick = onNotNow,
                backgroundColor = ProtonTheme.colors.interactionWeakNorm,
                contentColor = ProtonTheme.colors.textNorm,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RowScope.BannerButton(
    @StringRes text: Int,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    fontWeight: FontWeight
) {
    ProtonSecondaryButton(
        onClick = onClick,
        shape = ProtonTheme.shapes.huge,
        colors = ButtonDefaults.protonSecondaryButtonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = ProtonDimens.Spacing.Large, vertical = ProtonDimens.Spacing.Medium),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = stringResource(text),
            style = ProtonTheme.typography.bodySmall.copy(fontSize = BodyFontSize, lineHeight = BodyLineHeight),
            fontWeight = fontWeight
        )
    }
}

/**
 * An import tray with a warning badge, standing in for the design's iOS symbol, which has no Proton icon. The badge
 * is drawn over a slightly larger copy in the banner's background, which cuts it out of the tray.
 */
@Composable
private fun ByoeStorageBannerIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(IconSize)) {
        Icon(
            painter = painterResource(R.drawable.ic_proton_arrow_down_to_square),
            contentDescription = null,
            tint = ProtonTheme.colors.iconNorm,
            modifier = Modifier.size(IconSize)
        )
        Icon(
            painter = painterResource(R.drawable.ic_proton_exclamation_triangle_filled),
            contentDescription = null,
            tint = ProtonTheme.colors.backgroundNorm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = BadgeOffset, y = BadgeOffset)
                .size(BadgeCutoutSize)
        )
        Icon(
            painter = painterResource(R.drawable.ic_proton_exclamation_triangle_filled),
            contentDescription = null,
            tint = ProtonTheme.colors.iconNorm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = BadgeOffset - BadgeCutoutWidth, y = BadgeOffset - BadgeCutoutWidth)
                .size(BadgeSize)
        )
    }
}

private val IconSize = 20.dp
private val BadgeSize = 10.dp
private val BadgeCutoutWidth = 1.5.dp
private val BadgeCutoutSize = BadgeSize + BadgeCutoutWidth * 2
private val BadgeOffset = 3.dp
private val TitleFontSize = 15.sp
private val BodyFontSize = 13.sp

// Aligns the glyph with the middle of the title's first line rather than with its top.
private val IconTopPadding = 2.dp
private val BodyLineHeight = 18.sp
private val ShadowBlur = 6.dp

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ByoeStorageBannerPreview() {
    ProtonTheme {
        ByoeStorageBannerContent(onUpgrade = {}, onNotNow = {})
    }
}
