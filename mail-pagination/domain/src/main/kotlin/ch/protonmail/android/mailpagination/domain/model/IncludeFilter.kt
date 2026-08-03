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

package ch.protonmail.android.mailpagination.domain.model

/**
 * The locations a paginator should include on top of the ones its location already covers.
 *
 * Spam and trash are independent: the mailbox drives both together through a single chip, while
 * content search exposes one toggle per location.
 */
data class IncludeFilter(
    val includeSpam: Boolean,
    val includeTrash: Boolean
) {

    val includesAny: Boolean get() = includeSpam || includeTrash

    companion object {

        val None = IncludeFilter(includeSpam = false, includeTrash = false)

        /** Both locations at once, for callers that expose a single "include spam and trash" switch. */
        fun spamAndTrash(include: Boolean) = IncludeFilter(includeSpam = include, includeTrash = include)
    }
}
