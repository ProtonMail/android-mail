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

import arrow.core.Either

/**
 * Device-local telemetry inspection, backed by the SDK's debug log. Only consumed by the internal
 * debug menu (dev/alpha builds); nothing in the product depends on it.
 *
 * Every call requires a logged-in user session, since the log lives in the user database.
 */
interface TelemetryDebugRepository {

    /** The captured events, newest first. */
    suspend fun getEvents(): Either<TelemetryDebugError, List<TelemetryDebugEvent>>

    suspend fun getSwitches(): Either<TelemetryDebugError, TelemetryDebugSwitches>

    suspend fun setCapturing(enabled: Boolean): Either<TelemetryDebugError, Unit>

    /** Halts/resumes delivery to the backend. Events keep being recorded either way. */
    suspend fun setSendingHalted(halted: Boolean): Either<TelemetryDebugError, Unit>

    /** Empties the debug log. Does not touch the production send queue nor the switches. */
    suspend fun clearCapturedEvents(): Either<TelemetryDebugError, Unit>

    /** Drops every event still waiting in the production send queue, so it is never delivered. */
    suspend fun discardPendingQueue(): Either<TelemetryDebugError, Unit>
}
