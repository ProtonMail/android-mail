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

/**
 * Common metadata present in all event payloads.
 * Encapsulates device, app, and session information.
 */
data class EventPayloadMetadata(
    val asid: String,
    val appPackageName: String,
    val openUri: String?,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val platform: String,
    val make: String,
    val model: String,
    val languageCode: String,
    val appIdentifier: String
)

/**
 * Sealed class hierarchy representing event payloads sent to the backend.
 */
sealed class EventPayload {

    abstract val eventType: String
    abstract val metadata: EventPayloadMetadata

    data class Install(
        override val metadata: EventPayloadMetadata,
        val isReinstall: Boolean,
        val installRef: String?,
        val installReceipt: String?
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_INSTALL
    }

    data class Open(
        override val metadata: EventPayloadMetadata,
        val isNewSession: Boolean
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_OPEN
    }

    data class Signup(
        override val metadata: EventPayloadMetadata,
        val registrationMethod: String?,
        val referralCode: String?
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_SIGNUP
    }

    data class Subscription(
        override val metadata: EventPayloadMetadata,
        val contentList: List<String>,
        val price: Double,
        val currency: String,
        val cycle: Int,
        val couponCode: String?,
        val transactionId: String?,
        val isFirstPurchase: Boolean,
        val isFreeToPaid: Boolean
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_SUB
    }

    data class FeatureUsage(
        override val metadata: EventPayloadMetadata,
        val action: String
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_FEATURE_USAGE
    }

    data class OptOut(
        override val metadata: EventPayloadMetadata
    ) : EventPayload() {

        override val eventType: String = EVENT_TYPE_OPT_OUT
    }

    private companion object {

        const val EVENT_TYPE_INSTALL = "install"
        const val EVENT_TYPE_OPEN = "open"
        const val EVENT_TYPE_SIGNUP = "signup"
        const val EVENT_TYPE_SUB = "sub"
        const val EVENT_TYPE_FEATURE_USAGE = "feature_usage"
        const val EVENT_TYPE_OPT_OUT = "opt_out"
    }
}
