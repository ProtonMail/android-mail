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

package ch.protonmail.android.mailevents.data.remote.model

import uniffi.proton_mail_uniffi.MeasurementEventType
import uniffi.proton_mail_uniffi.MeasurementValue

fun EventPayload.toMeasurementEventType(): MeasurementEventType = when (this) {
    is EventPayload.Install -> MeasurementEventType.Install
    is EventPayload.Open -> MeasurementEventType.Open(newSession = isNewSession)
    is EventPayload.Signup -> MeasurementEventType.Signup
    is EventPayload.Subscription -> MeasurementEventType.Sub
    is EventPayload.FeatureUsage -> MeasurementEventType.FeatureUsage
    is EventPayload.OptOut -> MeasurementEventType.OptOut
}

fun EventPayload.toMeasurementFields(): Map<String, MeasurementValue?> = buildMap {
    put("open_uri", metadata.openUri?.let { MeasurementValue.String(it) })

    when (this@toMeasurementFields) {
        is EventPayload.Install -> {
            put("is_reinstall", MeasurementValue.Bool(isReinstall))
            put("install_ref", installRef?.let { MeasurementValue.String(it) })
            put("install_receipt", installReceipt?.let { MeasurementValue.String(it) })
        }

        is EventPayload.Signup -> {
            put("registration_method", registrationMethod?.let { MeasurementValue.String(it) })
            put("referral_code", referralCode?.let { MeasurementValue.String(it) })
        }

        is EventPayload.Subscription -> {
            put("content_list", MeasurementValue.String(contentList.joinToString(",")))
            put("price", MeasurementValue.String(price.toString()))
            put("currency", MeasurementValue.String(currency))
            put("cycle", MeasurementValue.String(cycle.toString()))
            put("coupon_code", couponCode?.let { MeasurementValue.String(it) })
            put("transaction_id", transactionId?.let { MeasurementValue.String(it) })
            put("is_first_purchase", MeasurementValue.Bool(isFirstPurchase))
            put("is_free_to_paid", MeasurementValue.Bool(isFreeToPaid))
        }

        is EventPayload.FeatureUsage -> {
            put("action", MeasurementValue.String(action))
        }

        is EventPayload.Open,
        is EventPayload.OptOut -> Unit
    }
}
