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

package ch.protonmail.android.mailupselling.presentation.ui.bottomsheet

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingColors
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingDimens
import ch.protonmail.android.uicomponents.chips.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionUnspecified
import me.proton.core.compose.theme.defaultHighlightUnspecified
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UpsellingPlanItem(
    modifier: Modifier = Modifier,
    planUiModel: DynamicPlanInstanceUiModel,
    actions: UpsellingBottomSheet.Actions
) {
    val colors = requireNotNull(UpsellingColors.BottomSheetContentColors)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = ProtonDimens.SmallSpacing)
    ) {
        if (planUiModel.discount != null) {
            var discountTagHeight by remember { mutableIntStateOf(0) }
            UpsellingDiscountTag(
                modifier = Modifier
                    .zIndex(UpsellingDimens.DiscountTagDefaultZIndex)
                    .offset(y = UpsellingDimens.DiscountTagVerticalOffset)
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { discountTagHeight = it.size.height },
                text = planUiModel.discount
            )
        }

        Column(
            modifier = Modifier
                .thenIf(planUiModel.discount != null) {
                    background(
                        color = UpsellingColors.PaymentDiscountedItemBackground,
                        shape = RoundedCornerShape(ProtonDimens.DefaultSpacing)
                    )
                    border(
                        width = MailDimens.DefaultBorder,
                        color = UpsellingColors.PaymentDiscountedItemBorder,
                        shape = RoundedCornerShape(ProtonDimens.DefaultSpacing)
                    )
                }
                .thenIf(planUiModel.discount == null) {
                    background(
                        color = UpsellingColors.PaymentStandardItemBackground,
                        shape = RoundedCornerShape(ProtonDimens.DefaultSpacing)
                    )
                    border(
                        width = MailDimens.DefaultBorder,
                        color = UpsellingColors.PaymentStandardItemBorder,
                        shape = RoundedCornerShape(ProtonDimens.DefaultSpacing)
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(
                    top = ProtonDimens.MediumSpacing,
                    bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
                ),
                text = pluralStringResource(R.plurals.upselling_month, planUiModel.cycle, planUiModel.cycle),
                style = ProtonTheme.typography.captionUnspecified,
                color = colors.textNorm
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = planUiModel.currency,
                    style = ProtonTheme.typography.defaultHighlightUnspecified,
                    color = colors.textNorm
                )
                Spacer(modifier = Modifier.padding(UpsellingDimens.CurrencyDivider))
                Text(
                    text = planUiModel.price.string(),
                    style = ProtonTheme.typography.defaultHighlightUnspecified,
                    color = colors.textNorm
                )
                Text(
                    text = stringResource(id = R.string.upselling_month),
                    style = ProtonTheme.typography.captionUnspecified,
                    color = colors.textWeak,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            val eventListener = UpsellingPaymentEventListener(
                context = LocalContext.current,
                userId = planUiModel.userId.value,
                actions
            )
            val buttonHeight = UpsellingDimens.ButtonHeight
            val buttonCornerRadius = UpsellingDimens.ButtonCornerRadius

            AndroidView(
                modifier = Modifier
                    .padding(ProtonDimens.DefaultSpacing)
                    .fillMaxWidth(),
                factory = { ctx ->
                    ProtonPaymentButton(ContextThemeWrapper(ctx, R.style.ProtonTheme)).apply {
                        this.id = planUiModel.viewId
                    }
                },
                update = { button ->
                    button.apply {
                        this.cornerRadius = buttonCornerRadius
                        this.height = buttonHeight
                        this.userId = planUiModel.userId.value
                        this.currency = planUiModel.currency
                        this.cycle = planUiModel.cycle
                        this.plan = planUiModel.dynamicPlan
                        this.text = context.getString(R.string.upselling_get_button, planUiModel.name)
                        if (!planUiModel.highlighted) {
                            this.setBackgroundColor(UpsellingColors.SecondaryButtonBackground)
                        }
                        setOnEventListener(eventListener)
                    }
                }
            )
        }
    }
}
