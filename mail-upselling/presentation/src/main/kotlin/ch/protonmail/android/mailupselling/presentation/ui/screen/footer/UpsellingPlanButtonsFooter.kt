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
import androidx.compose.foundation.background
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
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.eventlistener.UpsellingPaymentEventListener
import ch.protonmail.android.mailupselling.presentation.ui.screen.LocalEntryPointIsStandalone
import ch.protonmail.android.mailupselling.presentation.ui.screen.LocalPaymentButtonsHorizontalEnabled
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.cyclebuttons.CycleOptions
import ch.protonmail.android.mailupselling.presentation.ui.screen.plans.UpsellingPlansList
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@Composable
internal fun UpsellingPlanButtonsFooter(
    modifier: Modifier = Modifier,
    plans: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    Column(
        modifier.background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.backgroundColor)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerHeight)
                .background(UpsellingLayoutValues.UpsellingPlanButtonsFooter.spacerColor)
        )

        val hasStandaloneHost = LocalEntryPointIsStandalone.current
        val shouldDisplayHorizontalLayout = LocalPaymentButtonsHorizontalEnabled.current

        if (hasStandaloneHost && shouldDisplayHorizontalLayout) {
            PaymentButtonsHorizontalLayout(plans, actions)
        } else {
            PaymentButtonsSideBySideLayout(plans, actions)
        }
    }
}

@Composable
private fun PaymentButtonsHorizontalLayout(
    plans: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    var selectedPlan by remember { mutableStateOf(plans.longerCycle) }
    val telemetryPayload = UpsellingTelemetryTargetPlanPayload(selectedPlan.name, selectedPlan.cycle)

    Column {
        Text(
            modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
            text = stringResource(R.string.upselling_select_plan),
            style = ProtonTheme.typography.body2Regular,
            fontSize = UpsellingLayoutValues.RectangularPaymentButtons.textSize,
            color = UpsellingLayoutValues.RectangularPaymentButtons.textColor,
            textAlign = TextAlign.Start
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

        UpsellingAutoRenewPolicyText(modifier = Modifier.padding(ProtonDimens.DefaultSpacing))

        val userId = plans.longerCycle.userId.value
        val eventListener = UpsellingPaymentEventListener(
            context = LocalContext.current,
            userId = userId,
            telemetryPayload,
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
                    ProtonPaymentButton(ContextThemeWrapper(ctx, R.style.ProtonTheme))
                },
                update = { button ->
                    button.apply {
                        this.setBackgroundColor(
                            UpsellingLayoutValues.UpsellingPlanButtonsFooter.paymentButtonBackground
                        )
                        this.setTextAppearance(R.style.ProtonTextView_Body1)
                        this.setTextColor(UpsellingLayoutValues.UpsellingPlanButtonsFooter.paymentButtonTextColor)
                        this.textSize = UpsellingLayoutValues.UpsellingPlanButtonsFooter.paymentButtonTextSize
                        this.cornerRadius = buttonCornerRadius
                        this.userId = userId
                        this.currency = selectedPlan.currency
                        this.cycle = selectedPlan.cycle
                        this.plan = selectedPlan.dynamicPlan
                        this.text = context.getString(R.string.upselling_get_button, selectedPlan.name)
                        setOnEventListener(eventListener)
                    }
                }
            )
        }
    }
}

@Composable
private fun PaymentButtonsSideBySideLayout(
    plans: DynamicPlanInstanceListUiModel.Data,
    actions: UpsellingScreen.Actions
) {
    UpsellingPlansList(
        modifier = Modifier
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(bottom = ProtonDimens.DefaultSpacing),
        plans,
        actions
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF522580)
@AdaptivePreviews
@Composable
private fun UpsellingStickyFooterPreview() {
    ProtonTheme3 {
        Box(modifier = Modifier.height(480.dp)) {
            UpsellingPlanButtonsFooter(
                plans = UpsellingContentPreviewData.Base.plans.list
                    as DynamicPlanInstanceListUiModel.Data,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
