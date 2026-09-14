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

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartOutcome
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.FULLY_INDEXED_PERCENTAGE
import uniffi.mail_uniffi.SyncDriverEvent
import uniffi.mail_uniffi.SyncEvent
import uniffi.mail_uniffi.SyncStartOutcome
import uniffi.mail_uniffi.SyncStatus
import uniffi.mail_uniffi.SyncWorkerEvent

internal fun SyncStartOutcome.toStartOutcome(): ContentIndexingStartOutcome = when (this) {
    SyncStartOutcome.STARTED -> ContentIndexingStartOutcome.Started
    SyncStartOutcome.ONGOING -> ContentIndexingStartOutcome.AlreadyRunning
    SyncStartOutcome.DISABLED -> ContentIndexingStartOutcome.Refused
    SyncStartOutcome.COMPLETED -> ContentIndexingStartOutcome.AlreadyCompleted
    SyncStartOutcome.PAUSED -> ContentIndexingStartOutcome.Paused
}

internal fun SyncStatus.toIndexingState(progress: Double?): ContentIndexingState = when (this) {
    SyncStatus.PENDING -> ContentIndexingState.Idle
    SyncStatus.ONGOING -> progress.toRunningState()
    SyncStatus.COMPLETED -> ContentIndexingState.Completed
    SyncStatus.FAILURE -> ContentIndexingState.Failed
    SyncStatus.PAUSED -> ContentIndexingState.Paused(progress)
}

/**
 * What an account that Rust reports as syncing is really doing.
 *
 * `Ongoing` says only that a sync task is alive: Rust records it when the task starts and never
 * revises it, so an account that has since finished its backfill and is only subscribed for new mail
 * keeps reporting `Ongoing` for the life of the process. The progress counters can tell them apart.
 *
 * No progress at all means Rust has not sized the backfill yet: Initializing rather than
 * Running(0.0), so the UI shows a "preparing" placeholder instead of a misleading 0%.
 */
private fun Double?.toRunningState(): ContentIndexingState = when {
    this == null -> ContentIndexingState.Initializing
    this >= FULLY_INDEXED_PERCENTAGE -> ContentIndexingState.Completed
    else -> ContentIndexingState.Running(this)
}

/**
 * Returns null for events that don't map to a UI-visible state change (e.g. per-item
 * worker events), so callers can filter them out of the stream.
 */
internal fun SyncEvent.toIndexingState(stalledPercentage: Double? = null): ContentIndexingState? = when (this) {
    is SyncEvent.Started -> ContentIndexingState.Initializing

    // Published once when the queue is parked; Rust re-emits the last Progress on resume, so this
    // state clears itself without any prompting from us.
    // [stalledPercentage] has to be supplied: the event carries no progress of its own.
    is SyncEvent.WaitingForUnmeteredConnection ->
        ContentIndexingState.WaitingForUnmeteredConnection(stalledPercentage)
    is SyncEvent.Progress -> this.v1.percentage.toRunningState()

    // Published when the user pauses the account. Rust keeps the pause across restarts, so this
    // state only clears when something starts the account again.
    // [stalledPercentage] has to be supplied here too: the event carries no progress of its own.
    is SyncEvent.Paused -> ContentIndexingState.Paused(stalledPercentage)
    is SyncEvent.Completed -> ContentIndexingState.Completed
    is SyncEvent.Stopped -> ContentIndexingState.Cancelled
    is SyncEvent.Driver -> when (this.v1) {
        is SyncDriverEvent.Failure -> ContentIndexingState.Failed
        is SyncDriverEvent.Completed -> null
    }

    // A failing worker ends the account's sync exactly as a failing driver does - Rust records the
    // failure and stops - so it cannot be dropped as a per-item event: the stream would then fall
    // silent on the last percentage and never report the account as failed.
    is SyncEvent.Worker -> when (this.v1) {
        is SyncWorkerEvent.Failure -> ContentIndexingState.Failed
        is SyncWorkerEvent.Processed -> null
    }
}

internal fun SyncEvent.isTerminal(): Boolean = when (this) {
    is SyncEvent.Completed,
    is SyncEvent.Stopped,

    // Rust stops the account's sync on a pause and remembers it, so no further event is coming
    // until the account is started again.
    is SyncEvent.Paused -> true

    is SyncEvent.Driver -> this.v1 is SyncDriverEvent.Failure
    is SyncEvent.Worker -> this.v1 is SyncWorkerEvent.Failure
    is SyncEvent.Started,
    is SyncEvent.WaitingForUnmeteredConnection,
    is SyncEvent.Progress -> false
}
