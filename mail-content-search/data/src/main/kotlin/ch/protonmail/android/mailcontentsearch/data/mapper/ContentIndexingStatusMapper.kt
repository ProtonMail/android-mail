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
import uniffi.mail_uniffi.ContentSearchIndexingProgress
import uniffi.mail_uniffi.ContentSearchIndexingStatus

@Suppress("MagicNumber")
internal fun ContentSearchIndexingProgress.toPercentage() = (this.estimatedFraction ?: 0.0) * 100

internal fun ContentSearchIndexingProgress.toIndexingState(): ContentIndexingState = toIndexingState(this.status, this)

internal fun toIndexingState(
    status: ContentSearchIndexingStatus,
    progress: ContentSearchIndexingProgress?
): ContentIndexingState = when (status) {
    ContentSearchIndexingStatus.NONE -> ContentIndexingState.Idle
    ContentSearchIndexingStatus.ONGOING -> ContentIndexingState.Running(progress?.toPercentage() ?: 0.0)
    ContentSearchIndexingStatus.COMPLETED -> ContentIndexingState.Completed
    ContentSearchIndexingStatus.INTERRUPTED -> ContentIndexingState.Cancelled
}

internal fun ContentSearchIndexingStatus.isTerminal(): Boolean = when (this) {
    ContentSearchIndexingStatus.NONE,
    ContentSearchIndexingStatus.COMPLETED,
    ContentSearchIndexingStatus.INTERRUPTED -> true

    ContentSearchIndexingStatus.ONGOING -> false
}
