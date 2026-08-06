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

import me.proton.core.domain.entity.UserId

/**
 * What the session-wide indexing orchestrator is doing.
 *
 * Every case except [Progress] means the orchestrator has nothing left to drive, so they double as
 * the worker's exit conditions - it holds a foreground service open only while progress is arriving.
 */
sealed interface ContentIndexingActivity {

    /**
     * @param activeUserId the account being indexed, or `null` while the orchestrator is between
     *  accounts. Rust reports it as an opaque id, so it is not guaranteed to still be logged in.
     * @param completedUsers / [userCount] account-level progress, safe to show. The overall
     *  percentage is deliberately absent: Rust sums it across every account's totals, so it jumps
     *  whenever an account is added or removed.
     */
    data class Progress(
        val activeUserId: UserId?,
        val completedUsers: Long,
        val userCount: Long
    ) : ContentIndexingActivity

    /** Nothing to index right now, but accounts may become eligible later. */
    data object WaitingOnUsers : ContentIndexingActivity

    /** The backfill is done for [userId] and it has moved to keeping up with new mail. */
    data class ForwardMode(val userId: UserId) : ContentIndexingActivity

    data object Stopped : ContentIndexingActivity

    data class Failed(val reason: String) : ContentIndexingActivity
}
