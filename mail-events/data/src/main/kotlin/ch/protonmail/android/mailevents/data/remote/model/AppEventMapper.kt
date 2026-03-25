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

import ch.protonmail.android.mailevents.domain.model.AppEvent

/**
 * Convert an [AppEvent] with its [EventMetadata] into the appropriate [EventPayload].
 * This resolves the correct payload type based on the event type.
 */
fun AppEvent.toPayload(metadata: EventMetadata): EventPayload = when (this) {
    is AppEvent.Install -> metadata.toInstallPayload(this)
    is AppEvent.AppOpen -> metadata.toOpenPayload(this)
    is AppEvent.AccountCreated -> metadata.toSignupPayload(this)
    is AppEvent.Subscription -> metadata.toSubscriptionPayload(this)
    is AppEvent.MessageSent -> metadata.toFeatureUsagePayload(ACTION_SENT_MESSAGE)
    is AppEvent.OnboardingCompleted -> metadata.toFeatureUsagePayload(ACTION_ONBOARDING_COMPLETED)
    is AppEvent.OfferReceived -> metadata.toFeatureUsagePayload("${ACTION_OFFER_RECEIVED}_${this.offerId}")
    is AppEvent.OfferClicked -> metadata.toFeatureUsagePayload("${ACTION_OFFER_CLICKED}_${this.offerId}")
    is AppEvent.SubscriptionPaywallShown -> metadata.toFeatureUsagePayload(ACTION_SUBSCRIPTION_PAYWALL)
    is AppEvent.SubscriptionOnboardingShown -> metadata.toFeatureUsagePayload(ACTION_SUBSCRIPTION_ONBOARDING)
    is AppEvent.SubscriptionManualShown -> metadata.toFeatureUsagePayload(ACTION_SUBSCRIPTION_MANUAL)
    is AppEvent.OptOut -> metadata.toOptOutPayload()
}

private fun EventMetadata.toPayloadMetadata() = EventPayloadMetadata(
    asid = asid,
    appPackageName = appPackageName,
    openUri = null,
    osVersion = deviceInfo.osVersion,
    appVersion = appVersion,
    locale = deviceInfo.locale,
    platform = deviceInfo.platform,
    make = deviceInfo.make,
    model = deviceInfo.model,
    languageCode = deviceInfo.languageCode,
    appIdentifier = appIdentifier
)

private fun EventMetadata.toInstallPayload(event: AppEvent.Install) = EventPayload.Install(
    metadata = toPayloadMetadata(),
    isReinstall = event.isReinstall,
    installRef = event.installRef,
    installReceipt = event.installReceipt
)

private fun EventMetadata.toOpenPayload(event: AppEvent.AppOpen) = EventPayload.Open(
    metadata = toPayloadMetadata(),
    isNewSession = event.isNewSession
)

private fun EventMetadata.toSignupPayload(event: AppEvent.AccountCreated) = EventPayload.Signup(
    metadata = toPayloadMetadata(),
    registrationMethod = event.registrationMethod,
    referralCode = event.referralCode
)

private fun EventMetadata.toSubscriptionPayload(event: AppEvent.Subscription) = EventPayload.Subscription(
    metadata = toPayloadMetadata(),
    contentList = event.contentList,
    price = event.price,
    currency = event.currency,
    cycle = event.cycle,
    couponCode = event.couponCode,
    transactionId = event.transactionId,
    isFirstPurchase = event.isFirstPurchase,
    isFreeToPaid = event.isFreeToPaid
)

private fun EventMetadata.toFeatureUsagePayload(action: String) = EventPayload.FeatureUsage(
    metadata = toPayloadMetadata(),
    action = action
)

private fun EventMetadata.toOptOutPayload() = EventPayload.OptOut(
    metadata = toPayloadMetadata()
)

private const val ACTION_SENT_MESSAGE = "first_message_sent"
private const val ACTION_ONBOARDING_COMPLETED = "onboarding_completed"
private const val ACTION_OFFER_RECEIVED = "offer_received"
private const val ACTION_OFFER_CLICKED = "offer_clicked"
private const val ACTION_SUBSCRIPTION_PAYWALL = "in_app_subscription_paywall"
private const val ACTION_SUBSCRIPTION_ONBOARDING = "in_app_subscription_onboarding"
private const val ACTION_SUBSCRIPTION_MANUAL = "in_app_subscription_manual"
