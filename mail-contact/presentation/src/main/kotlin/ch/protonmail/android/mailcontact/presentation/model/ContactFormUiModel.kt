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

package ch.protonmail.android.mailcontact.presentation.model

import java.time.LocalDate
import android.graphics.Bitmap
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.contact.domain.entity.ContactId

data class ContactFormUiModel(
    val id: ContactId?,
    val avatar: ContactFormAvatar,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val emails: List<InputField.SingleTyped>,
    val phones: List<InputField.SingleTyped>,
    val addresses: List<InputField.Address>,
    val birthday: InputField.Date?,
    val notes: List<InputField.Note>,
    val others: List<InputField>,
    val otherTypes: List<FieldType.OtherType>
)

val emptyContactFormUiModel = ContactFormUiModel(
    id = null,
    avatar = ContactFormAvatar.Empty,
    displayName = "",
    firstName = "",
    lastName = "",
    emails = emptyList(),
    phones = emptyList(),
    addresses = emptyList(),
    birthday = null,
    notes = emptyList(),
    others = emptyList(),
    otherTypes = FieldType.OtherType.values().toList()
)

sealed interface InputField {

    data class SingleTyped(
        val value: String,
        val selectedType: FieldType
    ) : InputField

    data class Address(
        val streetAddress: String,
        val postalCode: String,
        val locality: String,
        val region: String,
        val country: String,
        val selectedType: FieldType
    ) : InputField

    data class ImageTyped(
        val value: Bitmap,
        val selectedType: FieldType
    ) : InputField

    data class DateTyped(
        val value: LocalDate,
        val selectedType: FieldType
    ) : InputField

    data class Date(
        val value: LocalDate
    ) : InputField

    data class Note(
        val value: String
    ) : InputField
}

sealed interface FieldType {
    enum class EmailType(val localizedValue: TextUiModel) : FieldType {
        Email(TextUiModel(R.string.contact_type_email)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other))
    }

    enum class PhoneType(val localizedValue: TextUiModel) : FieldType {
        Phone(TextUiModel(R.string.contact_type_phone)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other)),
        Mobile(TextUiModel(R.string.contact_type_mobile)),
        Main(TextUiModel(R.string.contact_type_main)),
        Fax(TextUiModel(R.string.contact_type_fax)),
        Pager(TextUiModel(R.string.contact_type_pager))
    }

    enum class AddressType(val localizedValue: TextUiModel) : FieldType {
        Address(TextUiModel(R.string.contact_type_address)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other))
    }

    enum class OtherType(val localizedValue: TextUiModel) : FieldType {
        Photo(TextUiModel(R.string.contact_property_photo)),
        Organization(TextUiModel(R.string.contact_property_organization)),
        Title(TextUiModel(R.string.contact_property_title)),
        Role(TextUiModel(R.string.contact_property_role)),
        TimeZone(TextUiModel(R.string.contact_property_time_zone)),
        Logo(TextUiModel(R.string.contact_property_logo)),
        Member(TextUiModel(R.string.contact_property_member)),
        Language(TextUiModel(R.string.contact_property_language)),
        Url(TextUiModel(R.string.contact_property_url)),
        Gender(TextUiModel(R.string.contact_property_gender)),
        Anniversary(TextUiModel(R.string.contact_property_anniversary))
    }
}

sealed interface ContactFormAvatar {

    // Use data class with camera icon res id here once we implement image picker.
    object Empty : ContactFormAvatar

    data class Photo(
        val bitmap: Bitmap
    ) : ContactFormAvatar
}
