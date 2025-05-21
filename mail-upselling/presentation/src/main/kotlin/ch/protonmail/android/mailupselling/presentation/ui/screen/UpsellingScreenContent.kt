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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementsUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.backgroundGradient
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.backgroundGradientVariantB
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable.ComparisonTable
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.simplelist.UpsellingEntitlementsCheckedListLayout
import ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.simplelist.UpsellingEntitlementsListLayout
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingPlanButtonsFooter
import ch.protonmail.android.uicomponents.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.compose.theme.headlineSmallNorm

@Composable
internal fun UpsellingScreenContent(
    modifier: Modifier = Modifier,
    state: UpsellingScreenContentState.Data,
    actions: UpsellingScreen.Actions
) {
    val isStandalone = LocalEntryPointIsStandalone.current
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp <= MailDimens.NarrowScreenWidth.value

    val dynamicPlansModel = state.plans
    val scrollState = rememberScrollState()

    val shouldShowImage by remember {
        derivedStateOf { scrollState.value == 0 }
    }

    Box {
        if (shouldShowImage) {
            IconButton(
                modifier = Modifier
                    .padding(ProtonDimens.ExtraSmallSpacing)
                    .align(alignment = Alignment.TopStart)
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

        Column(
            modifier = modifier
                .thenIf(isStandalone) { Modifier.fillMaxHeight() }
                .verticalScroll(scrollState)
                .background(
                    when (state.plans.variant) {
                        DynamicPlansVariant.PromoB,
                        DynamicPlansVariant.SocialProof -> backgroundGradientVariantB
                        DynamicPlansVariant.Normal,
                        DynamicPlansVariant.PromoA -> backgroundGradient
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
            Spacer(modifier = Modifier.weight(UpsellingLayoutValues.topSpacingWeight))

            if (!isNarrowScreen) {
                Image(
                    modifier = Modifier
                        .padding(horizontal = ProtonDimens.DefaultSpacing)
                        .padding(top = ProtonDimens.DefaultSpacing),
                    painter = painterResource(id = dynamicPlansModel.icon.iconResId),
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }

            Text(
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .padding(top = ProtonDimens.DefaultSpacing),
                text = dynamicPlansModel.title.text.string(),
                style = if (isNarrowScreen) {
                    ProtonTheme.typography.headlineSmallNorm
                } else ProtonTheme.typography.headlineNorm,
                color = UpsellingLayoutValues.titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))

            when (dynamicPlansModel.description) {
                is DynamicPlanDescriptionUiModel.Simple -> Text(
                    modifier = Modifier
                        .padding(horizontal = ProtonDimens.DefaultSpacing)
                        .padding(top = ProtonDimens.SmallSpacing),
                    text = dynamicPlansModel.description.text.string(),
                    style = ProtonTheme.typography.body2Regular,
                    color = UpsellingLayoutValues.subtitleColor,
                    textAlign = TextAlign.Center
                )
                DynamicPlanDescriptionUiModel.SocialProof -> SocialProofDescription()
            }

            if (state.plans.variant == DynamicPlansVariant.SocialProof) {
                SocialProofBadges(modifier = Modifier.padding(vertical = ProtonDimens.DefaultSpacing))
            }

            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))

            when (state.plans.entitlements) {
                is PlanEntitlementsUiModel.ComparisonTableList ->
                    ComparisonTable(state.plans.variant == DynamicPlansVariant.PromoB, state.plans.entitlements)
                is PlanEntitlementsUiModel.SimpleList -> UpsellingEntitlementsListLayout(state.plans.entitlements)
                is PlanEntitlementsUiModel.CheckedSimpleList ->
                    UpsellingEntitlementsCheckedListLayout(state.plans.entitlements)
            }

            Spacer(modifier = Modifier.weight(UpsellingLayoutValues.bottomSpacingWeight))

            if (dynamicPlansModel.list is DynamicPlanInstanceListUiModel.Data) {
                UpsellingPlanButtonsFooter(
                    modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
                    dynamicPlansModel.list,
                    actions
                )
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        actions.onDisplayed()
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingScreenContentPreview_PromoA() {
    ProtonTheme3 {
        UpsellingScreenContent(
            state = UpsellingContentPreviewData.PromoA,
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
private fun UpsellingScreenContentPreview_PromoB() {
    ProtonTheme3 {
        UpsellingScreenContent(
            state = UpsellingContentPreviewData.PromoB,
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
    ProtonTheme3 {
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
