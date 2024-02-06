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

import ch.protonmail.android.mailcontact.domain.mapper.DecryptedContactMapper
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.Expertise
import ezvcard.property.Uid
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DecryptedContactMapperTest {

    private val sut = DecryptedContactMapper()

    private val contactId = ContactWithCardsSample.Mario.id

    private val decryptedContact = DecryptedContact(
        contactId
    )

    private val existingUid = Uid("Fallback-UID")
    private val existingVersion = VCardVersion.V3_0
    private val existingVCard = VCard().apply {
        uid = existingUid
        version = existingVersion
    }

    private val fallbackUid = Uid("Fallback-UID")
    private val fallbackName = "Fallback-Name"

    @Test
    fun `ClearText ContactCard is not returned if existing VCard is null`() {
        // Given
        val expectedContactCard = null

        // When
        val actual = sut.mapToClearTextContactCard(
            fallbackUid,
            expectedContactCard
        )

        // Then
        assertNull(actual)
    }

    @Test
    fun `ClearText ContactCard is returned even if existing VCard doesn't contain CATEGORIES`() {
        // Given
        val expectedContactCard = existingVCard

        // When
        val actual = sut.mapToClearTextContactCard(
            fallbackUid,
            expectedContactCard
        )!!

        // Then
        assertNotNull(actual)
    }

    @Test
    fun `properties of existing VCard are not cleared when returning ClearText ContactCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            // we don't have expertise in Android Mail so this is good for testing
            addExpertise(Expertise("ClearText generation skill"))
        }

        // When
        val actual = sut.mapToClearTextContactCard(
            fallbackUid,
            expectedContactCard
        )!!

        // Then
        assertEquals(expectedContactCard.expertise.first().value, actual.expertise.first().value)
    }

    @Test
    fun `properties of existing VCard are not cleared when returning Signed ContactCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            // we don't have expertise in Android Mail so this is good for testing
            addExpertise(Expertise("Signed generation skill"))
        }

        // When
        val actual = sut.mapToSignedContactCard(
            fallbackUid,
            fallbackName,
            decryptedContact,
            expectedContactCard
        )

        // Then
        assertEquals(expectedContactCard.expertise.first().value, actual.expertise.first().value)
    }

    @Test
    fun `properties of existing VCard are not cleared when returning EncryptedAndSigned ContactCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            // we don't have expertise in Android Mail so this is good for testing
            addExpertise(Expertise("EncryptedAndSigned generation skill"))
        }

        // When
        val actual = sut.mapToEncryptedAndSignedContactCard(
            fallbackUid,
            decryptedContact,
            expectedContactCard
        )

        // Then
        assertEquals(expectedContactCard.expertise.first().value, actual.expertise.first().value)
    }

    @Test
    fun `fallback UID is returned in ClearText ContactCard when it didn't exist in VCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            uid.value = null
        }

        // When
        val actual = sut.mapToClearTextContactCard(
            fallbackUid,
            expectedContactCard
        )!!

        // Then
        assertEquals(expectedContactCard.uid, actual.uid)
    }

    @Test
    fun `fallback UID is returned in Signed ContactCard when it didn't exist in VCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            uid.value = null
        }

        // When
        val actual = sut.mapToSignedContactCard(
            fallbackUid,
            fallbackName,
            decryptedContact,
            expectedContactCard
        )

        // Then
        assertEquals(expectedContactCard.uid, actual.uid)
    }

    @Test
    fun `fallback UID is returned in EncryptedAndSigned ContactCard when it didn't exist in VCard`() {
        // Given
        val expectedContactCard = existingVCard.apply {
            uid.value = null
        }

        // When
        val actual = sut.mapToEncryptedAndSignedContactCard(
            fallbackUid,
            decryptedContact,
            expectedContactCard
        )

        // Then
        assertEquals(expectedContactCard.uid, actual.uid)
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
            fallbackUid,
            VCard()
        )!!

        val actualSigned = sut.mapToSignedContactCard(
            fallbackUid,
            fallbackName,
            expectedDecryptedContact,
            VCard()
        )

        val actualEncryptedAndSigned = sut.mapToEncryptedAndSignedContactCard(
            fallbackUid,
            expectedDecryptedContact,
            VCard()
        )

        // Then
        assertTrue(actualClearText.notes.isEmpty())
        assertTrue(actualClearText.telephoneNumbers.isEmpty())

        assertTrue(actualSigned.notes.isEmpty())
        assertTrue(actualSigned.telephoneNumbers.isEmpty())

        assertTrue(actualEncryptedAndSigned.notes.size == 2)
        assertTrue(actualEncryptedAndSigned.telephoneNumbers.size == 1)
    }

}
