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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer.plans

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanCycle
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.eventlistener.UpsellingPaymentEventListener
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpsellingPlanItem(
    modifier: Modifier = Modifier,
    planUiModel: DynamicPlanInstanceUiModel.Standard,
    actions: UpsellingScreen.Actions
) {
    val displayedPrice = planUiModel.primaryPrice
    val period = planUiModel.cycle.cycleStringValue()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.SmallSpacing)
    ) {
        if (planUiModel.discountRate != null) {
            var discountTagHeight by remember { mutableIntStateOf(0) }
            UpsellingDiscountTag(
                modifier = Modifier
                    .zIndex(UpsellingLayoutValues.SquarePaymentButtons.discountTagDefaultZIndex)
                    .offset(y = UpsellingLayoutValues.SquarePaymentButtons.discountTagVerticalOffset)
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { discountTagHeight = it.size.height },
                discountRate = planUiModel.discountRate
            )
        }

        Column(
            modifier = Modifier
                .thenIf(planUiModel.discountRate != null) {
                    Modifier
                        .background(
                            color = UpsellingLayoutValues.SquarePaymentButtons.highlightedBackgroundColor,
                            shape = UpsellingLayoutValues.SquarePaymentButtons.shape
                        )
                        .border(
                            width = MailDimens.DefaultBorder,
                            color = UpsellingLayoutValues.SquarePaymentButtons.highlightedBorderColor,
                            shape = UpsellingLayoutValues.SquarePaymentButtons.shape
                        )
                }
                .thenIf(planUiModel.discountRate == null) {
                    Modifier
                        .background(
                            color = UpsellingLayoutValues.SquarePaymentButtons.nonHighlightedBackgroundColor,
                            shape = UpsellingLayoutValues.SquarePaymentButtons.shape
                        )
                        .border(
                            width = MailDimens.DefaultBorder,
                            color = UpsellingLayoutValues.SquarePaymentButtons.nonHighlightedBorderColor,
                            shape = UpsellingLayoutValues.SquarePaymentButtons.shape
                        )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(
                    top = ProtonDimens.MediumSpacing,
                    bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
                ),
                text = pluralStringResource(
                    R.plurals.upselling_month,
                    planUiModel.cycle.months,
                    planUiModel.cycle.months
                ),
                style = ProtonTheme.typography.captionRegular,
                color = UpsellingLayoutValues.SquarePaymentButtons.textColor
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = planUiModel.currency,
                    style = ProtonTheme.typography.body1Bold,
                    color = UpsellingLayoutValues.SquarePaymentButtons.textColor
                )
                Spacer(modifier = Modifier.padding(UpsellingLayoutValues.SquarePaymentButtons.currencyDivider))
                Text(
                    text = displayedPrice.highlightedPrice.string(),
                    style = ProtonTheme.typography.body1Bold,
                    color = UpsellingLayoutValues.SquarePaymentButtons.textColor
                )
                Text(
                    text = period.string(),
                    style = ProtonTheme.typography.overlineRegular,
                    color = UpsellingLayoutValues.SquarePaymentButtons.subtextColor,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))

            Row {
                if (planUiModel.cycle == DynamicPlanCycle.Yearly && displayedPrice.secondaryPrice != null) {
                    Text(
                        text = planUiModel.currency,
                        style = ProtonTheme.typography.overlineRegular,
                        color = UpsellingLayoutValues.SquarePaymentButtons.subtextColor
                    )
                    Spacer(modifier = Modifier.padding(UpsellingLayoutValues.SquarePaymentButtons.currencyDivider))
                    Text(
                        text = displayedPrice.secondaryPrice.string(),
                        style = ProtonTheme.typography.overlineRegular,
                        color = UpsellingLayoutValues.SquarePaymentButtons.subtextColor
                    )
                    Text(
                        text = stringResource(id = R.string.upselling_month),
                        style = ProtonTheme.typography.overlineRegular,
                        color = UpsellingLayoutValues.SquarePaymentButtons.subtextColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    Text(
                        text = "",
                        style = ProtonTheme.typography.overlineRegular,
                        color = UpsellingLayoutValues.SquarePaymentButtons.subtextColor
                    )
                }
            }

            val eventListener = UpsellingPaymentEventListener(
                context = LocalContext.current,
                userId = planUiModel.userId.value,
                telemetryPayload = planUiModel.toTelemetryPayload(),
                actions
            )
            val buttonCornerRadius = UpsellingLayoutValues.SquarePaymentButtons.buttonCornerRadius
            var maxWrappedViewSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { maxWrappedViewSize = it.size }
            ) {
                AndroidView(
                    modifier = Modifier
                        .widthIn(max = maxWrappedViewSize.width.pxToDp())
                        .align(Alignment.Center)
                        .padding(ProtonDimens.DefaultSpacing),
                    factory = { ctx ->
                        ProtonPaymentButton(ContextThemeWrapper(ctx, R.style.ProtonTheme)).apply {
                            this.id = planUiModel.viewId
                        }
                    },
                    update = { button ->
                        button.apply {
                            this.cornerRadius = buttonCornerRadius
                            this.userId = planUiModel.userId.value
                            this.currency = planUiModel.currency
                            this.cycle = planUiModel.cycle.months
                            this.plan = planUiModel.dynamicPlan
                            this.text = context.getString(R.string.upselling_get_button, planUiModel.name)
                            this.outlineProvider = null
                            this.clipToOutline = false
                            this.elevation = 0f

                            if (planUiModel.discountRate == null) {
                                setBackgroundColor(
                                    UpsellingLayoutValues.SquarePaymentButtons.SecondaryPaymentButtonBackground
                                )
                            }

                            setOnEventListener(eventListener)
                        }
                    }
                )
            }
        }
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingItem() {
    val plans = UpsellingContentPreviewData.Base.plans.list as DynamicPlanInstanceListUiModel.Data
    ProtonTheme {
        Column {
            UpsellingPlanItem(
                modifier = Modifier,
                plans.longerCycle as DynamicPlanInstanceUiModel.Standard,
                actions = UpsellingScreen.Actions.Empty
            )
            UpsellingPlanItem(
                modifier = Modifier,
                plans.shorterCycle as DynamicPlanInstanceUiModel.Standard,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
