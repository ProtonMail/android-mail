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

package ch.protonmail.android.mailevents.domain.model

sealed interface AppEvent {

    data class Install(
        val isReinstall: Boolean,
        val installReceipt: String?,
        val installRef: String?
    ) : AppEvent

    data class AppOpen(val isNewSession: Boolean) : AppEvent

    data class Subscription(
        val contentList: List<String>,
        val price: Double,
        val currency: String,
        val cycle: Int,
        val couponCode: String?,
        val transactionId: String?,
        val isFirstPurchase: Boolean,
        val isFreeToPaid: Boolean
    ) : AppEvent

    data class AccountCreated(
        val registrationMethod: String?,
        val referralCode: String?
    ) : AppEvent

    data object OnboardingCompleted : AppEvent

    data object MessageSent : AppEvent

    data object SubscriptionPaywallShown : AppEvent

    data object SubscriptionOnboardingShown : AppEvent

    data object SubscriptionManualShown : AppEvent

    data class OfferReceived(val offerId: String) : AppEvent

    data class OfferClicked(val offerId: String) : AppEvent

    data object OptOut : AppEvent
}
