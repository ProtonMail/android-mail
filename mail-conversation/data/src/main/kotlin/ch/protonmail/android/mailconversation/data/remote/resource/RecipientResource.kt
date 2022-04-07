/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailconversation.data.remote.resource

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipientResource(
    @SerialName("Address")
    val address: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Group")
    val group: String? = null,
) {
    fun toRecipient() = Recipient(
        address = address,
        name = name,
        group = group
    )
}
