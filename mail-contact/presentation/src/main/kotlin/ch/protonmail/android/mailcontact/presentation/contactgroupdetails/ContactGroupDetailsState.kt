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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsUiModel

sealed class ContactGroupDetailsState(
    open val close: Effect<Unit>
) {

    data class Loading(
        override val close: Effect<Unit> = Effect.empty(),
        val errorLoading: Effect<TextUiModel> = Effect.empty()
    ) : ContactGroupDetailsState(close)

    data class Data(
        override val close: Effect<Unit> = Effect.empty(),
        val isSendEnabled: Boolean,
        val contactGroup: ContactGroupDetailsUiModel,
        val openComposer: Effect<List<String>> = Effect.empty(),
        val deleteDialogState: DeleteDialogState,
        val deletionSuccess: Effect<TextUiModel> = Effect.empty(),
        val deletionError: Effect<TextUiModel> = Effect.empty()
    ) : ContactGroupDetailsState(Effect.empty())
}

