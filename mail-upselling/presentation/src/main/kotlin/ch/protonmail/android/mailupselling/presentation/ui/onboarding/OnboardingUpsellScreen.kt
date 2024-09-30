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

package ch.protonmail.android.mailupselling.presentation.ui.onboarding

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellButtonsUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanSwitcherUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.viewmodel.OnboardingUpsellViewModel
import coil.compose.AsyncImage
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionStrongUnspecified
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallStrongInverted
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
fun OnboardingUpsellScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingUpsellViewModel = hiltViewModel(),
    exitScreen: () -> Unit
) {
    when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
        is OnboardingUpsellState.Loading -> ProtonCenteredProgress()
        is OnboardingUpsellState.Data -> OnboardingUpsellScreenContent(
            modifier = modifier,
            state = state
        )
        is OnboardingUpsellState.Error -> OnboardingUpsellError(state, exitScreen)
    }
}

@Composable
private fun OnboardingUpsellScreenContent(modifier: Modifier = Modifier, state: OnboardingUpsellState.Data) {
    val selectedPlansType = remember { mutableStateOf(PlansType.Annual) }
    val selectedPlan = remember { mutableStateOf(state.planUiModels.annualPlans[0].title) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(MailDimens.ExtraLargeSpacing),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.upselling_onboarding_title),
                style = ProtonTheme.typography.defaultStrongNorm
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                PlanSwitcher(
                    planSwitcherUiModel = state.planSwitcherUiModel,
                    selectedPlansType = selectedPlansType.value,
                    onSwitch = { selectedPlansType.value = it }
                )
                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            }

            val plans = when (selectedPlansType.value) {
                PlansType.Monthly -> state.planUiModels.monthlyPlans
                PlansType.Annual -> state.planUiModels.annualPlans
            }

            itemsIndexed(plans) { index, item ->
                PlanCard(
                    plan = item,
                    isSelected = item.title == selectedPlan.value,
                    isBestValue = index == 0,
                    numberOfEntitlementsToShow = MAX_ENTITLEMENTS_TO_SHOW - index,
                    onClick = { selectedPlan.value = item.title }
                )
                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            }
        }

        UpsellButtons(
            selectedPlansType = selectedPlansType.value,
            selectedPlan = selectedPlan.value,
            buttonsUiModel = state.buttonsUiModel
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun PlanSwitcher(
    modifier: Modifier = Modifier,
    planSwitcherUiModel: OnboardingUpsellPlanSwitcherUiModel,
    selectedPlansType: PlansType,
    onSwitch: (PlansType) -> Unit
) {
    Box(modifier = modifier.height(MailDimens.PlanSwitcherHeight)) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.75f)
                .height(MailDimens.ExtraLargeSpacing)
                .background(color = ProtonTheme.colors.backgroundDeep, shape = ProtonTheme.shapes.large)
        ) {
            PlanSwitcherItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSwitch(PlansType.Monthly) },
                text = R.string.upselling_onboarding_switcher_monthly,
                isSelected = selectedPlansType == PlansType.Monthly
            )
            PlanSwitcherItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSwitch(PlansType.Annual) },
                text = R.string.upselling_onboarding_switcher_annual,
                isSelected = selectedPlansType == PlansType.Annual
            )
        }
        planSwitcherUiModel.discount?.let {
            PlanSwitcherLabel(modifier = Modifier.align(Alignment.TopEnd), text = planSwitcherUiModel.discount)
        }
    }
}

@Composable
private fun PlanSwitcherLabel(modifier: Modifier, text: TextUiModel) {
    Text(
        modifier = modifier
            .background(color = ProtonTheme.colors.interactionNorm, shape = ProtonTheme.shapes.large)
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
        text = text.string(),
        style = ProtonTheme.typography.captionStrongUnspecified.copy(color = ProtonTheme.colors.textInverted)
    )
}

