/*
 * Copyright (c) 2025 Proton Technologies AG
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

package me.proton.android.core.payment.data

import me.proton.android.core.payment.data.model.toExternal
import me.proton.android.core.payment.domain.PaymentMetricsTracker
import me.proton.android.core.payment.domain.model.PaymentObservabilityMetric
import me.proton.android.core.payment.domain.model.PaymentObservabilityValue
import uniffi.mail_uniffi.sendPaymentObservabilityMetric
import javax.inject.Inject
import javax.inject.Singleton
import uniffi.mail_uniffi.PaymentObservabilityMetric as MetricRust

@Singleton
class PaymentMetricsTrackerRust @Inject constructor() : PaymentMetricsTracker {

    override fun track(metric: PaymentObservabilityMetric, value: PaymentObservabilityValue) {
        val metricWithValue = when (metric) {
            PaymentObservabilityMetric.IAP_SUBSCRIBE -> MetricRust.IapSubscribe(value.toExternal())
            PaymentObservabilityMetric.SEND_PAYMENT_TOKEN -> MetricRust.SendPaymentToken(value.toExternal())
            PaymentObservabilityMetric.CREATE_SUBSCRIPTION -> MetricRust.CreateSubscription(value.toExternal())
            PaymentObservabilityMetric.GET_SUBSCRIPTION -> MetricRust.GetSubscription(value.toExternal())
            PaymentObservabilityMetric.GET_PLANS -> MetricRust.GetPlans(value.toExternal())
            PaymentObservabilityMetric.GET_PAYMENTS_STATUS -> MetricRust.GetPlans(value.toExternal())
        }

        sendPaymentObservabilityMetric(metricWithValue)
    }
}
