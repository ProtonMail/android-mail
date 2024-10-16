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

sealed interface UpsellingEntryPoint {

    sealed interface BottomSheet : UpsellingEntryPoint {

        data object Mailbox : BottomSheet
        data object ContactGroups : BottomSheet
        data object Labels : BottomSheet
        data object Folders : BottomSheet
        data object MobileSignature : BottomSheet
        data object AutoDelete : BottomSheet

    }

    data object PostOnboarding : UpsellingEntryPoint

    fun UpsellingEntryPoint.getDimensionValue(): String = when (this) {
        BottomSheet.ContactGroups -> "contact_groups"
        BottomSheet.Folders -> "folders_creation"
        BottomSheet.Labels -> "labels_creation"
        BottomSheet.Mailbox -> "mailbox_top_bar"
        BottomSheet.MobileSignature -> "mobile_signature_edit"
        BottomSheet.AutoDelete -> "auto_delete_messages"
        PostOnboarding -> "post_onboarding"
    }
}
