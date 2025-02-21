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

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import ch.protonmail.android.mailupselling.presentation.model.UserIdUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellButtonsUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellOperation.Action
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanSwitcherUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPlanUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellPriceUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingAutoRenewGenericPolicyText
import ch.protonmail.android.mailupselling.presentation.viewmodel.OnboardingUpsellViewModel
import coil.compose.AsyncImage
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionStrongUnspecified
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotEmpty

@Composable
fun OnboardingUpsellScreen(
    modifier: Modifier = Modifier,
    exitScreen: () -> Unit,
    viewModel: OnboardingUpsellViewModel = hiltViewModel()
) {
    val paymentButtonActions = remember {
        OnboardingPayButton.Actions.Empty.copy(
            onUpgradeAttempt = { viewModel.submit(Action.TrackEvent.UpgradeAttempt(it)) },
            onUpgradeCancelled = { viewModel.submit(Action.TrackEvent.UpgradeCancelled(it)) },
            onUpgradeErrored = { viewModel.submit(Action.TrackEvent.UpgradeErrored(it)) },
            onSuccess = { viewModel.submit(Action.TrackEvent.UpgradeSuccess(it)) }
        )
    }

    when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
        is OnboardingUpsellState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
        is OnboardingUpsellState.Data -> OnboardingUpsellScreenContent(
            modifier = modifier,
            state = state,
            exitScreen = exitScreen,
            onPlanSelected = { plansType, planName ->
                viewModel.submit(Action.PlanSelected(plansType, planName))
            },
            paymentButtonActions
        )

        is OnboardingUpsellState.Error -> OnboardingUpsellError(state, exitScreen)
        is OnboardingUpsellState.UnsupportedFlow -> exitScreen()
    }
}

