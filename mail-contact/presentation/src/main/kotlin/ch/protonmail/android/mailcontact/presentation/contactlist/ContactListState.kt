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
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect

sealed interface ContactListState {

    val isContactGroupsUpsellingVisible: Boolean

    data class Loading(
        val errorLoading: Effect<TextUiModel> = Effect.empty(),
        override val isContactGroupsUpsellingVisible: Boolean = false
    ) : ContactListState

    sealed interface Loaded : ContactListState {

        val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect>
        val openContactForm: Effect<Unit>
        val openContactGroupForm: Effect<Unit>
        val openImportContact: Effect<Unit>
        val openContactSearch: Effect<Boolean>
        val subscriptionError: Effect<TextUiModel>
        val upsellingInProgress: Effect<TextUiModel>
        val bottomSheetType: BottomSheetType

        data class Data(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty(),
            override val openContactSearch: Effect<Boolean> = Effect.empty(),
            override val subscriptionError: Effect<TextUiModel> = Effect.empty(),
            override val upsellingInProgress: Effect<TextUiModel> = Effect.empty(),
            override val isContactGroupsUpsellingVisible: Boolean = false,
            override val bottomSheetType: BottomSheetType = BottomSheetType.Menu,
            val contacts: List<ContactListItemUiModel>,
            val contactGroups: List<ContactGroupItemUiModel>
        ) : Loaded

        data class Empty(
            override val bottomSheetVisibilityEffect: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
            override val openContactForm: Effect<Unit> = Effect.empty(),
            override val openContactGroupForm: Effect<Unit> = Effect.empty(),
            override val openImportContact: Effect<Unit> = Effect.empty(),
            override val openContactSearch: Effect<Boolean> = Effect.empty(),
            override val subscriptionError: Effect<TextUiModel> = Effect.empty(),
            override val upsellingInProgress: Effect<TextUiModel> = Effect.empty(),
            override val isContactGroupsUpsellingVisible: Boolean = false,
            override val bottomSheetType: BottomSheetType = BottomSheetType.Menu
        ) : Loaded
    }

    sealed interface BottomSheetType {
        data object Menu : BottomSheetType
        data object Upselling : BottomSheetType
    }
}

