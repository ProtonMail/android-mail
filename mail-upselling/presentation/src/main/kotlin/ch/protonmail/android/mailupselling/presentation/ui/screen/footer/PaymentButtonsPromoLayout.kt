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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.eventlistener.UpsellingPaymentEventListener
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingContentPreviewData
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@Composable
internal fun PaymentButtonsPromoLayout(
    priceFormatted: TextUiModel,
    plan: DynamicPlanInstanceUiModel,
    actions: UpsellingScreen.Actions
) {
    Column(
        modifier = Modifier
    ) {
        Spacer(modifier = Modifier.weight(1f))

        UpsellingAutoRenewGenericPolicyText(
            modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
            planUiModel = plan,
            isShort = true
        )

        val userId = plan.userId.value
        val eventListener = UpsellingPaymentEventListener(
            context = LocalContext.current,
            userId = userId,
            telemetryPayload = plan.toTelemetryPayload(DynamicPlansVariant.PromoB),
            actions
        )
        val buttonCornerRadius = 8.dp.dpToPx()

        val btnText = remember { mutableStateOf("") }
        val newText = priceFormatted.string()
        LaunchedEffect(Unit) {
            btnText.value = newText
        }
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
                        this.setTextAppearance(R.style.ProtonTextView_Bold_LightPaymentButton)
                        this.cornerRadius = buttonCornerRadius
                        this.userId = userId
                        this.currency = plan.currency
                        this.cycle = plan.cycle.months
                        this.plan = plan.dynamicPlan
                        this.text = btnText.value
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
private fun PaymentButtonsPromoLayoutPreview() {
    ProtonTheme3 {
        Box(modifier = Modifier.height(180.dp)) {
            PaymentButtonsPromoLayout(
                priceFormatted = TextUiModel.Text("Get 1 month for EUR 0.99"),
                plan = UpsellingContentPreviewData.PromoList.shorterCycle,
                actions = UpsellingScreen.Actions.Empty
            )
        }
    }
}
