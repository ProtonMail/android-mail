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

package ch.protonmail.android.mailcontentsearch.presentation.settings.mapper

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState

internal fun ContentIndexingState.toPercentage(): Double? = when (this) {
    is ContentIndexingState.Running -> percentage.takeIf { !isBackfillDone }
    ContentIndexingState.Idle,
    ContentIndexingState.Initializing,
    ContentIndexingState.WaitingForUnmeteredConnection,
    ContentIndexingState.Completed,
    ContentIndexingState.Cancelled,
    ContentIndexingState.Failed -> null
}

internal fun ContentIndexingState.isActive(): Boolean = when (this) {
    ContentIndexingState.Initializing,
    // Parked, not finished: the account still has a backfill waiting on a connection it may use.
    ContentIndexingState.WaitingForUnmeteredConnection -> true

    is ContentIndexingState.Running -> !isBackfillDone

    ContentIndexingState.Idle,
    ContentIndexingState.Completed,
    ContentIndexingState.Cancelled,
    ContentIndexingState.Failed -> false
}

internal fun ContentIndexingState.isWaitingForUnmeteredConnection(): Boolean =
    this is ContentIndexingState.WaitingForUnmeteredConnection

/**
 * Rust keeps the account in `Running` once the backfill has caught up - it stays subscribed for new
 * mail - and it does not reliably publish `Completed`. Left alone the settings screen would sit on
 * "Preparing 100.00%", which reads as stuck rather than finished.
 */
private val ContentIndexingState.Running.isBackfillDone: Boolean
    get() = percentage >= FULLY_INDEXED_PERCENTAGE

private const val FULLY_INDEXED_PERCENTAGE = 100.0
