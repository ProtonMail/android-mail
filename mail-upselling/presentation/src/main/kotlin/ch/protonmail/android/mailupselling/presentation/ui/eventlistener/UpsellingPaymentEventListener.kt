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

package ch.protonmail.android.mailupselling.presentation.ui.eventlistener

import android.content.Context
import android.content.Intent
import android.widget.Toast
import ch.protonmail.android.mailupselling.domain.model.UpsellingActions
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.R
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.presentation.view.ProtonPaymentEventListener
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.presentation.entity.PlanInput
import me.proton.core.plan.presentation.ui.StartDynamicUpgradePlan
import me.proton.core.plan.presentation.ui.StartUnredeemedPurchase
import timber.log.Timber

internal class UpsellingPaymentEventListener(
    private val context: Context,
    private val userId: UserId,
    private val telemetryPayload: UpsellingTelemetryTargetPlanPayload,
    private val actions: UpsellingActions
) : ProtonPaymentEventListener {

    override fun invoke(event: ProtonPaymentEvent) {
        when (event) {
            is ProtonPaymentEvent.Error.Generic,
            ProtonPaymentEvent.Error.EmptyCustomerId,
            ProtonPaymentEvent.Error.GoogleProductDetailsNotFound,
            ProtonPaymentEvent.Error.PurchaseNotFound,
            ProtonPaymentEvent.Error.UnrecoverableBillingError,
            ProtonPaymentEvent.Error.UnsupportedPaymentProvider -> {
                logEvent("Error while performing 1 click upselling flow - ${event::class.java}")

                actions.onError(context.getString(R.string.upselling_snackbar_upgrade_error_generic))
                actions.onUpgradeErrored(telemetryPayload)
                actions.onDismiss()
            }

            is ProtonPaymentEvent.Error.SubscriptionManagedByOtherApp -> {
                logEvent("Subscription managed by other app - ${event::class.java}")

                actions.onError(context.getString(R.string.upselling_snackbar_upgrade_error_generic))
                actions.onUpgradeErrored(telemetryPayload)
                actions.onDismiss()
            }

            is ProtonPaymentEvent.GiapSuccess -> {
                actions.onUpgrade(context.getString(R.string.upselling_snackbar_upgrade_in_progress))
                actions.onSuccess(telemetryPayload)
                actions.onDismiss()
            }

            is ProtonPaymentEvent.Error.GiapUnredeemed -> {
                actions.onDismiss()
                logEvent("GiapUnredeemed received while performing 1 click upselling flow.")

                val intent = StartUnredeemedPurchase.createIntent(context, Unit).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
            }

            ProtonPaymentEvent.Error.RecoverableBillingError -> {
                logEvent("RecoverableBillingEvent received while performing 1 click upselling flow.")

                Toast.makeText(
                    context,
                    context.getString(R.string.upselling_snackbar_upgrade_error_recoverable),
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ProtonPaymentEvent.StartRegularBillingFlow -> {
                logEvent("Starting regular flow while performing 1 click upselling flow.")
                // Unsupported from 1 click bottom sheet, redirecting to Subscription page.
                actions.onDismiss()

                val intent = StartDynamicUpgradePlan.createIntent(context, PlanInput(userId.id))
                context.startActivity(intent)
            }

            ProtonPaymentEvent.Loading -> {
                actions.onUpgradeAttempt(telemetryPayload)
            }

            ProtonPaymentEvent.Error.UserCancelled -> {
                actions.onUpgradeCancelled(telemetryPayload)
            }
        }
    }

    private fun logEvent(message: String) {
        Timber.tag("UpsellingPaymentEventListener").w(message)
    }
}

