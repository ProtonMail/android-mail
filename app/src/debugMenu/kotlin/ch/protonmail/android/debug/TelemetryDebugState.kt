/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.debug

import androidx.compose.runtime.Stable

internal sealed interface TelemetryDebugState {

    data object Loading : TelemetryDebugState

    data class Failed(val message: String) : TelemetryDebugState

    @Stable
    data class Data(
        val capturing: Boolean,
        val sendingHalted: Boolean,
        val searchQuery: String,
        /** Events matching [searchQuery], newest first. */
        val events: List<TelemetryDebugEventUiModel>,
        val capturedCount: Int,
        /** Message of the last failed action, if any. Cleared on the next successful refresh. */
        val actionError: String?
    ) : TelemetryDebugState
}

internal data class TelemetryDebugEventUiModel(
    val id: String,
    val name: String,
    val measurementGroup: String,
    val values: List<TelemetryDebugEntryUiModel>,
    val dimensions: List<TelemetryDebugEntryUiModel>,
    val payload: String
)

internal data class TelemetryDebugEntryUiModel(val key: String, val value: String)
