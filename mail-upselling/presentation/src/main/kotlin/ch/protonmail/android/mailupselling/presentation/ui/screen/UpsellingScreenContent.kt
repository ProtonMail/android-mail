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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.backgroundGradient
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable.ComparisonTable
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.simplelist.UpsellingEntitlementsListLayout
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingPlanButtonsFooter
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Don't use PaddingValues or it won't render the blur
@Composable
internal fun UpsellingScreenContent(
    modifier: Modifier = Modifier,
    state: UpsellingScreenContentState.Data,
    actions: UpsellingScreen.Actions
) {
    val plans = state.plans
    val scrollState = rememberScrollState()

    var footerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val hazeState = rememberHazeState()

    val shouldShowImage by remember {
        derivedStateOf { scrollState.value == 0 }
    }

    val scrollProgress = remember {
        derivedStateOf {
            val maxScroll = 100f
            (scrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
        }
    }

    val imageScale = remember {
        derivedStateOf {
            1f - scrollProgress.value * 0.2f
        }
    }

    val imageAlpha = remember {
        derivedStateOf {
            1f - scrollProgress.value * 0.5f
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (plans.list is PlanUpgradeInstanceListUiModel.Data) {
                val configuration = LocalConfiguration.current
                val screenHeight = configuration.screenHeightDp.dp
                val maxHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    screenHeight / 3
                } else {
                    screenHeight / 2
                }

                Column(
                    modifier = Modifier
                        .onSizeChanged { size ->
                            footerHeight = with(density) { size.height.toDp() }
                        }
                        .background(ProtonTheme.colors.brandPlus20)
                        .hazeEffect(state = hazeState)
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    UpsellingPlanButtonsFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            ),
                        plans = plans.list,
                        actions = actions
                    )
                }
            }
        }
    ) { _ ->
        Box(
            modifier = modifier
                .hazeSource(state = hazeState)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(backgroundGradient)
                    .padding(bottom = footerHeight + ProtonDimens.Spacing.Large)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))
                Spacer(modifier = Modifier.weight(UpsellingLayoutValues.topSpacingWeight))

                Image(
                    modifier = Modifier
                        .height(UpsellingLayoutValues.imageHeight)
                        .padding(horizontal = ProtonDimens.Spacing.Large)
                        .padding(top = ProtonDimens.Spacing.Large)
                        .scale(imageScale.value)
                        .alpha(imageAlpha.value),
                    painter = painterResource(id = plans.icon.iconResId),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    contentScale = ContentScale.Fit
                )

                Text(
                    modifier = Modifier
                        .padding(horizontal = ProtonDimens.Spacing.Large)
                        .padding(top = ProtonDimens.Spacing.Large),
                    text = plans.title.text.string(),
                    style = ProtonTheme.typography.titleLargeNorm,
                    fontWeight = FontWeight.Bold,
                    color = UpsellingLayoutValues.titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))

                when (plans.description) {
                    is PlanUpgradeDescriptionUiModel.Simple -> Text(
                        modifier = Modifier
                            .padding(horizontal = ProtonDimens.Spacing.Large)
                            .padding(top = ProtonDimens.Spacing.Small),
                        text = plans.description.text.string(),
                        style = ProtonTheme.typography.bodyLargeNorm,
                        fontWeight = FontWeight.Normal,
                        color = UpsellingLayoutValues.subtitleColor,
                        textAlign = TextAlign.Center
                    )

                    PlanUpgradeDescriptionUiModel.SocialProof -> SocialProofDescription()
                }

                if (state.plans.variant == PlanUpgradeVariant.SocialProof) {
                    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
                    SocialProofBadges(modifier = Modifier.padding(vertical = ProtonDimens.Spacing.Standard))
                }

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                when (state.plans.entitlements) {
                    is PlanUpgradeEntitlementsListUiModel.ComparisonTableList ->
                        ComparisonTable(state.plans.entitlements, state.plans.variant)

                    is PlanUpgradeEntitlementsListUiModel.SimpleList ->
                        UpsellingEntitlementsListLayout(state.plans.entitlements)
                }

                Spacer(modifier = Modifier.weight(UpsellingLayoutValues.bottomSpacingWeight))
            }

            if (shouldShowImage) {
                IconButton(
                    modifier = Modifier
                        .padding(ProtonDimens.Spacing.Tiny)
                        .padding(
                            WindowInsets.safeDrawing
                                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                                .asPaddingValues()
                        )
                        .align(alignment = Alignment.TopEnd)
                        .zIndex(1f),
                    onClick = actions.onDismiss
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(UpsellingLayoutValues.closeButtonSize)
                            .background(
                                color = UpsellingLayoutValues.closeButtonBackgroundColor,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            tint = UpsellingLayoutValues.closeButtonColor,
                            contentDescription = stringResource(R.string.upselling_close_button_content_description)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        actions.onDisplayed()
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingScreenContentPreview_Promo() {
    ProtonTheme {
        UpsellingScreenContent(
            state = UpsellingContentPreviewData.Base,
            actions = UpsellingScreen.Actions(
                onDisplayed = {},
                onDismiss = {},
                onError = {},
                onUpgradeAttempt = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {}
            )
        )
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingContentPreview_SocialProof() {
    ProtonTheme {
        UpsellingScreenContent(
            state = UpsellingContentPreviewData.SocialProof,
            actions = UpsellingScreen.Actions(
                onDisplayed = {},
                onDismiss = {},
                onError = {},
                onUpgradeAttempt = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {}
            )
        )
    }
}
