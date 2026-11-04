/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.model

/**
 * The running mail import of a BYOE address. [processed] counts the messages imported so far and [total] the
 * messages to import, null while it is not known yet.
 */
data class ByoeImportStatus(
    val addressId: String,
    val state: ByoeImportState,
    val error: ByoeImportError?,
    val processed: Long,
    val total: Long?
)

enum class ByoeImportState {
    Queued,
    Running,
    Done,
    Failed,

    /** Waits for the user; [ByoeImportStatus.error] says why. */
    Paused,
    Canceled,
    Delayed,
    Unknown
}

enum class ByoeImportError {

    /** The user needs to reconnect the account. */
    Connection,

    /** The user needs more storage. */
    StorageLimit,
    Unknown
}