@Composable
private fun PlanSwitcherItem(
    modifier: Modifier,
    @StringRes text: Int,
    isSelected: Boolean
) {
    val selectedBackgroundModifier = if (isSelected) {
        Modifier.background(color = ProtonTheme.colors.backgroundNorm, shape = ProtonTheme.shapes.large)
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(ProtonDimens.ExtraSmallSpacing)
            .then(selectedBackgroundModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = text),
            style = ProtonTheme.typography.defaultSmallStrongNorm
        )
    }
}

@Composable
private fun PlanCard(
    modifier: Modifier = Modifier,
    plan: OnboardingUpsellPlanUiModel,
    isSelected: Boolean,
    isBestValue: Boolean,
    numberOfEntitlementsToShow: Int,
    onClick: () -> Unit
) {
    val borderWidth = if (isSelected) MailDimens.OnboardingUpsellBestValueBorder else MailDimens.DefaultBorder
    val borderColor = if (isSelected) ProtonTheme.colors.interactionNorm else ProtonTheme.colors.separatorNorm

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = ProtonTheme.shapes.large)
            .selectable(selected = isSelected, onClick = onClick),
        shape = ProtonTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors().copy(containerColor = ProtonTheme.colors.backgroundNorm)
    ) {
        if (isBestValue) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProtonTheme.colors.interactionNorm)
                    .padding(ProtonDimens.SmallSpacing),
                text = stringResource(id = R.string.upselling_onboarding_best_value),
                textAlign = TextAlign.Center,
                style = ProtonTheme.typography.defaultSmallStrongInverted
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing)
        ) {
            PlanCheckmark(isSelected = isSelected)
            PlanNameAndPrice(plan = plan)
            Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            PlanEntitlements(plan = plan, numberOfEntitlementsToShow = numberOfEntitlementsToShow)
        }
    }
}

@Composable
private fun PlanCheckmark(modifier: Modifier = Modifier, isSelected: Boolean) {
    if (isSelected) {
        Box(
            modifier = modifier
                .size(ProtonDimens.DefaultIconSize)
                .background(color = ProtonTheme.colors.interactionNorm, shape = CircleShape)
                .padding(ProtonDimens.ExtraSmallSpacing)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_checkmark),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconInverted
            )
        }
    } else {
        Icon(
            modifier = modifier.size(ProtonDimens.DefaultIconSize),
            painter = painterResource(id = R.drawable.ic_proton_empty_circle),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconDisabled
        )
    }
}

