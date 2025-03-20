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

package ch.protonmail.android.mailupselling.domain.model

import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint.Feature
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint.PostOnboarding

sealed interface UpsellingEntryPoint {

    sealed interface BottomSheet : UpsellingEntryPoint
    sealed interface Standalone : UpsellingEntryPoint

    sealed interface Feature : UpsellingEntryPoint {
        data object Mailbox : Standalone, Feature
        data object MailboxPromo : Standalone, Feature
        data object Navbar : Standalone, Feature
        data object ContactGroups : BottomSheet, Feature
        data object Labels : BottomSheet, Feature
        data object Folders : BottomSheet, Feature
        data object MobileSignature : BottomSheet, Feature
        data object AutoDelete : BottomSheet, Feature
    }

    data object PostOnboarding : UpsellingEntryPoint
}

fun UpsellingEntryPoint.getDimensionValue(): String = when (this) {
    Feature.ContactGroups -> "contact_groups"
    Feature.Folders -> "folders_creation"
    Feature.Labels -> "labels_creation"
    Feature.Mailbox -> "mailbox_top_bar"
    Feature.MobileSignature -> "mobile_signature_edit"
    Feature.AutoDelete -> "auto_delete_messages"
    PostOnboarding -> "post_onboarding"
    Feature.Navbar -> "navbar_upsell"
    Feature.MailboxPromo -> "mailbox_top_bar_promo"
}
