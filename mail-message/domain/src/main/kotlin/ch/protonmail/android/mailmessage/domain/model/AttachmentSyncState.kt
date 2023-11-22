/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.domain.model

enum class AttachmentSyncState(val value: Int) {
    Local(0),
    Uploaded(1),

    /**
     * External can be used for attachments coming from a forwarded messages or
     * from an attachment which was added via a different client
     */
    External(2),
    ExternalUploaded(3);

    companion object {

        fun from(value: Int) = AttachmentSyncState.values().find { it.value == value } ?: Local
    }
}
