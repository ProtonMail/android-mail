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

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.domain.model.UpsellingActions
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.toTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.eventlistener.UpsellingPaymentEventListener
import me.proton.core.payment.presentation.view.ProtonPaymentButton

@Composable
internal fun OnboardingPayButton(
    planInstanceUiModel: DynamicPlanInstanceUiModel,
    actions: OnboardingPayButton.Actions
) {
    val buttonCornerRadius = UpsellingLayoutValues.Onboarding.payButtonCornerRadius
    var maxWrappedViewSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { maxWrappedViewSize = it.size }
    ) {

        val eventListener = UpsellingPaymentEventListener(
            context = LocalContext.current,
            userId = planInstanceUiModel.userId.value,
            telemetryPayload = planInstanceUiModel.toTelemetryPayload(),
            actions
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.OnboardingUpsellButtonHeight)
                .align(Alignment.Center),
            factory = { ctx ->
                ProtonPaymentButton(ContextThemeWrapper(ctx, R.style.ProtonTheme)).apply {
                    this.id = planInstanceUiModel.viewId
                }
            },
            update = { button ->
                button.apply {
                    this.cornerRadius = buttonCornerRadius
                    this.userId = planInstanceUiModel.userId.value
                    this.currency = planInstanceUiModel.currency
                    this.cycle = planInstanceUiModel.cycle.months
                    this.plan = planInstanceUiModel.dynamicPlan
                    this.text = context.getString(R.string.upselling_get_button, planInstanceUiModel.name)
                    setOnEventListener(eventListener)
                }
            }
        )
    }
}

object OnboardingPayButton {

    data class Actions(
        override val onError: (String) -> Unit,
        override val onUpgradeAttempt: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgradeCancelled: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgradeErrored: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onSuccess: (UpsellingTelemetryTargetPlanPayload) -> Unit,
        override val onUpgrade: (String) -> Unit,
        override val onDismiss: () -> Unit
    ) : UpsellingActions {

        companion object {

            val Empty = Actions(
                onUpgradeAttempt = {},
                onError = {},
                onUpgrade = {},
                onUpgradeCancelled = {},
                onUpgradeErrored = {},
                onSuccess = {},
                onDismiss = {}
            )
        }
    }
}
