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
    is ContentIndexingState.Running -> percentage
    ContentIndexingState.Idle,
    ContentIndexingState.Initializing,
    ContentIndexingState.Completed,
    ContentIndexingState.Cancelled,
    ContentIndexingState.Failed -> null
}

internal fun ContentIndexingState.isActive(): Boolean = when (this) {
    ContentIndexingState.Initializing,
    is ContentIndexingState.Running -> true

    ContentIndexingState.Idle,
    ContentIndexingState.Completed,
    ContentIndexingState.Cancelled,
    ContentIndexingState.Failed -> false
}

internal fun ContentIndexingState.isTerminal(): Boolean = when (this) {
    ContentIndexingState.Idle,
    ContentIndexingState.Completed,
    ContentIndexingState.Cancelled,
    ContentIndexingState.Failed -> true

    ContentIndexingState.Initializing,
    is ContentIndexingState.Running -> false
}
