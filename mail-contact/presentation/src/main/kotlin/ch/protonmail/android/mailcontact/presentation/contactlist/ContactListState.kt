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

    data class Loading(
        val errorLoading: Effect<TextUiModel> = Effect.empty()
    ) : ContactListState

    sealed interface ListLoaded : ContactListState {

        val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect>
        val openContactForm: Effect<Unit>
        val openContactGroupForm: Effect<Unit>
        val openImportContact: Effect<Unit>

        data class Data(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty(),
            val contacts: List<ContactListItemUiModel>,
            val contactGroups: List<ContactGroupItemUiModel>
        ) : ListLoaded

        data class Empty(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty()
        ) : ListLoaded
    }
}

sealed interface BottomSheetVisibilityEffect {
    object Show : BottomSheetVisibilityEffect
    object Hide : BottomSheetVisibilityEffect
}
