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

package ch.protonmail.android.mailcontentsearch.domain.model

sealed interface ContentIndexingState {
    data object Idle : ContentIndexingState
    data object Initializing : ContentIndexingState

    /**
     * Rust has parked the account's queue because the connection is metered and the user has not
     * allowed indexing over mobile data. It resumes on its own once an unmetered connection is
     * back, so this is a pause rather than an end.
     */
    data object WaitingForUnmeteredConnection : ContentIndexingState
    data class Running(val percentage: Double) : ContentIndexingState
    data object Completed : ContentIndexingState
    data object Cancelled : ContentIndexingState
    data object Failed : ContentIndexingState
}

/**
 * How far this account's backfill has got, or null when there is no determinate progress to show.
 *
 * Rust keeps the account in [ContentIndexingState.Running] once the backfill has caught up - it
 * stays subscribed for new mail - and it does not reliably publish
 * [ContentIndexingState.Completed], so a percentage at or above 100 is read as done rather than
 * reported as "100%" forever. Shared by every surface that shows the account's progress, so the
 * settings screen and the indexing notification cannot disagree about it.
 */
val ContentIndexingState.backfillPercentage: Double?
    get() = (this as? ContentIndexingState.Running)?.percentage?.takeIf { it < FULLY_INDEXED_PERCENTAGE }

private const val FULLY_INDEXED_PERCENTAGE = 100.0
