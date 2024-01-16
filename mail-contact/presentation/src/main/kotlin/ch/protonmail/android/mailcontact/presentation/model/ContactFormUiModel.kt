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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.contact.domain.entity.ContactId

data class ContactFormUiModel(
    val id: ContactId?,
    val displayName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val emails: List<InputField.SingleTyped> = emptyList(),
    val phones: List<InputField.SingleTyped> = emptyList(),
    val addresses: List<InputField.Address> = emptyList(),
    val birthday: InputField.Date? = null,
    val notes: List<InputField.Note> = emptyList()
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
}
