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

package ch.protonmail.android.uitest.models.detail

internal enum class RecipientKind {
    To,
    Cc,
    Bcc
}

internal sealed class ExtendedHeaderRecipientEntry(
    val kind: RecipientKind,
    val index: Int,
    val name: String,
    val address: String
) {

    class To(
        index: Int,
        name: String,
        address: String
    ) : ExtendedHeaderRecipientEntry(
        kind = RecipientKind.To,
        index = index,
        name = name,
        address = address
    )

    class Cc(
        index: Int,
        name: String,
        address: String
    ) : ExtendedHeaderRecipientEntry(
        kind = RecipientKind.Cc,
        index = index,
        name = name,
        address = address
    )

    class Bcc(
        index: Int,
        name: String,
        address: String
    ) : ExtendedHeaderRecipientEntry(
        kind = RecipientKind.Bcc,
        index = index,
        name = name,
        address = address
    )
}
