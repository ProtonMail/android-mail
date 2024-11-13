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

package ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable.cyclebuttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.isYearly
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.bottomsheet.UpsellingBottomSheetContentPreviewData
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3

@Composable
internal fun CycleOptions(
    modifier: Modifier = Modifier,
    plans: DynamicPlanInstanceListUiModel.Data,
    selectedPlan: DynamicPlanInstanceUiModel,
    onPlanSelected: (plan: DynamicPlanInstanceUiModel) -> Unit
) {
    val density = LocalDensity.current
    var maxHeight by remember { mutableStateOf(0.dp) }

    val shorterInteractionSource = remember { MutableInteractionSource() }
    val longerInteractionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier) {
        // Yearly option sets the height.
        CycleOptionCard(
            cycleOptionUiModel = plans.longerCycle,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    with(density) {
                        maxHeight = size.height.toDp()
                    }
                }
                .clickable(interactionSource = longerInteractionSource, indication = null) {
                    onPlanSelected(plans.longerCycle)
                },
            isSelected = plans.longerCycle == selectedPlan
        )

        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))

        // Monthly option inherits the height of the yearly option.
        CycleOptionCard(
            cycleOptionUiModel = plans.shorterCycle,
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .clickable(interactionSource = shorterInteractionSource, indication = null) {
                    onPlanSelected(plans.shorterCycle)
                },
            isSelected = plans.shorterCycle == selectedPlan
        )
    }
}

@Composable
internal fun CycleOptionCard(
    modifier: Modifier = Modifier,
    cycleOptionUiModel: DynamicPlanInstanceUiModel,
    isSelected: Boolean = true
) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors()
            .copy(containerColor = UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardContainerColor),
        border = if (isSelected) {
            UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardSelectedBorderStroke
        } else {
            UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardStandardBorderStroke
        },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ProtonDimens.DefaultSpacing),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CycleName(uiModel = cycleOptionUiModel)
                CyclePrices(uiModel = cycleOptionUiModel)
            }
        }
    }
}

@Composable
private fun CycleName(modifier: Modifier = Modifier, uiModel: DynamicPlanInstanceUiModel) {
    val name = if (uiModel.isYearly()) {
        stringResource(R.string.upselling_select_plan_year)
    } else {
        stringResource(R.string.upselling_select_plan_month)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = ProtonTheme.typography.body2Regular,
            fontSize = UpsellingLayoutValues.RectangularPaymentButtons.textSize,
            color = UpsellingLayoutValues.RectangularPaymentButtons.textColor
        )

        if (uiModel.discount != null) {
            Box(
                modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colorStops = UpsellingLayoutValues.RectangularPaymentButtons.discountTagColorStops
                            ),
                            shape = UpsellingLayoutValues.RectangularPaymentButtons.discountBadgeShape
                        )
                        .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
                    text = TextUiModel.TextResWithArgs(
                        R.string.upselling_select_plan_save,
                        listOf(uiModel.discount.toString())
                    ).string(),
                    style = ProtonTheme.typography.captionMedium,
                    color = UpsellingLayoutValues.RectangularPaymentButtons.discountTagTextColor,
                    textAlign = TextAlign.Center,
                    fontSize = UpsellingLayoutValues.RectangularPaymentButtons.discountTextSize
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CyclePrices(modifier: Modifier = Modifier, uiModel: DynamicPlanInstanceUiModel) {
    val (highlightedPrice, price, period) = when {
        uiModel.discount != null ->
            Triple(uiModel.fullPrice, uiModel.price.string(), stringResource(R.string.upselling_year))

        else ->
            Triple(uiModel.price, null, stringResource(R.string.upselling_month))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        FlowRow {
            Text(
                text = "${uiModel.currency} ${highlightedPrice.string()}",
                style = ProtonTheme.typography.body1Bold,
                fontSize = UpsellingLayoutValues.RectangularPaymentButtons.mainPriceTextSize,
                color = UpsellingLayoutValues.RectangularPaymentButtons.mainPriceTextColor
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = ProtonDimens.ExtraSmallSpacing / 2),
                text = period,
                style = ProtonTheme.typography.captionRegular,
                fontSize = UpsellingLayoutValues.RectangularPaymentButtons.subtextSize,
                color = UpsellingLayoutValues.RectangularPaymentButtons.subtextColor
            )
        }

        if (uiModel.discount != null) {
            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
            Text(
                text = "${uiModel.currency} $price${stringResource(R.string.upselling_month)}",
                style = ProtonTheme.typography.captionRegular,
                fontSize = UpsellingLayoutValues.RectangularPaymentButtons.subtextSize,
                color = UpsellingLayoutValues.RectangularPaymentButtons.subtextColor,
                textAlign = TextAlign.End
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingCard() {
    ProtonTheme3 {
        Column(modifier = Modifier.height(108.dp)) {
            CycleOptions(
                plans = UpsellingBottomSheetContentPreviewData.Base.plans.list as DynamicPlanInstanceListUiModel.Data,
                selectedPlan = UpsellingBottomSheetContentPreviewData.Base.plans.list.longerCycle
            ) { }
        }
    }
}
