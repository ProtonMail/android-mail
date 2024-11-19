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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ColorHexWithName
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModel

sealed interface ContactGroupFormState {

    val close: Effect<Unit>

    data class Loading(
        override val close: Effect<Unit> = Effect.empty(),
        val errorLoading: Effect<TextUiModel> = Effect.empty()
    ) : ContactGroupFormState

    data class Data(
        override val close: Effect<Unit> = Effect.empty(),
        val contactGroup: ContactGroupFormUiModel,
        val colors: List<ColorHexWithName>,
        val displaySaveLoader: Boolean = false,
        val isSaveEnabled: Boolean = false,
        val closeWithSuccess: Effect<TextUiModel> = Effect.empty(),
        val showErrorSnackbar: Effect<TextUiModel> = Effect.empty(),
        val deleteDialogState: DeleteDialogState = DeleteDialogState.Hidden,
        val deletionSuccess: Effect<TextUiModel> = Effect.empty(),
        val deletionError: Effect<TextUiModel> = Effect.empty(),
        val subscriptionNeededError: Effect<TextUiModel> = Effect.empty()
    ) : ContactGroupFormState {
        val displayDeleteButton = contactGroup.id != null
    }
}
