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

package ch.protonmail.android.mailpagination.data.scroller

import uniffi.mail_uniffi.MailScrollerError

sealed class ScrollerUpdate<out T> {
    abstract val scrollerId: String

    data class None(
        override val scrollerId: String
    ) : ScrollerUpdate<Nothing>()

    data class Append<T>(
        override val scrollerId: String,
        val items: List<T>
    ) : ScrollerUpdate<T>()

    data class ReplaceFrom<T>(
        override val scrollerId: String,
        val idx: Int,
        val items: List<T>
    ) : ScrollerUpdate<T>()

    data class ReplaceBefore<T>(
        override val scrollerId: String,
        val idx: Int,
        val items: List<T>
    ) : ScrollerUpdate<T>()

    data class ReplaceRange<T>(
        override val scrollerId: String,
        val fromIdx: Int,
        val toIdx: Int,
        val items: List<T>
    ) : ScrollerUpdate<T>()

    data class Error(
        val error: MailScrollerError,
        override val scrollerId: String = NO_SCROLLER_IN_ERROR
    ) : ScrollerUpdate<Nothing>()

    companion object {
        const val NO_SCROLLER_IN_ERROR = "no_scroller_in_error"
    }
}

fun <T> ScrollerUpdate<T>.itemCount(): Int = when (this) {
    is ScrollerUpdate.None -> 0
    is ScrollerUpdate.Append -> items.size
    is ScrollerUpdate.ReplaceFrom -> items.size
    is ScrollerUpdate.ReplaceBefore -> items.size
    is ScrollerUpdate.ReplaceRange -> items.size
    is ScrollerUpdate.Error -> 0
}

fun ScrollerUpdate<*>.debugTypeName(): String = when (this) {
    is ScrollerUpdate.None -> "None"
    is ScrollerUpdate.Append -> "Append"
    is ScrollerUpdate.ReplaceFrom -> "ReplaceFrom"
    is ScrollerUpdate.ReplaceBefore -> "ReplaceBefore"
    is ScrollerUpdate.ReplaceRange -> "ReplaceRange"
    is ScrollerUpdate.Error -> "Error"
}
