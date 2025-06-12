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

package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentEvent
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightUIState
import javax.inject.Inject

internal class DriveSpotlightContentReducer @Inject constructor() {

    fun newStateFrom(event: DriveSpotlightContentEvent): DriveSpotlightUIState = when (event) {
        is DriveSpotlightContentEvent.StorageError -> DriveSpotlightUIState.Error(
            error = Effect.of(
                TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id)
            )
        )

        is DriveSpotlightContentEvent.DataLoaded -> {
            DriveSpotlightUIState.Data(event.storageGB?.takeIf { it > MIN_STORAGE_GB_THRESHOLD }?.toInt())
        }
    }
}

private const val MIN_STORAGE_GB_THRESHOLD = 1.5f
