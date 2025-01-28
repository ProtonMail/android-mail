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

package ch.protonmail.android.mailcontact.presentation

import java.time.LocalDate
import ch.protonmail.android.mailcommon.presentation.usecase.DecodeByteArray
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.filterOutUnsupportedFields
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactFormUiModelMapperTest {

    private val decodeByteArray = mockk<DecodeByteArray> {
        every { this@mockk.invoke(any()) } returns mockk()
    }

    private val contactFormUiModelMapper = ContactFormUiModelMapper(
        decodeByteArray = decodeByteArray
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `maps DecryptedContact to ContactFormUiModel`() {
        val decryptedContact = getDecryptedContact()
        val contactFormUiModel = getContactFormUiModel()

        val actual = contactFormUiModelMapper.toContactFormUiModel(decryptedContact)

        assertEquals(contactFormUiModel, actual)
    }

    @Test
    fun `maps ContactFormUiModel to DecryptedContact`() {
        val decryptedContact = getDecryptedContact()
        val contactFormUiModel = getContactFormUiModel()
        val contactFormUiModelWithBlankValues = contactFormUiModel.copy(
            emails = contactFormUiModel.emails.toMutableList().apply {
                this.add(
                    InputField.SingleTyped(
                        fieldId = "NotUsedInThisContext",
                        value = " ",
                        selectedType = FieldType.EmailType.Other
                    )
                )
            },
            telephones = contactFormUiModel.telephones.toMutableList().apply {
                this.add(
                    InputField.SingleTyped(
                        fieldId = "NotUsedInThisContext",
                        value = " ",
                        selectedType = FieldType.TelephoneType.Pager
                    )
                )
            },
            addresses = contactFormUiModel.addresses.toMutableList().apply {
                this.add(
                    InputField.Address(
                        fieldId = "NotUsedInThisContext",
                        streetAddress = " ",
                        city = " ",
                        region = " ",
                        postalCode = " ",
                        country = " ",
                        selectedType = FieldType.AddressType.Work
                    )
                )
            },
            notes = contactFormUiModel.notes.toMutableList().apply {
                this.add(
                    InputField.Note(
                        fieldId = "NotUsedInThisContext",
                        value = " "
                    )
                )
            },
            others = contactFormUiModel.others.toMutableList().apply {
                this.add(
                    InputField.SingleTyped(
                        fieldId = "NotUsedInThisContext",
                        value = " ",
                        selectedType = FieldType.OtherType.Url
                    )
                )
                this.add(
                    InputField.SingleTyped(
                        fieldId = "NotUsedInThisContext",
                        value = " ",
                        selectedType = FieldType.OtherType.Gender
                    )
                )
            }
        )

        val actual = contactFormUiModelMapper.toDecryptedContact(
            contactFormUiModelWithBlankValues,
            decryptedContact.contactGroupLabels,
            decryptedContact.photos,
            decryptedContact.logos
        )

        assertEquals(decryptedContact, actual)
    }

    @Test
    fun `maps form address to domain address correctly`() {
        val expected = getDecryptedContact().copy(
            addresses = listOf(
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Address,
                    streetAddress = "Address Street1",
                    locality = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country"
                )
            )
        )
        val contactFormUiModel = getContactFormUiModel()
        val contactFormUiModelWithBlankValues = contactFormUiModel.copy(
            addresses = listOf(
                InputField.Address(
                    fieldId = "0",
                    streetAddress = "Address Street1",
                    city = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country",
                    selectedType = FieldType.AddressType.Address
                )
            )
        )

        val actual = contactFormUiModelMapper.toDecryptedContact(
            contactFormUiModelWithBlankValues,
            expected.contactGroupLabels,
            expected.photos,
            expected.logos
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `given empty display name when mapping ContactFormUiModel to DecryptedContact use first and last name`() {
        val contactFormUiModel = getContactFormUiModel().copy(
            displayName = ""
        )
        val decryptedContact = getDecryptedContact().copy(
            formattedName = ContactProperty.FormattedName(
                value = "${contactFormUiModel.firstName} ${contactFormUiModel.lastName}"
            )
        )

        val actual = contactFormUiModelMapper.toDecryptedContact(
            contactFormUiModel,
            decryptedContact.contactGroupLabels,
            decryptedContact.photos,
            decryptedContact.logos
        )

        assertEquals(decryptedContact, actual)
    }

    @Test
    fun `maps empty DecryptedContact to empty ContactFormUiModel`() {
        val decryptedContact = DecryptedContact(
            id = ContactSample.Mario.id
        )

        val actual = contactFormUiModelMapper.toContactFormUiModel(decryptedContact)

        val expected = emptyContactFormUiModel().copy(
            id = ContactSample.Mario.id
        )

        assertEquals(actual, expected)
    }

    private fun getDecryptedContact(): DecryptedContact {
        return DecryptedContact(
            id = ContactSample.Mario.id,
            contactGroupLabels = listOf(
                ContactGroupLabel(
                    "Group 1",
                    androidx.compose.ui.graphics.Color.Red.getHexStringFromColor()
                )
            ),
            structuredName = ContactProperty.StructuredName(
                family = "Mario Last Name", given = "Mario First Name"
            ),
            formattedName = ContactProperty.FormattedName(value = "Mario@protonmail.com"),
            emails = listOf(
                ContactProperty.Email(type = ContactProperty.Email.Type.Email, value = "Mario@protonmail.com"),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Home,
                    value = "home_email@Mario.protonmail.com"
                ),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Work,
                    value = "work_email@Mario.protonmail.com"
                ),
                ContactProperty.Email(
                    type = ContactProperty.Email.Type.Other,
                    value = "other_email@Mario.protonmail.com"
                )
            ),
            telephones = listOf(
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Telephone, text = "1231231235"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Home,
                    text = "23233232323"
                ),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Work,
                    text = "45454545"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Other, text = "565656"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Mobile,
                    text = "676767"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Main, text = "787887"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Fax,
                    text = "898989"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Pager, text = "90909090")
            ),
            addresses = listOf(
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Address,
                    streetAddress = "Address Street1",
                    locality = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country"
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Other,
                    streetAddress = "Address Other1",
                    locality = "City",
                    region = "Region",
                    postalCode = "234",
                    country = "Country"
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Home,
                    streetAddress = "Home address the rest is empty",
                    locality = "",
                    region = "",
                    postalCode = "",
                    country = ""
                ),
                ContactProperty.Address(
                    type = ContactProperty.Address.Type.Work,
                    streetAddress = "City the rest is empty",
                    locality = "",
                    region = "",
                    postalCode = "",
                    country = ""
                )
            ),
            birthday = ContactProperty.Birthday(date = LocalDate.of(2023, 12, 14)),
            notes = listOf(ContactProperty.Note(value = "Note1"), ContactProperty.Note(value = "Note2")),
            photos = listOf(
                ContactProperty.Photo(
                    data = ContactImagesSample.Photo,
                    contentType = "jpeg",
                    mediaType = null,
                    extension = null
                )
            ),
            organizations = listOf(
                ContactProperty.Organization(value = "Organization1"),
                ContactProperty.Organization(value = "Organization2")
            ),
            titles = listOf(ContactProperty.Title(value = "Title")),
            roles = listOf(ContactProperty.Role(value = "Role")),
            timezones = listOf(ContactProperty.Timezone(text = "Europe/Paris")),
            logos = listOf(
                ContactProperty.Logo(
                    data = ContactImagesSample.Logo,
                    contentType = "jpeg",
                    mediaType = null,
                    extension = null
                )
            ),
            members = listOf(ContactProperty.Member(value = "Member")),
            languages = listOf(ContactProperty.Language(value = "English")),
            urls = listOf(ContactProperty.Url(value = "http://proton.me")),
            gender = ContactProperty.Gender(gender = "Gender"),
            anniversary = ContactProperty.Anniversary(date = LocalDate.of(2023, 12, 6))
        )
    }

    @SuppressWarnings("ComplexMethod")
    private fun getContactFormUiModel(): ContactFormUiModel {
        var incrementalFieldId = 0
        return ContactFormUiModel(
            id = ContactSample.Mario.id,
            avatar = ContactFormAvatar.Photo(bitmap = decodeByteArray(ContactImagesSample.Photo)!!),
            displayName = "Mario@protonmail.com",
            firstName = "Mario First Name",
            lastName = "Mario Last Name",
            emails = mutableListOf(
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Mario@protonmail.com",
                    selectedType = FieldType.EmailType.Email
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "home_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Home
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "work_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Work
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "other_email@Mario.protonmail.com",
                    selectedType = FieldType.EmailType.Other
                )
            ),
            telephones = mutableListOf(
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "1231231235",
                    selectedType = FieldType.TelephoneType.Telephone
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "23233232323",
                    selectedType = FieldType.TelephoneType.Home
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "45454545",
                    selectedType = FieldType.TelephoneType.Work
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "565656",
                    selectedType = FieldType.TelephoneType.Other
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "676767",
                    selectedType = FieldType.TelephoneType.Mobile
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "787887",
                    selectedType = FieldType.TelephoneType.Main
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "898989",
                    selectedType = FieldType.TelephoneType.Fax
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "90909090",
                    selectedType = FieldType.TelephoneType.Pager
                )
            ),
            addresses = mutableListOf(
                InputField.Address(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    streetAddress = "Address Street1",
                    city = "City",
                    region = "Region",
                    postalCode = "123",
                    country = "Country",
                    selectedType = FieldType.AddressType.Address
                ),
                InputField.Address(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    streetAddress = "Address Other1",
                    city = "City",
                    region = "Region",
                    postalCode = "234",
                    country = "Country",
                    selectedType = FieldType.AddressType.Other
                ),
                InputField.Address(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    streetAddress = "Home address the rest is empty",
                    city = "",
                    region = "",
                    postalCode = "",
                    country = "",
                    selectedType = FieldType.AddressType.Home
                ),
                InputField.Address(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    streetAddress = "City the rest is empty",
                    city = "",
                    region = "",
                    postalCode = "",
                    country = "",
                    selectedType = FieldType.AddressType.Work
                )
            ),
            birthday = InputField.Birthday(
                fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                value = LocalDate.of(2023, 12, 14)
            ),
            notes = mutableListOf(
                InputField.Note(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Note1"
                ),
                InputField.Note(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Note2"
                )
            ),
            others = mutableListOf(
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Organization1",
                    selectedType = FieldType.OtherType.Organization
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Organization2",
                    selectedType = FieldType.OtherType.Organization
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Title",
                    selectedType = FieldType.OtherType.Title
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Role",
                    selectedType = FieldType.OtherType.Role
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Europe/Paris",
                    selectedType = FieldType.OtherType.TimeZone
                ),
                InputField.ImageTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = decodeByteArray(ContactImagesSample.Logo)!!,
                    selectedType = FieldType.OtherType.Logo
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Member",
                    selectedType = FieldType.OtherType.Member
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "English",
                    selectedType = FieldType.OtherType.Language
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "http://proton.me",
                    selectedType = FieldType.OtherType.Url
                ),
                InputField.SingleTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = "Gender",
                    selectedType = FieldType.OtherType.Gender
                ),
                InputField.DateTyped(
                    fieldId = incrementalFieldId.toString().also { incrementalFieldId++ },
                    value = LocalDate.of(2023, 12, 6),
                    selectedType = FieldType.OtherType.Anniversary
                )
            ),
            otherTypes = FieldType.OtherType.values().filterOutUnsupportedFields().toList(),
            incrementalUniqueFieldId = incrementalFieldId
        )
    }
}
