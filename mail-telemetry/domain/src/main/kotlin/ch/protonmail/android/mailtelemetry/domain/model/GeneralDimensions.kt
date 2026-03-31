/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailtelemetry.domain.model

data class GeneralDimensions(
    val upsellEntryPoint: UpsellEntryPoint,
    val planBeforeUpgrade: String,
    val modalVariant: UpsellModalVariant,
    val upsellFeatureFlags: UpsellFeatureFlags
)

data class UpsellFeatureFlags(
    val parentFlagName: String,
    val childFlagName: String
)

enum class UpsellEntryPoint {

    AUTO_DELETE_MESSAGES,
    CONTACT_GROUPS,
    DOLLAR_PROMO,
    FOLDERS_CREATION,
    LABELS_CREATION,
    MAILBOX_TOP_BAR,
    MAILBOX_TOP_BAR_PROMO,
    NAVBAR_UPSELL,
    MOBILE_SIGNATURE_EDIT,
    POST_ONBOARDING,
    SCHEDULE_SEND,
    SNOOZE
}

enum class UpsellModalVariant {

    COMPARISON_PLUS,
    COMPARISON_UNLIMITED
}
