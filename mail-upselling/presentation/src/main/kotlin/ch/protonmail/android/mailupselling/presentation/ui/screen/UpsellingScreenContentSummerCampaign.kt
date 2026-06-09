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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.isNightMode
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.isIntroOffer
import ch.protonmail.android.mailupselling.presentation.extension.isPromotional
import ch.protonmail.android.mailupselling.presentation.extension.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.extension.toUpsellModalVariant
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
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

private val TextOverlayHeight = 120.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Don't use PaddingValues or it won't render the blur
@Composable
internal fun UpsellingScreenContentSummerCampaign(
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
                .background(UpsellingLayoutValues.SummerCampaign.backgroundGradient())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = footerHeight + ProtonDimens.Spacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(imageAlpha.value)
                ) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(id = plans.icon.iconResId),
                        contentDescription = NO_CONTENT_DESCRIPTION,
                        contentScale = ContentScale.FillWidth
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = ProtonDimens.Spacing.ExtraLarge,
                                top = ProtonDimens.Spacing.Massive + ProtonDimens.Spacing.Large
                            ),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Image(
                            modifier = Modifier.height(TextOverlayHeight),
                            painter = painterResource(id = R.drawable.summer_campaign_text),
                            contentDescription = NO_CONTENT_DESCRIPTION,
                            contentScale = ContentScale.FillHeight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

                when (state.plans.entitlements) {
                    is PlanUpgradeEntitlementsListUiModel.ComparisonTableList ->
                        ComparisonTable(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                                .padding(horizontal = ProtonDimens.Spacing.Large),
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
                    onClick = {
                        if (state.plans.list is PlanUpgradeInstanceListUiModel.Data) {
                            val upsellingTelemetryPayload = state.plans.list.longerCycle.toTelemetryPayload(
                                modalVariant = state.plans.list.variant.toUpsellModalVariant(),
                                upsellIsPromotional = state.plans.list.variant.isPromotional(),
                                isIntroOffer = state.plans.list.variant.isIntroOffer()
                            )
                            actions.onUpgradeCancelled(upsellingTelemetryPayload)
                        }
                        actions.onDismiss()
                    }
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

@Preview
@AdaptivePreviews
@Composable
private fun UpsellingScreenSummerCampaign_Promo() {
    SummerCampaignPreview(state = UpsellingContentPreviewData.SummerCampaign)
}

@Preview(showSystemUi = true)
@AdaptivePreviews
@Composable
private fun SummerCampaign_LongList() {
    val longEntitlements = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
        items = List(20) { i ->
            ComparisonTableEntitlementItemUiModel(
                title = TextUiModel.Text("Entitlement ${i + 1}"),
                freeValue = if (i % 3 == 0) {
                    ComparisonTableEntitlement.Free.Value(TextUiModel.Text("${i + 1}"))
                } else {
                    ComparisonTableEntitlement.Free.NotPresent
                },
                paidValue = if (i % 4 == 0) {
                    ComparisonTableEntitlement.Paid.Value(TextUiModel.Text("Unlimited"))
                } else {
                    ComparisonTableEntitlement.Paid.Present
                }
            )
        }
    )
    val state = UpsellingScreenContentState.Data(
        UpsellingContentPreviewData.SummerCampaign.plans.copy(entitlements = longEntitlements)
    )
    SummerCampaignPreview(state = state)
}

@Composable
private fun SummerCampaignPreview(state: UpsellingScreenContentState.Data) {
    val storeOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
        ProtonTheme {
            UpsellingScreenContentSummerCampaign(
                state = state,
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
}

@Preview(name = "en (default)")
@Preview(name = "cs", locale = "cs")
@Preview(name = "da", locale = "da")
@Preview(name = "de", locale = "de")
@Preview(name = "es-ES", locale = "es-rES")
@Preview(name = "es-419", locale = "b+es+419")
@Preview(name = "fr", locale = "fr")
@Preview(name = "it", locale = "it")
@Preview(name = "ja", locale = "ja")
@Preview(name = "ko", locale = "ko")
@Preview(name = "nl", locale = "nl")
@Preview(name = "pl", locale = "pl")
@Preview(name = "pt-BR", locale = "pt-rBR")
@Preview(name = "ru", locale = "ru")
private annotation class SummerCampaignLocalePreviews

@SummerCampaignLocalePreviews
@Composable
private fun SummerCampaign_Locales() {
    SummerCampaignPreview(state = UpsellingContentPreviewData.SummerCampaign)
}
