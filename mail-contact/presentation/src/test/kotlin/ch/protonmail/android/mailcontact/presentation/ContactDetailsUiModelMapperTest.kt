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
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.DecodeByteArray
import ch.protonmail.android.mailcommon.presentation.usecase.FormatLocalDate
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.Avatar
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsGroupLabel
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsGroupsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsItem
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModelMapper
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactDetailsUiModelMapperTest {

    private val getAppLocale = mockk<GetAppLocale> {
        every { this@mockk.invoke() } returns Locale.US
    }

    private val formatLocalDate = FormatLocalDate(
        getAppLocale = getAppLocale
    )
    private val decodeByteArray = mockk<DecodeByteArray> {
        every { this@mockk.invoke(any()) } returns mockk()
    }

    private val contactDetailsUiModelMapper = ContactDetailsUiModelMapper(
        formatLocalDate = formatLocalDate,
        decodeByteArray = decodeByteArray
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(Color.Red.getHexStringFromColor()) } returns Color.Red.toArgb()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `maps DecryptedContact to ContactDetailsUiModel`() {
        val photoByteArray = ContactImagesSample.Photo
        val logoByteArray = ContactImagesSample.Logo
        val decryptedContact = DecryptedContact(
            id = ContactSample.Mario.id,
            contactGroupLabels = listOf(
                ContactGroupLabel(
                    "Group 1",
                    Color.Red.getHexStringFromColor()
                )
            ),
            structuredName = ContactProperty.StructuredName(
                family = "Last", given = "First"
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
                    data = photoByteArray,
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
                    data = logoByteArray,
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

        val actual = contactDetailsUiModelMapper.toContactDetailsUiModel(decryptedContact)

        val expected = ContactDetailsUiModel(
            id = ContactSample.Mario.id,
            defaultPhoneNumber = "1231231235",
            defaultEmail = "Mario@protonmail.com",
            nameHeader = "Mario@protonmail.com",
            nameSubText = "First Last",
            avatar = Avatar.Photo(bitmap = decodeByteArray(photoByteArray)!!),
            contactMainDetailsItemList = listOf(
                /* Emails */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_proton_at,
                    header = TextUiModel(R.string.contact_type_email),
                    value = TextUiModel("Mario@protonmail.com"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Email("Mario@protonmail.com")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_at,
                    header = TextUiModel(R.string.contact_type_home),
                    value = TextUiModel("home_email@Mario.protonmail.com"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Email("home_email@Mario.protonmail.com")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_at,
                    header = TextUiModel(R.string.contact_type_work),
                    value = TextUiModel("work_email@Mario.protonmail.com"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Email("work_email@Mario.protonmail.com")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_at,
                    header = TextUiModel(R.string.contact_type_other),
                    value = TextUiModel("other_email@Mario.protonmail.com"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Email("other_email@Mario.protonmail.com")
                ),
                /* Phones */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_phone),
                    value = TextUiModel("1231231235"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("1231231235")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_home),
                    value = TextUiModel("23233232323"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("23233232323")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_work),
                    value = TextUiModel("45454545"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("45454545")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_other),
                    value = TextUiModel("565656"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("565656")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_mobile),
                    value = TextUiModel("676767"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("676767")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_main),
                    value = TextUiModel("787887"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("787887")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_fax),
                    value = TextUiModel("898989"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("898989")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_phone,
                    header = TextUiModel(R.string.contact_type_pager),
                    value = TextUiModel("90909090"),
                    type = ContactDetailsItem.ContactDetailType.Triggerable.Phone("90909090")
                ),
                /* Addresses */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_proton_map_pin,
                    header = TextUiModel(R.string.contact_type_address),
                    value = TextUiModel(
                        "Address Street1\n123, City\nRegion, Country"

                    )
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_map_pin,
                    header = TextUiModel(R.string.contact_type_other),
                    value = TextUiModel(
                        "Address Other1\n234, City\nRegion, Country"
                    )
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_map_pin,
                    header = TextUiModel(R.string.contact_type_home),
                    value = TextUiModel(
                        "Home address the rest is empty"
                    )
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_map_pin,
                    header = TextUiModel(R.string.contact_type_work),
                    value = TextUiModel(
                        "City the rest is empty"
                    )
                ),
                /* Birthday */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_cake,
                    header = TextUiModel(R.string.contact_property_birthday),
                    value = TextUiModel(formatLocalDate(LocalDate.of(2023, 12, 14)))
                ),
                /* Notes */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_proton_note,
                    header = TextUiModel(R.string.contact_property_note),
                    value = TextUiModel("Note1")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_note,
                    header = TextUiModel(R.string.contact_property_note),
                    value = TextUiModel("Note2")
                )
            ),
            contactOtherDetailsItemList = listOf(
                /* Organizations */
                ContactDetailsItem.Text(
                    displayIcon = true,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_organization),
                    value = TextUiModel("Organization1")
                ),
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_organization),
                    value = TextUiModel("Organization2")
                ),
                /* Titles */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_title),
                    value = TextUiModel("Title")
                ),
                /* Roles */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_role),
                    value = TextUiModel("Role")
                ),
                /* Time zones */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_time_zone),
                    value = TextUiModel("Europe/Paris")
                ),
                /* Logos */
                ContactDetailsItem.Image(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_logo),
                    value = decodeByteArray(logoByteArray)!!
                ),
                /* Members */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_member),
                    value = TextUiModel("Member")
                ),
                /* Languages */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_language),
                    value = TextUiModel("English")
                ),
                /* Urls */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_url),
                    value = TextUiModel("http://proton.me")
                ),
                /* Genders */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_gender),
                    value = TextUiModel("Gender")
                ),
                /* Anniversaries */
                ContactDetailsItem.Text(
                    displayIcon = false,
                    iconResId = R.drawable.ic_proton_text_align_left,
                    header = TextUiModel(R.string.contact_property_anniversary),
                    value = TextUiModel(formatLocalDate(LocalDate.of(2023, 12, 6)))
                )
            ),
            contactGroups = ContactDetailsGroupsItem(
                displayGroupSection = true,
                iconResId = R.drawable.ic_proton_users,
                groupLabelList = listOf(
                    ContactDetailsGroupLabel(
                        name = "Group 1",
                        color = Color.Red
                    )
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `maps empty DecryptedContact to empty ContactDetailsUiModel`() {
        val decryptedContact = DecryptedContact(
            id = ContactSample.Mario.id
        )

        val actual = contactDetailsUiModelMapper.toContactDetailsUiModel(decryptedContact)

        val expected = ContactDetailsUiModel(
            id = ContactSample.Mario.id,
            nameHeader = "",
            avatar = Avatar.Initials(""),
            contactMainDetailsItemList = emptyList(),
            contactOtherDetailsItemList = emptyList(),
            contactGroups = ContactDetailsGroupsItem(
                displayGroupSection = false,
                iconResId = R.drawable.ic_proton_users,
                groupLabelList = emptyList()
            )
        )

        assertEquals(actual, expected)
    }
}
