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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3

@Composable
internal fun CycleOptionCard(
    cycleOptionUiModel: DynamicPlanInstanceUiModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = true
) {
    Box(modifier = modifier) {
        OutlinedCard(
            colors = CardDefaults.outlinedCardColors()
                .copy(containerColor = UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardContainerColor),
            border = if (isSelected) {
                UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardSelectedBorderStroke
            } else {
                UpsellingLayoutValues.RectangularPaymentButtons.outlinedCardStandardBorderStroke
            },
            modifier = Modifier.padding(top = ProtonDimens.DefaultIconSize / 2)
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
        if (isSelected) {
            Image(
                painter = painterResource(id = R.drawable.ic_check_filled),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = ProtonDimens.DefaultIconSize / 2)
                    .width(ProtonDimens.DefaultIconSize)
                    .wrapContentHeight(),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
        }
    }
}

@Composable
private fun CycleName(modifier: Modifier = Modifier, uiModel: DynamicPlanInstanceUiModel) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiModel.cycle.cyclePlanName().string(),
            style = ProtonTheme.typography.body2Regular,
            fontSize = UpsellingLayoutValues.RectangularPaymentButtons.textSize,
            color = UpsellingLayoutValues.RectangularPaymentButtons.textColor
        )

        val discountRate = uiModel.discountRate

        if (discountRate != null) {
            Box(
                modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .background(
                            color = UpsellingLayoutValues.RectangularPaymentButtons.discountTagBackground,
                            shape = UpsellingLayoutValues.RectangularPaymentButtons.discountBadgeShape
                        )
                        .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
                    text = TextUiModel.TextResWithArgs(
                        R.string.upselling_select_plan_save,
                        listOf(uiModel.discountRate.toString())
                    ).string(),
                    style = ProtonTheme.typography.body1Bold,
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

    val primaryPrice = uiModel.primaryPrice
    val displayedPrice = primaryPrice.highlightedPrice
    val pricePerCycle = primaryPrice.pricePerCycle
    val period = uiModel.cycle.cycleStringValue()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        FlowRow {
            Text(
                text = "${uiModel.currency} ${displayedPrice.string()}",
                style = ProtonTheme.typography.body1Bold,
                fontSize = UpsellingLayoutValues.RectangularPaymentButtons.mainPriceTextSize,
                color = UpsellingLayoutValues.RectangularPaymentButtons.mainPriceTextColor
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = ProtonDimens.ExtraSmallSpacing / 2),
                text = period.string(),
                style = ProtonTheme.typography.captionRegular,
                fontSize = UpsellingLayoutValues.RectangularPaymentButtons.subtextSize,
                color = UpsellingLayoutValues.RectangularPaymentButtons.subtextColor
            )
        }

        if (uiModel.cycle != DynamicPlanCycle.Monthly) {
            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
            Text(
                text = "${uiModel.currency} ${pricePerCycle.string()}${stringResource(R.string.upselling_month)}",
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
private fun CycleOptionCardPreview_Regular() {
    val plans = UpsellingContentPreviewData.NormalList
    val cycle = plans.longerCycle
    ProtonTheme3 {
        Column(modifier = Modifier.height(108.dp)) {
            CycleOptionCard(
                cycleOptionUiModel = cycle,
                modifier = Modifier
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun CycleOptionCardPreview_Promo() {
    val plans = UpsellingContentPreviewData.PromoList
    val cycle = plans.longerCycle
    ProtonTheme3 {
        Column(modifier = Modifier.height(108.dp)) {
            CycleOptionCard(
                cycleOptionUiModel = cycle,
                modifier = Modifier
            )
        }
    }
}
