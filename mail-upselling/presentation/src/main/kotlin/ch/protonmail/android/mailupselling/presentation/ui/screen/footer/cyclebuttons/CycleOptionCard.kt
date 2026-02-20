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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.labelMediumNorm
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.cyclePlanName
import ch.protonmail.android.mailupselling.presentation.extension.cycleStringValue
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingCheckmark
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData

@Composable
internal fun CycleOptionCard(
    cycleOptionUiModel: PlanUpgradeInstanceUiModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = true
) {
    val paymentButtonParams = UpsellingLayoutValues.PaymentButtons
    val defaultColors = CardDefaults.outlinedCardColors()
    val containerColor = if (isSelected) {
        paymentButtonParams.outlinedCardContainerSelectedColor
    } else {
        paymentButtonParams.outlinedCardContainerColor
    }

    val border = if (isSelected) {
        paymentButtonParams.outlinedCardSelectedBorderStroke
    } else {
        CardDefaults.outlinedCardBorder(enabled = false)
    }

    Box(modifier = modifier) {
        OutlinedCard(
            colors = defaultColors.copy(containerColor = containerColor),
            border = border,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = ProtonDimens.IconSize.Default / 2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ProtonDimens.Spacing.Large),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CycleName(uiModel = cycleOptionUiModel)
                    CyclePrices(uiModel = cycleOptionUiModel)
                }
            }
        }

        if (isSelected) {
            UpsellingCheckmark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = ProtonDimens.Spacing.Large)
                    .padding(vertical = ProtonDimens.Spacing.Compact)
            )
        }
    }
}

@Composable
private fun CycleName(modifier: Modifier = Modifier, uiModel: PlanUpgradeInstanceUiModel) {
    val paymentButtonParams = UpsellingLayoutValues.PaymentButtons

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiModel.cycle.cyclePlanName().string(),
            style = ProtonTheme.typography.bodyLarge,
            color = paymentButtonParams.planNameColor
        )

        val discountRate = uiModel.discountRate
        if (discountRate != null) {
            Box(
                modifier = Modifier.padding(start = ProtonDimens.Spacing.Small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .background(
                            color = paymentButtonParams.discountTagBackground,
                            shape = paymentButtonParams.discountBadgeShape
                        )
                        .padding(horizontal = ProtonDimens.Spacing.Compact, vertical = ProtonDimens.Spacing.Small),
                    text = TextUiModel.TextResWithArgs(
                        R.string.upselling_select_plan_save,
                        listOf(uiModel.discountRate.toString())
                    ).string(),
                    style = ProtonTheme.typography.labelMediumNorm,
                    fontWeight = FontWeight.Bold,
                    color = paymentButtonParams.discountTagTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CyclePrices(modifier: Modifier = Modifier, uiModel: PlanUpgradeInstanceUiModel) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        PrimaryPriceRow(
            price = uiModel.primaryPrice.highlightedPrice,
            period = uiModel.cycle.cycleStringValue()
        )

        SecondaryPriceText(uiModel = uiModel)
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrimaryPriceRow(price: PlanUpgradePriceUiModel, period: TextUiModel) {
    FlowRow {
        Text(
            text = price.getFullFormat(),
            style = ProtonTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = ProtonDimens.Spacing.Small / 2),
            text = period.string(),
            style = ProtonTheme.typography.labelMediumNorm,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun getSecondaryPriceInfo(uiModel: PlanUpgradeInstanceUiModel): SecondaryPriceInfo? {
    val primaryPrice = uiModel.primaryPrice
    val pricePerCycle = primaryPrice.pricePerCycle

    return when (uiModel) {
        is PlanUpgradeInstanceUiModel.Standard -> {
            if (uiModel.cycle != PlanUpgradeCycle.Monthly) {
                SecondaryPriceInfo(
                    text = "${pricePerCycle.getFullFormat()} ${stringResource(R.string.upselling_month)}"
                )
            } else null
        }

        is PlanUpgradeInstanceUiModel.Promotional.BlackFriday -> {
            when (uiModel.cycle) {
                PlanUpgradeCycle.Monthly -> {
                    primaryPrice.secondaryPrice?.let {
                        SecondaryPriceInfo(
                            text = "${it.getFullFormat()} ${stringResource(R.string.upselling_month)}",
                            strikethrough = true
                        )
                    }
                }

                PlanUpgradeCycle.Yearly -> {
                    SecondaryPriceInfo(
                        text = stringResource(R.string.upselling_month_only, pricePerCycle.getFullFormat())
                    )
                }
            }
        }

        is PlanUpgradeInstanceUiModel.Promotional.SpringPromo -> {
            when (uiModel.cycle) {
                PlanUpgradeCycle.Yearly -> {
                    SecondaryPriceInfo(
                        text = stringResource(R.string.upselling_month_only, pricePerCycle.getFullFormat())
                    )
                }

                else -> null
            }
        }

        else -> null
    }
}

@Composable
private fun SecondaryPriceText(uiModel: PlanUpgradeInstanceUiModel) {
    val info = getSecondaryPriceInfo(uiModel) ?: return

    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Tiny))
    Text(
        text = info.text,
        style = ProtonTheme.typography.labelMediumNorm.copy(
            textDecoration = if (info.strikethrough) TextDecoration.LineThrough else null
        ),
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.End
    )
}

private data class SecondaryPriceInfo(
    val text: String,
    val strikethrough: Boolean = false
)

@AdaptivePreviews
@Composable
private fun CycleOptionCardPreview_Regular() {
    val plans = UpsellingContentPreviewData.NormalList
    val cycle = plans.longerCycle
    ProtonTheme {
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
    ProtonTheme {
        Column(modifier = Modifier.height(108.dp)) {
            CycleOptionCard(
                cycleOptionUiModel = cycle,
                modifier = Modifier
            )
        }
    }
}
