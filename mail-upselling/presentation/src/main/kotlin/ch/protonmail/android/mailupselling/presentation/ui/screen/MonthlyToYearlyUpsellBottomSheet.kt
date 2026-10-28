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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.labelMediumNorm
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.domain.extensions.normalizedPriceWithCurrency
import ch.protonmail.android.mailupselling.domain.extensions.totalPrice
import ch.protonmail.android.mailupselling.domain.extensions.totalPriceWithCurrency
import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.ProductPriceWithCurrency
import ch.protonmail.android.mailupselling.domain.usecase.UpsellVariantPlan
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.formatted
import ch.protonmail.android.mailupselling.presentation.extension.toUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.toProduct
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.MailPurchaseButton
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.MailPurchaseButtonVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.UpsellingSecondaryButton
import ch.protonmail.android.mailupselling.presentation.viewmodel.MonthlyToYearlyUpsellSheetViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun MonthlyToYearlyUpsellBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthlyToYearlyUpsellSheetViewModel = hiltViewModel()
) {
    val offer by viewModel.offer.collectAsStateWithLifecycle()

    MonthlyToYearlyUpsellBottomSheet(
        offer = offer,
        actions = MonthlyToYearlyUpsellBottomSheet.Actions(
            onUpgradeAttempt = viewModel::recordUpgradeAttempt,
            onUpgradeErrored = viewModel::recordUpgradeError,
            onSwitched = {
                viewModel.recordUpgradeSuccess()
                viewModel.onSwitchedToYearly()
                onDismiss()
            },
            onDoNotShowAgain = {
                viewModel.recordDoNotShowAgain()
                onDismiss()
            },
            onDismiss = {
                viewModel.recordUpgradeCancelledByUser()
                onDismiss()
            }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyToYearlyUpsellBottomSheet(
    offer: MonthlyToYearlyOffer?,
    actions: MonthlyToYearlyUpsellBottomSheet.Actions,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = { close(actions.onDismiss) },
        modifier = modifier,
        sheetState = sheetState,
        shape = ProtonTheme.shapes.bottomSheet,
        containerColor = Color.Transparent,
        contentColor = UpsellingLayoutValues.titleColor,
        scrimColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) }
    ) {
        if (offer == null) {
            MonthlyToYearlyUpsellBottomSheetLoading()
        } else {
            MonthlyToYearlyUpsellBottomSheetContent(
                offer = offer,
                actions = actions.copy(
                    onSwitched = { close(actions.onSwitched) },
                    onDoNotShowAgain = { close(actions.onDoNotShowAgain) }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyToYearlyUpsellBottomSheetLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpsellingLayoutValues.backgroundGradient)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BottomSheetDefaults.DragHandle(color = UpsellingLayoutValues.MonthlyToYearly.dragHandleColor)
        ProtonCenteredProgress(modifier = Modifier.padding(ProtonDimens.Spacing.Huge))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyToYearlyUpsellBottomSheetContent(
    offer: MonthlyToYearlyOffer,
    actions: MonthlyToYearlyUpsellBottomSheet.Actions
) {
    val context = LocalContext.current
    val monthlyPrice = offer.monthly.renew
    val yearlyPrice = offer.yearly.offer.current
    val perMonth = monthlyPrice
        .normalizedPriceWithCurrency(monthlyPrice.currency, PlanUpgradeCycle.Monthly.months)
        .toUiModel()
        .getFullFormat()
    val monthlyPerYear = ProductPriceWithCurrency(
        monthlyPrice.totalPrice().multiply(BigDecimal(PlanUpgradeCycle.Yearly.months)),
        monthlyPrice.currency
    ).toUiModel().getFullFormat()
    val yearlyPerYear = yearlyPrice.totalPriceWithCurrency(yearlyPrice.currency).toUiModel().getFullFormat()
    val values = UpsellingLayoutValues.MonthlyToYearly

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpsellingLayoutValues.backgroundGradient)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BottomSheetDefaults.DragHandle(color = values.dragHandleColor)
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
        Image(
            modifier = Modifier.height(values.illustrationHeight),
            painter = painterResource(id = offer.plan.illustrationRes),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Huge),
            text = stringResource(R.string.upselling_monthly_to_yearly_title),
            style = ProtonTheme.typography.titleLargeNorm,
            fontWeight = FontWeight.Bold,
            color = UpsellingLayoutValues.titleColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))
        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Huge),
            text = stringResource(R.string.upselling_monthly_to_yearly_subtitle, offer.yearly.header.title),
            style = ProtonTheme.typography.bodyLargeNorm,
            color = UpsellingLayoutValues.subtitleColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))
        HorizontalDivider(color = values.pricingDividerColor)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large)
        ) {
            Text(
                text = stringResource(R.string.upselling_pu_choose_plan),
                style = ProtonTheme.typography.labelMediumNorm,
                fontWeight = FontWeight.Bold,
                color = UpsellingLayoutValues.PaymentButtons.choosePlanColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
                PriceRow(
                    name = stringResource(R.string.upselling_monthly_to_yearly_one_year),
                    badge = stringResource(R.string.upselling_monthly_to_yearly_currently),
                    price = monthlyPerYear,
                    secondaryPrice = perMonth,
                    secondaryPeriod = stringResource(R.string.upselling_month),
                    isSelected = false
                )
                PriceRow(
                    name = stringResource(R.string.upselling_monthly_to_yearly_yearly_plan),
                    badge = offer.discountRate?.let { stringResource(R.string.upselling_select_plan_save, it) },
                    price = yearlyPerYear,
                    isSelected = true
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.upselling_auto_renew_text),
                    style = ProtonTheme.typography.bodySmall,
                    color = UpsellingLayoutValues.subtitleColor,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.padding(top = ProtonDimens.Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
            ) {
                MailPurchaseButton(
                    product = offer.yearly.toProduct(context),
                    modifier = Modifier.fillMaxWidth(),
                    ctaText = stringResource(
                        R.string.upselling_monthly_to_yearly_cta,
                        offer.saving.formatted().string()
                    ),
                    variant = MailPurchaseButtonVariant.Default,
                    onPurchaseClicked = actions.onUpgradeAttempt,
                    onSuccess = { actions.onSwitched() },
                    onErrorMessage = { actions.onUpgradeErrored() }
                )
                UpsellingSecondaryButton(
                    text = stringResource(R.string.upselling_monthly_to_yearly_stay_on_monthly),
                    textColor = UpsellingLayoutValues.PaymentButtons.planNameColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = actions.onDoNotShowAgain
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    name: String,
    badge: String?,
    price: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    secondaryPrice: String? = null,
    secondaryPeriod: String? = null
) {
    val values = UpsellingLayoutValues.PaymentButtons
    val shape = ProtonTheme.shapes.extraLarge
    val containerColor = when {
        isSelected -> values.outlinedCardContainerSelectedColor
        else -> values.outlinedCardContainerColor
    }
    val borderModifier = if (isSelected) Modifier.border(values.outlinedCardSelectedBorderStroke, shape) else Modifier
    val secondaryColor = UpsellingLayoutValues.MonthlyToYearly.secondaryTextColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(UpsellingLayoutValues.MonthlyToYearly.priceRowHeight)
            .background(containerColor, shape)
            .then(borderModifier)
            .padding(horizontal = ProtonDimens.Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = ProtonTheme.typography.bodyLarge,
                color = values.planNameColor
            )
            if (badge != null) {
                Text(
                    modifier = Modifier
                        .background(values.discountTagBackground, values.discountBadgeShape)
                        .padding(horizontal = ProtonDimens.Spacing.Standard, vertical = ProtonDimens.Spacing.Small),
                    text = badge,
                    style = ProtonTheme.typography.labelMediumNorm,
                    fontWeight = FontWeight.Bold,
                    color = values.discountTagTextColor
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = values.planNameColor
                )
                Text(
                    modifier = Modifier.padding(start = ProtonDimens.Spacing.Small, bottom = ProtonDimens.Spacing.Tiny),
                    text = stringResource(R.string.upselling_year),
                    style = ProtonTheme.typography.labelMediumNorm,
                    color = secondaryColor
                )
            }
            if (secondaryPrice != null && secondaryPeriod != null) {
                Text(
                    text = "$secondaryPrice $secondaryPeriod",
                    style = ProtonTheme.typography.labelMediumNorm,
                    color = secondaryColor
                )
            }
        }
    }
}

object MonthlyToYearlyUpsellBottomSheet {

    data class Actions(
        val onUpgradeAttempt: () -> Unit,
        val onUpgradeErrored: () -> Unit,
        val onSwitched: () -> Unit,
        val onDoNotShowAgain: () -> Unit,
        val onDismiss: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onUpgradeAttempt = {},
                onUpgradeErrored = {},
                onSwitched = {},
                onDoNotShowAgain = {},
                onDismiss = {}
            )
        }
    }
}

private val UpsellVariantPlan.illustrationRes: Int
    get() = when (this) {
        UpsellVariantPlan.MAIL_PLUS -> R.drawable.illustration_monthly_to_yearly_plus
        UpsellVariantPlan.UNLIMITED -> R.drawable.illustration_monthly_to_yearly_unlimited
    }

internal class MonthlyToYearlyOfferPreviewProvider : PreviewParameterProvider<MonthlyToYearlyOffer> {
    override val values: Sequence<MonthlyToYearlyOffer>
        get() = sequenceOf(
            UpsellingContentPreviewData.MonthlyToYearlyPlusOffer,
            UpsellingContentPreviewData.MonthlyToYearlyUnlimitedOffer
        )
}

@AdaptivePreviews
@Composable
private fun MonthlyToYearlyUpsellSheetPreview(
    @PreviewParameter(MonthlyToYearlyOfferPreviewProvider::class) offer: MonthlyToYearlyOffer
) {
    ProtonTheme {
        MonthlyToYearlyUpsellBottomSheetContent(
            offer = offer,
            actions = MonthlyToYearlyUpsellBottomSheet.Actions.Empty
        )
    }
}