@Composable
private fun OnboardingUpsellScreenContent(
    modifier: Modifier = Modifier,
    state: OnboardingUpsellState.Data,
    exitScreen: () -> Unit,
    onPlanSelected: (PlansType, String) -> Unit,
    paymentButtonActions: OnboardingPayButton.Actions
) {
    val selectedPlansType = remember { mutableStateOf(PlansType.Annual) }
    val selectedPlan = remember { mutableStateOf(state.planUiModels.annualPlans[0].title) }

    LaunchedEffect(Unit) {
        onPlanSelected(selectedPlansType.value, selectedPlan.value)
    }

    BackHandler {
        // This screen should not be closed when pressing back
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm),
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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            PlanSwitcher(
                planSwitcherUiModel = state.planSwitcherUiModel,
                selectedPlansType = selectedPlansType.value,
                onSwitch = {
                    selectedPlansType.value = it
                    onPlanSelected(selectedPlansType.value, selectedPlan.value)
                }
            )
            Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))

            val plans = when (selectedPlansType.value) {
                PlansType.Monthly -> state.planUiModels.monthlyPlans
                PlansType.Annual -> state.planUiModels.annualPlans
            }

            plans.forEachIndexed { index, item ->
                PlanCard(
                    plan = item,
                    isSelected = item.title == selectedPlan.value,
                    isBestValue = index == 0,
                    numberOfEntitlementsToShow = MAX_ENTITLEMENTS_TO_SHOW - index,
                    onClick = {
                        selectedPlan.value = item.title
                        onPlanSelected(selectedPlansType.value, selectedPlan.value)
                    }
                )
                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            }
        }

        UpsellButtons(
            selectedPlan = selectedPlan.value,
            buttonsUiModel = state.buttonsUiModel,
            selectedPlanUiModel = state.selectedPayButtonPlanUiModel,
            onContinueWithProtonFree = exitScreen,
            exitScreen = exitScreen,
            paymentButtonActions = paymentButtonActions
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
            val monthlyInteractionSource = remember { MutableInteractionSource() }
            val yearlyInteractionSource = remember { MutableInteractionSource() }

            PlanSwitcherItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = monthlyInteractionSource,
                        indication = null
                    ) { onSwitch(PlansType.Monthly) },
                text = R.string.upselling_onboarding_switcher_monthly,
                isSelected = selectedPlansType == PlansType.Monthly
            )
            PlanSwitcherItem(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = yearlyInteractionSource,
                        indication = null
                    ) { onSwitch(PlansType.Annual) },
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
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .background(color = ProtonTheme.colors.interactionNorm, shape = ProtonTheme.shapes.large)
                .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
            text = text.string(),
            style = ProtonTheme.typography.captionStrongUnspecified.copy(color = Color.White)
        )
        Spacer(Modifier.width(ProtonDimens.SmallSpacing))
    }
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
    val interactionSource = remember { MutableInteractionSource() }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = ProtonTheme.shapes.large)
            .selectable(
                interactionSource = interactionSource,
                indication = null,
                selected = isSelected,
                onClick = onClick
            ),
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
                style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(color = Color.White)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing)
        ) {
            PlanCheckmark(isSelected = isSelected)
            PlanNameAndPrice(plan = plan)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProtonDimens.DefaultSpacing)
            )
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
                tint = Color.White
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
    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = ProtonDimens.DefaultSpacing)
        ) {
            Text(
                modifier = Modifier.padding(end = ProtonDimens.ExtraSmallSpacing),
                text = plan.title,
                style = ProtonTheme.typography.subheadline,
                color = ProtonTheme.colors.textNorm
            )
        }

        val priceUiModel = plan.priceUiModel as? OnboardingUpsellPriceUiModel.Paid ?: return@Row

        Column(modifier = Modifier.align(Alignment.Bottom)) {
            if (priceUiModel.originalAmount != null) {
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = "${priceUiModel.currency} ${priceUiModel.originalAmount.string()}",
                    style = ProtonTheme.typography.defaultSmallWeak.copy(
                        textDecoration = TextDecoration.LineThrough
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${priceUiModel.currency} ${priceUiModel.amount.string()}",
                    style = ProtonTheme.typography.headlineNorm.copy(fontWeight = FontWeight.W700)
                )
                Text(
                    text = priceUiModel.period.string(),
                    style = ProtonTheme.typography.defaultSmallWeak
                )
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
        plan.entitlements.items.forEachIndexed { index, item ->
            if (index < numberOfEntitlementsToShow || showAllEntitlements.value) {
                PlanEntitlement(entitlementUiModel = item)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProtonDimens.DefaultSpacing)
                )
            }
        }

        PremiumValueSection(modifier, plan.premiumValueDrawables)

        val numberOfEntitlementsNotShown = (plan.entitlements.items.size - numberOfEntitlementsToShow).coerceAtLeast(0)
        if (numberOfEntitlementsNotShown != 0) {
            MorePlanEntitlements(
                showAllEntitlements = showAllEntitlements.value,
                numberOfEntitlementsNotShown = numberOfEntitlementsNotShown,
                onClick = { showAllEntitlements.value = !showAllEntitlements.value }
            )
        }
    }
}

@Composable
private fun PlanEntitlement(entitlementUiModel: PlanEntitlementListUiModel) {
    val imageModel = when (entitlementUiModel) {
        is PlanEntitlementListUiModel.Default -> entitlementUiModel.remoteResource
        is PlanEntitlementListUiModel.Overridden -> entitlementUiModel.localResource
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            modifier = Modifier
                .size(ProtonDimens.LargeSpacing)
                .background(color = ProtonTheme.colors.backgroundSecondary, shape = CircleShape)
                .padding(ProtonDimens.SmallSpacing),
            placeholder = painterResource(R.drawable.ic_logo_mail_mono),
            model = imageModel,
            contentDescription = NO_CONTENT_DESCRIPTION,
            colorFilter = ColorFilter.tint(ProtonTheme.colors.iconNorm),
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
    showAllEntitlements: Boolean,
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
            painter = painterResource(
                id = if (showAllEntitlements) R.drawable.ic_proton_chevron_up else R.drawable.ic_proton_chevron_down
            ),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconAccent
        )
    }
}

