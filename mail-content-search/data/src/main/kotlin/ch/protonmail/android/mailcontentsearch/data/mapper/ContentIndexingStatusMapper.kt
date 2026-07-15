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

package ch.protonmail.android.mailcontentsearch.data.mapper

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import uniffi.mail_uniffi.SyncDriverEvent
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncStatus

internal fun SyncStatus.toIndexingState(progress: Double?): ContentIndexingState = when (this) {
    SyncStatus.PENDING -> ContentIndexingState.Idle
    // No known progress yet (e.g. a fresh one-shot read before any live event arrived):
    // Initializing rather than Running(0.0), so the UI shows a "preparing" placeholder
    // instead of a misleading 0%.
    SyncStatus.ONGOING -> progress?.let { ContentIndexingState.Running(it) } ?: ContentIndexingState.Initializing
    SyncStatus.COMPLETED -> ContentIndexingState.Completed
}

/**
 * Returns null for events that don't map to a UI-visible state change (e.g. per-item
 * worker events), so callers can filter them out of the stream.
 */
internal fun SyncEvent.toIndexingState(): ContentIndexingState? = when (this) {
    is SyncEvent.Started -> ContentIndexingState.Initializing
    is SyncEvent.Progress -> ContentIndexingState.Running(this.v1.percentage)
    is SyncEvent.Completed -> ContentIndexingState.Completed
    is SyncEvent.Stopped -> ContentIndexingState.Cancelled
    is SyncEvent.Driver -> when (this.v1) {
        is SyncDriverEvent.Failure -> ContentIndexingState.Failed
        is SyncDriverEvent.Completed -> null
    }

    is SyncEvent.Worker -> null
}

internal fun SyncEvent.isTerminal(): Boolean = when (this) {
    is SyncEvent.Completed,
    is SyncEvent.Stopped -> true

    is SyncEvent.Driver -> this.v1 is SyncDriverEvent.Failure
    is SyncEvent.Started,
    is SyncEvent.Progress,
    is SyncEvent.Worker -> false
}
