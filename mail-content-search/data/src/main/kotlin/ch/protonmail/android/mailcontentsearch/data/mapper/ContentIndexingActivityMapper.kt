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

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingStartSummary
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.SyncOrchestartorStartStats
import uniffi.mail_uniffi.SyncOrchestratorEvent

internal fun SyncOrchestartorStartStats.toStartSummary(): ContentIndexingStartSummary = ContentIndexingStartSummary(
    pending = pending.toLong(),
    ongoing = ongoing.toLong(),
    completed = completed.toLong(),
    failed = failed.toLong(),
    disabled = disabled.toLong(),
    total = total.toLong()
)

/**
 * Returns null for events that say nothing about whether there is still work to drive.
 *
 * [SyncOrchestratorEvent.Started] is one of them: it only acknowledges the start call, and treating
 * it as activity would let the worker mistake an orchestrator that never progresses for a healthy
 * one. [SyncOrchestratorEvent.Completed] is never published by the SDK, so it is folded into
 * [ContentIndexingActivity.Stopped] rather than given a case of its own.
 */
internal fun SyncOrchestratorEvent.toIndexingActivity(): ContentIndexingActivity? = when (this) {
    is SyncOrchestratorEvent.Started -> null

    is SyncOrchestratorEvent.Progress -> ContentIndexingActivity.Progress(
        // Rust reports the active account as an opaque id and leaves it null between accounts.
        activeUserId = v1.activeId?.takeIf { it.isNotBlank() }?.let(::UserId),
        completedUsers = v1.completedUsers.toLong(),
        userCount = v1.userCount.toLong()
    )

    is SyncOrchestratorEvent.ForwardModeEntered -> ContentIndexingActivity.ForwardMode(UserId(v1))
    is SyncOrchestratorEvent.WaitingOnUsers -> ContentIndexingActivity.WaitingOnUsers

    is SyncOrchestratorEvent.Stopped,
    is SyncOrchestratorEvent.Completed -> ContentIndexingActivity.Stopped

    is SyncOrchestratorEvent.Failure -> ContentIndexingActivity.Failed(v1)

    // One account failing does not end the run - the orchestrator records it and moves to the next
    // candidate, exactly as Rust's own `sync_and_wait` treats it.
    is SyncOrchestratorEvent.UserFailure -> null
}

/**
 * Traces the orchestrator, including the events [toIndexingActivity] drops.
 *
 * The percentage is the orchestrator's own, summed across every account's totals, so it moves when
 * accounts are added or removed as well as when messages are indexed. The per-account counters are
 * printed next to it so a jump is readable rather than mysterious.
 */
internal fun SyncOrchestratorEvent.log() {
    when (this) {
        is SyncOrchestratorEvent.Started -> Timber.d("content-search: orchestrator started")

        is SyncOrchestratorEvent.Progress -> Timber.d(
            "content-search: ${v1.activeId ?: "no account"} progress=${v1.percentage}% " +
                "(${v1.processed}/${v1.total}), accounts=${v1.completedUsers}/${v1.userCount}"
        )

        is SyncOrchestratorEvent.ForwardModeEntered -> Timber.d("content-search: $v1 entered forward mode")
        is SyncOrchestratorEvent.WaitingOnUsers -> Timber.d("content-search: orchestrator waiting on accounts")
        is SyncOrchestratorEvent.Stopped -> Timber.d("content-search: orchestrator stopped")
        is SyncOrchestratorEvent.Completed -> Timber.d("content-search: orchestrator completed")
        is SyncOrchestratorEvent.Failure -> Timber.e("content-search: orchestrator failed: $v1")
        is SyncOrchestratorEvent.UserFailure -> Timber.w("content-search: $userId failed, moving on: $failure")
    }
}
