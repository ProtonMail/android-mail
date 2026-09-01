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

package ch.protonmail.android.mailtelemetry.debug.domain

/**
 * A telemetry event as captured by the SDK's device-local debug log.
 */
data class TelemetryDebugEvent(
    val id: String,
    val measurementGroup: String,
    val name: String,
    val values: Map<String, Double>,
    val dimensions: Map<String, String>
)

/**
 * The two device-local telemetry debug switches, as persisted by the SDK. Both survive an app
 * restart, so a capture session outlives the screen that started it.
 */
data class TelemetryDebugSwitches(val capturing: Boolean, val sendingHalted: Boolean)

/** Failure of a telemetry debug call. The raw SDK message is kept verbatim: this is a QA tool. */
data class TelemetryDebugError(val message: String)
