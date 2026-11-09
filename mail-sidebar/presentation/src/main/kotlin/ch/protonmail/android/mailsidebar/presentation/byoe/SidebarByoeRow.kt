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

package ch.protonmail.android.mailsidebar.presentation.byoe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.viewmodel.hiltViewModelOrNull
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.presentation.sidebar.ByoeSidebarBannerAction
import ch.protonmail.android.mailbyoe.presentation.sidebar.ByoeSidebarBannerState
import ch.protonmail.android.mailbyoe.presentation.sidebar.ByoeSidebarBannerViewModel
import ch.protonmail.android.mailsidebar.presentation.R

/**
 * Temporary card that invites the user to use Gmail in Proton Mail, shown for a while after the BYOE onboarding
 * page or spotlight. The SDK decides when; it is asked again each time the sidebar opens.
 */
@Composable
fun SidebarByoeRow(
    isSidebarOpen: Boolean,
    onClick: (ByoeTreatment) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = hiltViewModelOrNull<ByoeSidebarBannerViewModel>()
    val state = viewModel?.state?.collectAsStateWithLifecycle()?.value ?: ByoeSidebarBannerState.Hidden

    LaunchedEffect(isSidebarOpen) {
        if (isSidebarOpen) viewModel?.submit(ByoeSidebarBannerAction.SidebarOpened)
    }

    // Set min to 1.dp to allow lazyColumns to render it.
    Box(modifier = modifier.heightIn(min = 1.dp)) {
        AnimatedVisibility(
            visible = state is ByoeSidebarBannerState.Visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SidebarByoeCard(
                onConnect = {
                    viewModel?.submit(ByoeSidebarBannerAction.ConnectClicked)
                    (state as? ByoeSidebarBannerState.Visible)?.let { onClick(it.treatment) }
                },
                onDismiss = { viewModel?.submit(ByoeSidebarBannerAction.CloseClicked) },
                modifier = Modifier.padding(
                    horizontal = ProtonDimens.Spacing.Large,
                    vertical = ProtonDimens.Spacing.Standard
                )
            )
        }
    }
}

@Composable
private fun SidebarByoeCard(
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBackgroundBrush(ProtonTheme.colors.brandNorm))
            .background(Color.White.copy(alpha = WHITE_WASH_ALPHA))
            .border(width = ProtonDimens.Spacing.ExtraTiny, color = CardBorder, shape = shape)
            .innerShadow(
                shape = shape,
                shadow = Shadow(
                    radius = ProtonDimens.Spacing.Standard,
                    color = CardBorder.copy(alpha = INNER_GLOW_ALPHA)
                )
            )
            .padding(ProtonDimens.Spacing.ModeratelyLarge),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Tiny)) {
                Text(
                    text = stringResource(R.string.drawer_byoe_title),
                    style = ProtonTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.drawer_byoe_subtitle),
                    style = ProtonTheme.typography.bodyMedium,
                    color = SubtitleText
                )
            }
            SidebarByoeGetStartedButton(onClick = onConnect)
        }
        Box(
            modifier = Modifier
                .size(ProtonDimens.IconSize.Default)
                .clip(CircleShape)
                .background(DismissBackground)
                .clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_proton_cross_small),
                contentDescription = stringResource(R.string.drawer_byoe_dismiss),
                tint = ActionText,
                modifier = Modifier.size(ProtonDimens.IconSize.Small)
            )
        }
    }
}

@Composable
private fun SidebarByoeGetStartedButton(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.drawer_byoe_get_started),
        style = ProtonTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = ActionText,
        modifier = Modifier
            .clip(CircleShape)
            .background(ButtonBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ProtonDimens.Spacing.ModeratelyLarge, vertical = ProtonDimens.Spacing.Standard)
    )
}

// A faint brand glow over the sidebar, as in the design.
private fun cardBackgroundBrush(brand: Color): Brush = Brush.radialGradient(
    colors = listOf(brand.copy(alpha = GLOW_ALPHA), Color.Transparent)
)

private const val GLOW_ALPHA = 0.1f
private const val WHITE_WASH_ALPHA = 0.04f
private const val INNER_GLOW_ALPHA = 0.3f

// The design's colors; the sidebar is dark in both themes, and they have no design system token there.
private val CardBorder = Color(0xFF796DFB)
private val TitleText = Color.White
private val SubtitleText = Color(0xFFB9BEFA).copy(alpha = 0.9f)
private val ActionText = Color(0xFFEAE5FF)
private val ButtonBackground = Color(0xFFB9BEFA).copy(alpha = 0.12f)
private val DismissBackground = Color(0xFFD4D9FB).copy(alpha = 0.1f)

@Preview
@Composable
private fun SidebarByoeCardPreview() {
    ProtonTheme {
        Box(modifier = Modifier.background(ProtonTheme.colors.sidebarBackground)) {
            SidebarByoeCard(
                onConnect = {},
                onDismiss = {},
                modifier = Modifier.padding(ProtonDimens.Spacing.Large)
            )
        }
    }
}