@Composable
private fun PlanNameAndPrice(modifier: Modifier = Modifier, plan: OnboardingUpsellPlanUiModel) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            Text(
                text = plan.title,
                style = ProtonTheme.typography.headlineNorm.copy(color = ProtonTheme.colors.brandDarken40)
            )
        }
        if (plan.monthlyPriceWithDiscount != null) {
            Column {
                if (plan.monthlyPrice != null) {
                    Text(
                        modifier = Modifier.align(Alignment.End),
                        text = "${plan.currency} ${plan.monthlyPrice.string()}",
                        style = ProtonTheme.typography.defaultSmallWeak.copy(
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${plan.currency} ${plan.monthlyPriceWithDiscount.string()}",
                        style = ProtonTheme.typography.headlineNorm.copy(fontWeight = FontWeight.W700)
                    )
                    Text(
                        text = stringResource(id = R.string.upselling_onboarding_month),
                        style = ProtonTheme.typography.defaultSmallWeak
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanEntitlements(
    modifier: Modifier = Modifier,
    plan: OnboardingUpsellPlanUiModel,
    numberOfEntitlementsToShow: Int
) {
    val showAllEntitlements = remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        plan.entitlements.forEachIndexed { index, item ->
            if (index < numberOfEntitlementsToShow || showAllEntitlements.value) {
                PlanEntitlement(entitlementUiModel = item)
                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            }
        }

        val numberOfEntitlementsNotShown = plan.entitlements.size - numberOfEntitlementsToShow
        if (numberOfEntitlementsNotShown != 0 && !showAllEntitlements.value) {
            MorePlanEntitlements(
                numberOfEntitlementsNotShown = numberOfEntitlementsNotShown,
                onClick = { showAllEntitlements.value = true }
            )
        }
    }
}

@Composable
private fun PlanEntitlement(modifier: Modifier = Modifier, entitlementUiModel: DynamicEntitlementUiModel) {
    val imageModel = when (entitlementUiModel) {
        is DynamicEntitlementUiModel.Default -> entitlementUiModel.remoteResource
        is DynamicEntitlementUiModel.Overridden -> entitlementUiModel.localResource
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            modifier = Modifier
                .size(ProtonDimens.LargeSpacing)
                .background(color = ProtonTheme.colors.backgroundSecondary, shape = CircleShape)
                .padding(ProtonDimens.SmallSpacing),
            placeholder = painterResource(R.drawable.ic_logo_mail_mono),
            model = imageModel,
            contentDescription = NO_CONTENT_DESCRIPTION,
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
        Text(
            text = entitlementUiModel.text.string(),
            style = ProtonTheme.typography.defaultSmallNorm
        )
    }
}

@Composable
private fun MorePlanEntitlements(
    modifier: Modifier = Modifier,
    numberOfEntitlementsNotShown: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = pluralStringResource(
                id = R.plurals.upselling_onboarding_more_features,
                count = numberOfEntitlementsNotShown,
                numberOfEntitlementsNotShown
            ),
            style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(color = ProtonTheme.colors.textAccent)
        )
        Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
        Icon(
            modifier = Modifier.size(ProtonDimens.SmallIconSize),
            painter = painterResource(id = R.drawable.ic_proton_chevron_down),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconAccent
        )
    }
}

@Composable
private fun UpsellButtons(
    modifier: Modifier = Modifier,
    selectedPlansType: PlansType,
    selectedPlan: String,
    buttonsUiModel: OnboardingUpsellButtonsUiModel
) {
    val billingMessage = buttonsUiModel.billingMessage[selectedPlan]?.let {
        when (selectedPlansType) {
            PlansType.Monthly -> it.monthlyBillingMessage
            PlansType.Annual -> it.annualBillingMessage
        }
    } ?: TextUiModel.Text(EMPTY_STRING)

    val getButtonLabel = buttonsUiModel.getButtonLabel[selectedPlan]
        ?: TextUiModel.TextResWithArgs(R.string.upselling_onboarding_get_plan, listOf(PROTON_FREE))

    MailDivider()
    Column(
        modifier = modifier.padding(horizontal = ProtonDimens.LargeSpacing, vertical = ProtonDimens.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = billingMessage.string(),
            style = ProtonTheme.typography.overlineRegular.copy(color = ProtonTheme.colors.brandDarken40)
        )
        Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
        ProtonSolidButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.OnboardingUpsellButtonHeight),
            onClick = {}
        ) {
            Text(
                text = getButtonLabel.string(),
                style = ProtonTheme.typography.defaultSmallStrongInverted
            )
        }
        ProtonTextButton(onClick = {}) {
            Text(
                text = stringResource(id = R.string.upselling_onboarding_continue_with_proton_free),
                style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(
                    color = ProtonTheme.colors.brandDarken40
                )
            )
        }
    }
}

@Composable
private fun OnboardingUpsellError(state: OnboardingUpsellState.Error, exitScreen: () -> Unit) {
    val context = LocalContext.current

    ConsumableTextEffect(effect = state.error) { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        exitScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingUpsellScreenContentPreview() {
    ProtonTheme {
        OnboardingUpsellScreenContent(
            state = OnboardingUpsellState.Data(
                planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel
            )
        )
    }
}

private const val MAX_ENTITLEMENTS_TO_SHOW = 4
const val PROTON_FREE = "Proton Free"

enum class PlansType { Monthly, Annual }
