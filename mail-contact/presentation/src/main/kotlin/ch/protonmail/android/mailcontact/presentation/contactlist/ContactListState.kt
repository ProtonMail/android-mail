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

package ch.protonmail.android.mailcontact.presentation.contactlist

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel

sealed interface ContactListState {

    val isContactGroupsCrudEnabled: Boolean

    data class Loading(
        val errorLoading: Effect<TextUiModel> = Effect.empty(),
        override val isContactGroupsCrudEnabled: Boolean = false
    ) : ContactListState

    sealed interface Loaded : ContactListState {

        val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect>
        val openContactForm: Effect<Unit>
        val openContactGroupForm: Effect<Unit>
        val openImportContact: Effect<Unit>
        val subscriptionError: Effect<TextUiModel>

        data class Data(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty(),
            override val subscriptionError: Effect<TextUiModel> = Effect.empty(),
            override val isContactGroupsCrudEnabled: Boolean = false,
            val contacts: List<ContactListItemUiModel>,
            val contactGroups: List<ContactGroupItemUiModel>
        ) : Loaded

        data class Empty(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty(),
            override val subscriptionError: Effect<TextUiModel> = Effect.empty(),
            override val isContactGroupsCrudEnabled: Boolean = false
        ) : Loaded
    }
}

sealed interface BottomSheetVisibilityEffect {
    data object Show : BottomSheetVisibilityEffect
    data object Hide : BottomSheetVisibilityEffect
}
