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
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.isNightMode
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable.ComparisonTable
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.simplelist.UpsellingEntitlementsListLayout
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingPlanButtonsFooter
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Don't use PaddingValues or it won't render the blur
@Composable
internal fun UpsellingScreenContentSpringPromo(
    modifier: Modifier = Modifier,
    state: UpsellingScreenContentState.Data,
    actions: UpsellingScreen.Actions
) {
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { original?.let { activity.requestedOrientation = it } }
    }

    val nightMode = isNightMode()
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
                val maxHeight = screenHeight / 2

                Column(
                    modifier = Modifier
                        .onSizeChanged { size ->
                            footerHeight = with(density) { size.height.toDp() }
                        }
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                tint = HazeTint(if (nightMode) Color.Transparent else Color.White.copy(alpha = 0.3f))
                            )
                        )
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
                .background(UpsellingLayoutValues.SpringPromo.backgroundGradient())
        ) {
            val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
            val navBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

            // Top clouds - offset upward to cover the status bar area
            Image(
                painter = painterResource(
                    if (nightMode) R.drawable.spring_promo_bg_cloud_dark1
                    else R.drawable.spring_promo_bg_cloud1
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -statusBarTop),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter
            )

            // Bottom clouds (light mode only)
            if (!nightMode) {
                Image(
                    painter = painterResource(R.drawable.spring_promo_bg_cloud2),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = navBarBottom),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.BottomCenter
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = footerHeight + ProtonDimens.Spacing.Large)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge * 2))
                Image(
                    modifier = Modifier
                        .padding(horizontal = ProtonDimens.Spacing.Large)
                        .padding(top = ProtonDimens.Spacing.Large)
                        .alpha(imageAlpha.value),
                    painter = painterResource(id = plans.icon.iconResId),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

                when (state.plans.entitlements) {
                    is PlanUpgradeEntitlementsListUiModel.ComparisonTableList ->
                        ComparisonTable(
                            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large),
                            entitlementsUiModel = state.plans.entitlements,
                            variant = state.plans.variant
                        )

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
                                color = if (nightMode) UpsellingLayoutValues.closeButtonBackgroundColor
                                else UpsellingLayoutValues.closeButtonLightBackgroundColor,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            tint = if (nightMode) Color.White else Color.Black,
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
private fun UpsellingScreenContentSpringPromo_Promo() {
    ProtonTheme {
        UpsellingScreenContentSpringPromo(
            state = UpsellingContentPreviewData.SpringPromo,
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
