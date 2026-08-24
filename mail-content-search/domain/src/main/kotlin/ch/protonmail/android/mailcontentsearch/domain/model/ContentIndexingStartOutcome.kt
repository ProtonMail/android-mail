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

enum class ContentIndexingStartOutcome {

    Started,

    /** The account was already being indexed, so the call changed nothing. */
    AlreadyRunning,

    /** Refused: content search is off for the account. */
    Refused,

    /** Nothing left to index for the account. */
    AlreadyCompleted;

    /** Whether indexing is running for the account as a result, whoever started it. */
    val isIndexing: Boolean get() = this == Started || this == AlreadyRunning
}
