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

import ch.protonmail.android.mailevents.data.remote.model.MeasurementFieldKey.Common
import ch.protonmail.android.mailevents.data.remote.model.MeasurementFieldKey.FeatureUsage
import ch.protonmail.android.mailevents.data.remote.model.MeasurementFieldKey.Install
import ch.protonmail.android.mailevents.data.remote.model.MeasurementFieldKey.Signup
import ch.protonmail.android.mailevents.data.remote.model.MeasurementFieldKey.Subscription
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
    put(Common.OPEN_URI, metadata.openUri?.let { MeasurementValue.String(it) })
    put(Common.OS_VERSION, MeasurementValue.String(metadata.osVersion))
    put(Common.APP_VERSION, MeasurementValue.String(metadata.appVersion))
    put(Common.LOCALE, MeasurementValue.String(metadata.locale))
    put(Common.PLATFORM, MeasurementValue.String(metadata.platform))
    put(Common.MAKE, MeasurementValue.String(metadata.make))
    put(Common.MODEL, MeasurementValue.String(metadata.model))
    put(Common.LANGUAGE_CODE, MeasurementValue.String(metadata.languageCode))
    put(Common.APP_IDENTIFIER, MeasurementValue.String(metadata.appIdentifier))

    when (this@toMeasurementFields) {
        is EventPayload.Install -> {
            put(Install.IS_REINSTALL, MeasurementValue.Bool(isReinstall))
            put(Install.INSTALL_REF, installRef?.let { MeasurementValue.String(it) })
            put(Install.INSTALL_RECEIPT, installReceipt?.let { MeasurementValue.String(it) })
        }

        is EventPayload.Signup -> {
            put(Signup.REGISTRATION_METHOD, registrationMethod?.let { MeasurementValue.String(it) })
            put(Signup.REFERRAL_CODE, referralCode?.let { MeasurementValue.String(it) })
        }

        is EventPayload.Subscription -> {
            put(Subscription.CONTENT_LIST, MeasurementValue.String(contentList.joinToString(",")))
            put(Subscription.PRICE, MeasurementValue.String(price.toString()))
            put(Subscription.CURRENCY, MeasurementValue.String(currency))
            put(Subscription.CYCLE, MeasurementValue.String(cycle.toString()))
            put(Subscription.COUPON_CODE, couponCode?.let { MeasurementValue.String(it) })
            put(Subscription.TRANSACTION_ID, transactionId?.let { MeasurementValue.String(it) })
            put(Subscription.IS_FIRST_PURCHASE, MeasurementValue.Bool(isFirstPurchase))
            put(Subscription.IS_FREE_TO_PAID, MeasurementValue.Bool(isFreeToPaid))
        }

        is EventPayload.FeatureUsage -> {
            put(FeatureUsage.ACTION, MeasurementValue.String(action))
        }

        is EventPayload.Open,
        is EventPayload.OptOut -> Unit
    }
}

private object MeasurementFieldKey {

    object Common {

        const val OPEN_URI = "openuri"
        const val OS_VERSION = "os_version"
        const val APP_VERSION = "app_version"
        const val LOCALE = "locale"
        const val PLATFORM = "platform"
        const val MAKE = "make"
        const val MODEL = "model"
        const val LANGUAGE_CODE = "language_code"
        const val APP_IDENTIFIER = "app_identifier"
    }

    object Install {

        const val IS_REINSTALL = "is_reinstall"
        const val INSTALL_REF = "install_ref"
        const val INSTALL_RECEIPT = "install_receipt"
    }

    object Signup {

        const val REGISTRATION_METHOD = "registration_method"
        const val REFERRAL_CODE = "referral_code"
    }

    object Subscription {

        const val CONTENT_LIST = "content_list"
        const val PRICE = "price"
        const val CURRENCY = "currency"
        const val CYCLE = "cycle"
        const val COUPON_CODE = "coupon_code"
        const val TRANSACTION_ID = "transaction_id"
        const val IS_FIRST_PURCHASE = "is_first_purchase"
        const val IS_FREE_TO_PAID = "is_free_to_paid"
    }

    object FeatureUsage {

        const val ACTION = "action"
    }
}
