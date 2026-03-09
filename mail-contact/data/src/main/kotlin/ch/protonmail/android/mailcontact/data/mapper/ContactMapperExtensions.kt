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

import ch.protonmail.android.mailcommon.data.mapper.LocalAvatarInformation
import ch.protonmail.android.mailcommon.data.mapper.LocalContactEmail
import ch.protonmail.android.mailcommon.data.mapper.LocalContactGroupId
import ch.protonmail.android.mailcommon.data.mapper.LocalContactId
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcontact.domain.model.ContactEmail
import ch.protonmail.android.mailcontact.domain.model.ContactGroupId
import ch.protonmail.android.mailcontact.domain.model.ContactId
import uniffi.mail_uniffi.DeviceContactSuggestion

fun LocalContactId.toContactId(): ContactId = ContactId(this.value.toString())
fun LocalContactGroupId.toContactGroupId(): ContactGroupId = ContactGroupId(this.value.toString())

fun ContactId.toLocalContactId(): LocalContactId = LocalContactId(this.id.toULong())
fun ContactGroupId.toLocalContactGroupId(): LocalContactGroupId = LocalContactGroupId(this.id.toULong())

fun LocalContactEmail.toContactEmail(): ContactEmail {
    return ContactEmail(
        id = ContactId(contactId.value.toString()),
        email = this.email,
        isProton = this.isProton,
        lastUsedTime = this.lastUsedTime.toLong(),
        name = this.name,
        avatarInformation = this.avatarInformation.toAvatarInformation()
    )
}

fun LocalAvatarInformation.toAvatarInformation() = AvatarInformation(
    initials = this.text,
    color = this.color
)

@MissingRustApi
fun DeviceContactSuggestion.toContactEmail() = ContactEmail(
    ContactId(this.email), // Rust doesn't expose any id for device contacts
    this.email,
    false, // This value should be provided by Rust
    0L, // This value should be provided by Rust
    name = "", // This value should be provided by Rust
    avatarInformation = AvatarInformation("", "") // This value should be provided by Rust
)

