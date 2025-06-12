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

package ch.protonmail.android.mailupselling.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

internal sealed interface DriveSpotlightUIState {
    data object Loading : DriveSpotlightUIState
    data class Data(val storageGB: Int?) : DriveSpotlightUIState
    data class Error(val error: Effect<TextUiModel>) : DriveSpotlightUIState
}

internal sealed interface DriveSpotlightContentOperation

internal sealed interface DriveSpotlightContentViewEvent : DriveSpotlightContentOperation {
    data object ContentShown : DriveSpotlightContentViewEvent
    data object OpenDriveClicked : DriveSpotlightContentViewEvent
}

internal sealed interface DriveSpotlightContentEvent : DriveSpotlightContentOperation {

    data class DataLoaded(val storageGB: Float?) : DriveSpotlightContentEvent

    data object StorageError : DriveSpotlightContentEvent
}