@Composable
private fun PremiumValueSection(modifier: Modifier = Modifier, logoDrawables: List<Int>) {
    logoDrawables.takeIfNotEmpty()?.let {
        Column(modifier = modifier.padding(bottom = ProtonDimens.DefaultSpacing)) {
            Text(
                modifier = modifier.padding(vertical = ProtonDimens.SmallSpacing),
                text = stringResource(R.string.upselling_onboarding_premium_value_included),
                style = ProtonTheme.typography.defaultSmallStrongUnspecified.copy(color = ProtonTheme.colors.textAccent)
            )
            Row(
                modifier = modifier
            ) {
                logoDrawables.forEach {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = NO_CONTENT_DESCRIPTION
                    )
                    Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
                }
            }
        }
    }
}

@Composable
private fun UpsellButtons(
    modifier: Modifier = Modifier,
    selectedPlan: String,
    buttonsUiModel: OnboardingUpsellButtonsUiModel,
    onContinueWithProtonFree: () -> Unit,
    exitScreen: () -> Unit,
    selectedPlanUiModel: DynamicPlanInstanceUiModel?,
    paymentButtonActions: OnboardingPayButton.Actions
) {

    val context = LocalContext.current

    val getButtonLabel = buttonsUiModel.getButtonLabel[selectedPlan]
        ?: TextUiModel.TextRes(R.string.upselling_onboarding_continue_with_proton_free)

    MailDivider()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (selectedPlanUiModel != null) {
            UpsellingAutoRenewGenericPolicyText(
                modifier = Modifier.padding(
                    vertical = ProtonDimens.DefaultSpacing,
                    horizontal = ProtonDimens.SmallSpacing
                ),
                color = ProtonTheme.colors.textAccent,
                planUiModel = selectedPlanUiModel
            )

            OnboardingPayButton(
                planInstanceUiModel = selectedPlanUiModel,
                actions = paymentButtonActions.copy(
                    onDismiss = exitScreen,
                    onUpgrade = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                )
            )
        } else {
            ProtonSolidButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ProtonDimens.DefaultSpacing)
                    .height(MailDimens.OnboardingUpsellButtonHeight),
                onClick = onContinueWithProtonFree
            ) {
                Text(
                    text = getButtonLabel.string(),
                    style = ProtonTheme.typography.defaultNorm.copy(color = Color.White)
                )
            }
        }
    }

    if (selectedPlan != PROTON_FREE) {
        ProtonTextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueWithProtonFree
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .padding(top = ProtonDimens.SmallSpacing)
                    .padding(bottom = ProtonDimens.DefaultSpacing),
                text = stringResource(id = R.string.upselling_onboarding_continue_with_proton_free),
                style = ProtonTheme.typography.body1Medium.copy(
                    color = ProtonTheme.colors.textAccent
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Suppress("MagicNumber")
@Composable
private fun OnboardingUpsellScreenContentPreview() {
    ProtonTheme {
        OnboardingUpsellScreenContent(
            state = OnboardingUpsellState.Data(
                planSwitcherUiModel = OnboardingUpsellPreviewData.PlanSwitcherUiModel,
                planUiModels = OnboardingUpsellPreviewData.PlanUiModels,
                buttonsUiModel = OnboardingUpsellPreviewData.ButtonsUiModel,
                selectedPayButtonPlanUiModel = DynamicPlanInstanceUiModel.Standard(
                    name = "Proton Unlimited",
                    userId = UserIdUiModel(UserId("")),
                    currency = "CHF",
                    cycle = DynamicPlanCycle.Yearly,
                    viewId = 1,
                    pricePerCycle = TextUiModel("12.99"),
                    totalPrice = TextUiModel("144.99"),
                    dynamicPlan = OnboardingUpsellPreviewData.OnboardingDynamicPlanInstanceUiModel.dynamicPlan,
                    discountRate = null
                )
            ),
            exitScreen = {},
            onPlanSelected = { _, _ -> },
            paymentButtonActions = OnboardingPayButton.Actions.Empty
        )
    }
}

private const val MAX_ENTITLEMENTS_TO_SHOW = 4
const val PROTON_FREE = "Proton Free"

enum class PlansType { Monthly, Annual }
