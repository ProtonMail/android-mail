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

package ch.protonmail.android.mailcontact.domain.usecase

import ch.protonmail.android.mailcontact.domain.VCARD_PROD_ID
import ch.protonmail.android.mailcontact.domain.mapper.DecryptedContactMapper
import ch.protonmail.android.mailcontact.domain.model.ContactGroupLabel
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.testdata.contact.ContactVCardSample
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.Uid
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecryptedContactMapperTest {

    private val sut = DecryptedContactMapper()

    private val contactId = ContactWithCardsSample.Mario.id

    private val decryptedContact = DecryptedContact(
        contactId
    )

    private val existingUid = Uid("Fallback-UID")
    private val existingVersion = VCardVersion.V4_0
    private val existingVCard = VCard().apply {
        uid = existingUid
        version = existingVersion
    }

    private val fallbackName = "Fallback-Name"

    @Test
    fun `ClearText ContactCard is not returned if existing VCard doesnt contain CATEGORIES`() {
        // Given
        val expectedContactCard = VCard()

        // When
        val actual = sut.mapToClearTextContactCard(
            expectedContactCard
        )

        // Then
        assertNull(actual)
    }

    @Test
    fun `ClearText ContactCard is returned if existing VCard contains CATEGORIES`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            setCategories("Coworkers", "Friends")
        }

        // When
        val actual = sut.mapToClearTextContactCard(
            expectedContactCard
        )

        // Then
        assertNotNull(actual)
    }

    @Test
    fun `ClearText ContactCard is returned if existing VCard contains no CATEGORIES but non-empty list is passed`() {
        // Given
        val expectedContactCard = existingVCard
        val expectedCategories = listOf(
            ContactGroupLabel("Coworkers", "#"),
            ContactGroupLabel("Friends", "#")
        )

        // When
        val actual = sut.mapToClearTextContactCard(
            expectedContactCard,
            expectedCategories
        )

        // Then
        assertNotNull(actual)
        assertTrue(actual.categories.values.contains("Coworkers"))
        assertTrue(actual.categories.values.contains("Friends"))
    }

    @Test
    fun `ClearText ContactCard is not returned if existing VCard contains no CATEGORIES and empty list is passed`() {
        // Given
        val expectedContactCard = existingVCard
        val expectedCategories = emptyList<ContactGroupLabel>()

        // When
        val actual = sut.mapToClearTextContactCard(
            expectedContactCard,
            expectedCategories
        )

        // Then
        assertNull(actual)
        assertNull(actual?.categories?.values)
    }

    @Test
    fun `if ClearText ContactCard is returned, it contains only CATEGORIES, PRODID and VERSION`() {
        // Given
        val givenExtendedPropertyName = "Extended Property"
        val expectedContactCard = existingVCard.apply {
            setProductId("Not-Proton-Product-Id")
            setCategories("Coworkers", "Friends")
            setFormattedName("Formatted Name")
            setOrganization("Organization")
            setExtendedProperty(givenExtendedPropertyName, "Extended Property Value")
        }

        // When
        val actual = sut.mapToClearTextContactCard(
            expectedContactCard
        )

        // Then
        assertNotNull(actual)

        assertTrue(actual.categories.values.size == 2)
        assertEquals(actual.version.version, VCardVersion.V4_0.version)
        assertEquals(actual.productId.value, VCARD_PROD_ID)

        assertNull(actual.formattedName)
        assertNull(actual.organization)
        assertNull(actual.getExtendedProperty(givenExtendedPropertyName))
    }

    @Test
    fun `encrypted properties are only returned in encrypted ContactCards`() {
        // Given
        val expectedDecryptedContact = decryptedContact.copy(
            notes = listOf(
                ContactProperty.Note("note1"),
                ContactProperty.Note("note2")
            ),
            telephones = listOf(
                ContactProperty.Telephone(ContactProperty.Telephone.Type.Home, "666")
            )
        )

        // When
        val actualClearText = sut.mapToClearTextContactCard(
            VCard()
        )

        val actualSigned = sut.mapToSignedContactCard(
            fallbackName,
            expectedDecryptedContact,
            VCard()
        )

        val actualEncryptedAndSigned = sut.mapToEncryptedAndSignedContactCard(
            expectedDecryptedContact,
            VCard()
        )

        // Then
        assertNull(actualClearText)

        assertTrue(actualSigned.notes.isEmpty())
        assertTrue(actualSigned.telephoneNumbers.isEmpty())

        assertTrue(actualEncryptedAndSigned.notes.size == 2)
        assertTrue(actualEncryptedAndSigned.telephoneNumbers.size == 1)
    }

    @Test
    fun `mapToSignedContactCard clears existing data in VCard if DecryptedContact doesn't have them`() {
        // Given
        val expectedDecryptedContact = decryptedContact // empty DecryptedContact

        // When
        val actualSigned = sut.mapToSignedContactCard(
            fallbackName,
            expectedDecryptedContact,
            Ezvcard.parse(ContactVCardSample.marioVCardType2).first()
        )

        // Then
        assertEquals(fallbackName, actualSigned.formattedName.value)
        assertTrue(actualSigned.emails.isEmpty())
    }

    @Test
    fun `mapToEncryptedAndSignedContactCard clears existing data in VCard if DecryptedContact doesn't have them`() {
        // Given
        val expectedDecryptedContact = decryptedContact // empty DecryptedContact

        // When
        val actualEncryptedAndSigned = sut.mapToEncryptedAndSignedContactCard(
            expectedDecryptedContact,
            Ezvcard.parse(ContactVCardSample.marioVCardType3).first()
        )

        // Then
        assertNull(actualEncryptedAndSigned.structuredName)
        assertTrue(actualEncryptedAndSigned.telephoneNumbers.isEmpty())
        assertTrue(actualEncryptedAndSigned.addresses.isEmpty())
        assertNull(actualEncryptedAndSigned.birthday)
        assertTrue(actualEncryptedAndSigned.notes.isEmpty())
        assertTrue(actualEncryptedAndSigned.organizations.isEmpty())
        assertTrue(actualEncryptedAndSigned.titles.isEmpty())
        assertTrue(actualEncryptedAndSigned.logos.isEmpty())
        assertTrue(actualEncryptedAndSigned.photos.isEmpty())
        assertTrue(actualEncryptedAndSigned.roles.isEmpty())
        assertTrue(actualEncryptedAndSigned.timezones.isEmpty())
        assertTrue(actualEncryptedAndSigned.members.isEmpty())
        assertTrue(actualEncryptedAndSigned.languages.isEmpty())
        assertTrue(actualEncryptedAndSigned.urls.isEmpty())
        assertNull(actualEncryptedAndSigned.gender)
        assertTrue(actualEncryptedAndSigned.anniversaries.isEmpty())
    }

}
