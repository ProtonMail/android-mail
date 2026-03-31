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

package ch.protonmail.android.mailtelemetry.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalGeneralDimensions
import ch.protonmail.android.mailcommon.data.mapper.LocalUpsellEntryPoint
import ch.protonmail.android.mailcommon.data.mapper.LocalUpsellFeatureFlags
import ch.protonmail.android.mailcommon.data.mapper.LocalUpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant

fun GeneralDimensions.toLocal() = LocalGeneralDimensions(
    upsellEntryPoint = this.upsellEntryPoint.toLocal(),
    planBeforeUpgrade = this.planBeforeUpgrade,
    modalVariant = this.modalVariant.toLocal(),
    upsellFeatureFlags = this.upsellFeatureFlags.toLocal()
)

fun UpsellEntryPoint.toLocal() = when (this) {
    UpsellEntryPoint.AUTO_DELETE_MESSAGES -> LocalUpsellEntryPoint.AUTO_DELETE_MESSAGES
    UpsellEntryPoint.CONTACT_GROUPS -> LocalUpsellEntryPoint.CONTACT_GROUPS
    UpsellEntryPoint.DOLLAR_PROMO -> LocalUpsellEntryPoint.DOLLAR_PROMO
    UpsellEntryPoint.FOLDERS_CREATION -> LocalUpsellEntryPoint.FOLDERS_CREATION
    UpsellEntryPoint.LABELS_CREATION -> LocalUpsellEntryPoint.LABELS_CREATION
    UpsellEntryPoint.MAILBOX_TOP_BAR -> LocalUpsellEntryPoint.MAILBOX_TOP_BAR
    UpsellEntryPoint.MAILBOX_TOP_BAR_PROMO -> LocalUpsellEntryPoint.MAILBOX_TOP_BAR_PROMO
    UpsellEntryPoint.NAVBAR_UPSELL -> LocalUpsellEntryPoint.NAVBAR_UPSELL
    UpsellEntryPoint.MOBILE_SIGNATURE_EDIT -> LocalUpsellEntryPoint.MOBILE_SIGNATURE_EDIT
    UpsellEntryPoint.POST_ONBOARDING -> LocalUpsellEntryPoint.POST_ONBOARDING
    UpsellEntryPoint.SCHEDULE_SEND -> LocalUpsellEntryPoint.SCHEDULE_SEND
    UpsellEntryPoint.SNOOZE -> LocalUpsellEntryPoint.SNOOZE
}

fun UpsellModalVariant.toLocal() = when (this) {
    UpsellModalVariant.COMPARISON_PLUS -> LocalUpsellModalVariant.COMPARISON_PLUS
    UpsellModalVariant.COMPARISON_UNLIMITED -> LocalUpsellModalVariant.COMPARISON_UNLIMITED
}

fun UpsellFeatureFlags.toLocal() = LocalUpsellFeatureFlags(
    parentFlagName = this.parentFlagName,
    childFlagName = this.childFlagName
)
