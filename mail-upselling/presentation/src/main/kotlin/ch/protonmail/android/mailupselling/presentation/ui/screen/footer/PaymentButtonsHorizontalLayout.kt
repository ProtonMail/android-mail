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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.eventlistener.UpsellingPaymentEventListener
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons.CycleOptions
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@Composable
internal fun PaymentButtonsHorizontalLayout(
    plans: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    var selectedPlan by remember { mutableStateOf(plans.longerCycle) }

    Column {
        Text(
            modifier = Modifier.padding(
                top = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing,
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing
            ).align(Alignment.CenterHorizontally),
            text = stringResource(R.string.upselling_select_plan),
            style = ProtonTheme.typography.body2Regular,
            fontSize = UpsellingLayoutValues.RectangularPaymentButtons.textSize,
            color = UpsellingLayoutValues.RectangularPaymentButtons.textColor,
            textAlign = TextAlign.Center
        )

        Box(
            Modifier
                .wrapContentHeight()
                .padding(horizontal = ProtonDimens.DefaultSpacing)
        ) {
            CycleOptions(
                plans = plans,
                selectedPlan = selectedPlan,
                onPlanSelected = { selectedPlan = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        UpsellingAutoRenewGenericPolicyText(
            modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
            planUiModel = selectedPlan
        )

        val userId = plans.longerCycle.userId.value
        val eventListener = UpsellingPaymentEventListener(
            context = LocalContext.current,
            userId = userId,
            telemetryPayload = selectedPlan.toTelemetryPayload(plans.variant),
            actions
        )
        val buttonCornerRadius = 8.dp.dpToPx()

        Box(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .padding(bottom = ProtonDimens.DefaultSpacing),
                factory = { ctx ->
                    ProtonPaymentButton(ContextThemeWrapper(ctx, R.style.ProtonTheme_LightPaymentButton))
                },
                update = { button ->
                    button.apply {
                        this.setBackgroundColor(
                            UpsellingLayoutValues.UpsellingPlanButtonsFooter.paymentButtonBackground
                        )
                        this.setTextAppearance(R.style.ProtonTextView_LightPaymentButton)
                        this.cornerRadius = buttonCornerRadius
                        this.userId = userId
                        this.currency = selectedPlan.currency
                        this.cycle = selectedPlan.cycle.months
                        this.plan = selectedPlan.dynamicPlan
                        this.text = context.getString(R.string.upselling_get_button, selectedPlan.name)
                        setOnEventListener(eventListener)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun PaymentButtonsHorizontalLayoutPreview() {
    ProtonTheme3 {
        Box(modifier = Modifier.height(480.dp)) {
            PaymentButtonsHorizontalLayout(
                plans = UpsellingContentPreviewData.NormalList,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
