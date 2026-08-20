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

package ch.protonmail.android.mailcontact.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemType
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeContact
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeDevice
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeGroup
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import javax.inject.Inject

class ContactItemTypeMapper @Inject constructor(
    private val contactItemMapper: ContactItemMapper,
    private val contactGroupItemMapper: ContactGroupItemMapper
) {

    fun toContactMetadata(contactItem: LocalContactItemType): ContactMetadata {
        return when (contactItem) {
            is LocalContactItemTypeContact -> {
                contactItemMapper.toContact(contactItem)
            }

            is LocalContactItemTypeGroup -> {
                contactGroupItemMapper.toContactGroup(contactItem)
            }

            // This is currently a no-op since we don't consume this mobile side
            // but the mapping is in place for the sake of compilation.
            is LocalContactItemTypeDevice -> {
                contactItemMapper.toContact(contactItem)
            }
        }
    }
}
