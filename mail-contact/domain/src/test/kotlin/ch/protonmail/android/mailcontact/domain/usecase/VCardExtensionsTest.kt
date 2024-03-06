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
import ch.protonmail.android.mailcontact.domain.protonSupportedVCardExtendedProperties
import ch.protonmail.android.mailcontact.domain.sanitizeAndBuildVCard
import ch.protonmail.android.testdata.contact.ContactVCardSample
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.property.Expertise
import ezvcard.property.ProductId
import ezvcard.property.RawProperty
import ezvcard.property.Uid
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class VCardExtensionsTest {

    @Test
    fun `sanitizeAndBuildVCard sets UID if UID in original VCARD was empty`() = runTest {
        // Given
        val given = VCard()

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        assertNotNull(actual.uid)
    }

    @Test
    fun `sanitizeAndBuildVCard copies UID if UID in original VCARD was present`() = runTest {
        // Given
        val givenUid = Uid("original UID")
        val given = VCard().apply {
            uid = givenUid
        }

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        assertEquals(givenUid, actual.uid)
    }

    @Test
    fun `sanitizeAndBuildVCard sets PRODID to Proton Product ID`() = runTest {
        // Given
        val given = VCard().apply {
            productId = ProductId("original PRODID")
        }

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        assertEquals(VCARD_PROD_ID, actual.productId.value)
    }

    @Test
    fun `sanitizeAndBuildVCard copies supported crypto-related extended properties from original VCARD`() = runTest {
        // Given
        val givenNotSupportedRawProperty = RawProperty("X-not-supported-property", "value")
        val given = VCard().apply {
            protonSupportedVCardExtendedProperties.forEach {
                extendedProperties.add(RawProperty(it, it))
            }
            extendedProperties.add(givenNotSupportedRawProperty)
        }

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        protonSupportedVCardExtendedProperties.forEach {
            assertEquals(it, actual.getExtendedProperty(it).value)
        }
        assertNull(actual.getExtendedProperty(givenNotSupportedRawProperty.propertyName))
    }

    @Test
    fun `sanitizeAndBuildVCard copies only supported properties from original VCARD`() = runTest {
        // Given
        val givenNotSupportedRawProperty = RawProperty("X-not-supported-property", "value")
        val givenNotSupportedProperty = Expertise("skill we do not possess")

        val given = Ezvcard.parse(ContactVCardSample.marioVCardType3).first().apply {
            addExpertise(givenNotSupportedProperty)
            extendedProperties.add(givenNotSupportedRawProperty)
        }

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        // properties copied
        assertNotNull(actual.structuredName.given)
        assertTrue(actual.telephoneNumbers.size == 9)
        assertTrue(actual.addresses.size == 4)
        assertTrue(actual.notes.size == 2)
        assertTrue(actual.organizations.size == 2)
        assertTrue(actual.titles.size == 1)
        assertTrue(actual.logos.size == 1)
        assertTrue(actual.photos.size == 1)
        assertTrue(actual.roles.size == 1)
        assertTrue(actual.timezones.size == 1)
        assertTrue(actual.members.size == 1)
        assertTrue(actual.languages.size == 1)
        assertTrue(actual.urls.size == 1)
        assertTrue(actual.anniversaries.size == 1)
        assertNotNull(actual.birthday.date)
        assertNotNull(actual.gender)

        // properties not copied
        assertTrue(actual.expertise.isEmpty())
        assertNull(actual.getExtendedProperty(givenNotSupportedRawProperty.propertyName))
    }

    @Test
    fun `sanitizeAndBuildVCard copies KEY properties from original VCARD`() = runTest {
        // Given
        val given = Ezvcard.parse(ContactVCardSample.stefanoVCardType2).first()

        // When
        val actual = given.sanitizeAndBuildVCard()

        // Then
        assertTrue(actual.keys.size == 2)

        assertTrue(actual.keys[0].pref == 1)
        assertNotNull(actual.keys[0].data)

        assertTrue(actual.keys[1].pref == 2)
        assertNotNull(actual.keys[1].data)
    }

}
