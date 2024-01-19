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

import java.time.LocalDate
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.contact.ContactVCardSample
import ezvcard.Ezvcard
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.user.domain.entity.User
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class GetDecryptedContactTest {

    private val user = mockk<User> {
        every { keys } returns emptyList()
        every { userId } returns UserSample.Primary.userId
    }

    private val decryptContactCardsMock = mockk<DecryptContactCards>()

    private val sut = GetDecryptedContact(decryptContactCardsMock)

    private val vCardSignature = "vCardSignature"

    @Test
    fun `parses and applies VCard Properties into DecryptedContact model from ClearText ContactCard`() = runTest {
        // Given
        val vCardClearText = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            FN;PREF=1:Mario_ClearText@protonmail.com
            END:VCARD
        """.trimIndent()

        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.ClearText(vCardClearText)
            )
        )

        expectDecryptContactCards(contactWithCards)

        val expected = DecryptedContact(
            id = ContactSample.Mario.id,
            formattedName = ContactProperty.FormattedName(value = "Mario_ClearText@protonmail.com")
        ).right()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `parses and applies VCard Properties into DecryptedContact model from Signed ContactCard`() = runTest {
        // Given
        val vCardSigned = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            FN;PREF=1:Mario_Signed@protonmail.com
            END:VCARD
        """.trimIndent()

        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.Signed(vCardSigned, vCardSignature)
            )
        )

        expectDecryptContactCards(contactWithCards)

        val expected = DecryptedContact(
            id = ContactSample.Mario.id,
            formattedName = ContactProperty.FormattedName(value = "Mario_Signed@protonmail.com")
        ).right()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `parses and applies VCard Properties into DecryptedContact model from Encrypted ContactCard`() = runTest {
        // Given
        val vCardEncrypted = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            FN;PREF=1:Mario_Encrypted@protonmail.com
            END:VCARD
        """.trimIndent()

        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.Encrypted(vCardEncrypted, vCardSignature)
            )
        )

        expectDecryptContactCards(contactWithCards)

        val expected = DecryptedContact(
            id = ContactSample.Mario.id,
            formattedName = ContactProperty.FormattedName(value = "Mario_Encrypted@protonmail.com")
        ).right()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `does not apply VCard Properties into DecryptedContact model from ContactCard that failed signature verification`() =
        runTest {
            // Given
            val vCardSigned = """
                BEGIN:VCARD
                VERSION:4.0
                PRODID:ez-vcard 0.11.3
                FN;PREF=1:Mario_Signed@protonmail.com
                END:VCARD
            """.trimIndent()

            val contactWithCards = ContactWithCards(
                contact = ContactSample.Mario,
                contactCards = listOf(
                    ContactCard.Encrypted(vCardSigned, vCardSignature)
                )
            )

            expectDecryptContactCardsFailureSignatureVerification(contactWithCards)

            val expected = DecryptedContact(
                id = ContactSample.Mario.id,
                formattedName = null
            ).right()

            // When
            val actual = sut(user.userId, contactWithCards)

            // Then
            assertEquals(expected, actual)
        }

    @Test
    fun `returns error from ContactCard that failed decryption`() = runTest {
        // Given
        val vCardEncrypted = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            FN;PREF=1:Mario_Encrypted@protonmail.com
            END:VCARD
        """.trimIndent()

        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.Encrypted(vCardEncrypted, vCardSignature)
            )
        )

        expectDecryptContactCardsFailureDecryption(contactWithCards)

        val expected = GetContactError.left()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `does not crash on malformed data`() = runTest {
        // Given
        val vCardEncrypted = """
            BEGIN:VCARD
            VERSION:4.0
            PRODID:ez-vcard 0.11.3
            BDAY:202312__
            ANNIVERSARY:202312__
            END:VCARD
        """.trimIndent()

        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.Encrypted(vCardEncrypted, vCardSignature)
            )
        )

        expectDecryptContactCards(contactWithCards)

        val expected = DecryptedContact(
            ContactSample.Mario.id
        ).right()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `parses and combines all VCard Properties into DecryptedContact model`() = runTest {
        // Given
        val contactWithCards = ContactWithCards(
            contact = ContactSample.Mario,
            contactCards = listOf(
                ContactCard.Signed(ContactVCardSample.marioVCardType2, vCardSignature),
                ContactCard.Encrypted(ContactVCardSample.marioVCardType3, vCardSignature)
            )
        )

        expectDecryptContactCards(contactWithCards)

        val expected = DecryptedContact(
            id = ContactSample.Mario.id,
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
                ContactProperty.Email(type = ContactProperty.Email.Type.Work, value = "work_email@Mario.protonmail.com"),
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
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Pager, text = "34343434"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Work,
                    text = "45454545"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Other, text = "565656"),
                ContactProperty.Telephone(
                    type = ContactProperty.Telephone.Type.Telephone,
                    text = "676767"
                ),
                ContactProperty.Telephone(type = ContactProperty.Telephone.Type.Telephone, text = "787887"),
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
                    streetAddress = "Work address the rest is empty",
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
                    data = byteArrayOf(0, 0, 1, 0, 1, 0, 32, 32, 0, 0, 1, 0, 32, 0, -88, 16, 0, 0, 22, 0, 0, 0, 40, 0, 0, 0, 32, 0, 0, 0, 64, 0, 0, 0, 1, 0, 32, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 48, 82, -1, 22, 54, 90, -1, 23, 55, 93, -1, 23, 56, 93, -1, 22, 54, 91, -1, 21, 51, 86, -1, 19, 47, 81, -1, 46, 75, 113, -122, 40, 66, 102, -1, 41, 67, 103, -1, 42, 69, 106, -1, 43, 70, 108, -1, 45, 73, 111, -1, 47, 77, 116, -1, 49, 80, 120, -1, 44, 75, 115, -1, 38, 70, 109, -1, 33, 66, 105, -1, 29, 61, 100, -1, 25, 59, 97, -1, 25, 58, 96, -1, 24, 57, 95, -1, 23, 56, 93, -1, 22, 55, 92, -1, 21, 52, 88, -1, 19, 48, 82, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 49, 84, -1, 26, 62, 101, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 105, -1, 28, 65, 105, -1, 27, 64, 105, -1, 27, 64, 105, -1, 27, 64, 104, -1, 27, 64, 103, -1, 25, 60, 99, -1, 22, 55, 92, -1, 40, 66, 103, -1, 47, 75, 114, -1, 53, 85, 126, -1, 58, 93, -120, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 55, 90, -123, -1, 45, 81, 123, -1, 37, 74, 115, -1, 32, 69, 110, -1, 29, 66, 107, -1, 28, 65, 106, -1, 27, 64, 104, -1, 26, 63, 103, -1, 26, 63, 103, -1, 25, 60, 99, -1, 20, 49, 84, -1, 0, 0, 0, 0, 25, 56, 94, 80, 28, 64, 104, -1, 29, 66, 107, -1, 29, 66, 107, -1, 29, 66, 107, -1, 29, 66, 107, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 106, -1, 27, 64, 105, -1, 27, 64, 105, -1, 27, 64, 104, -1, 26, 62, 102, -1, 24, 59, 98, -1, 42, 69, 106, -1, 52, 83, 123, -1, 58, 92, -122, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 53, 89, -125, -1, 43, 79, 121, -1, 35, 72, 113, -1, 30, 67, 108, -1, 28, 65, 106, -1, 27, 64, 105, -1, 26, 63, 103, -1, 26, 63, 103, -1, 25, 61, 100, -1, 0, 0, 0, 0, 25, 55, 92, -1, 32, 69, 110, -1, 32, 69, 110, -1, 32, 69, 110, -1, 32, 69, 110, -1, 32, 69, 110, -1, 32, 68, 110, -1, 31, 68, 109, -1, 30, 67, 108, -1, 30, 67, 108, -1, 29, 66, 107, -1, 29, 65, 107, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 105, -1, 27, 64, 105, -1, 27, 64, 104, -1, 26, 62, 102, -1, 23, 57, 95, -1, 48, 78, 117, -1, 56, 90, -124, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 48, 84, 126, -1, 38, 74, 116, -1, 32, 68, 110, -1, 28, 65, 106, -1, 28, 65, 105, -1, 26, 63, 103, -1, 26, 63, 103, -1, 21, 51, 87, -1, 29, 60, 97, -1, 37, 73, 115, -1, 38, 74, 116, -1, 38, 75, 116, -1, 39, 75, 117, -1, 39, 75, 117, -1, 38, 75, 116, -1, 37, 74, 115, -1, 36, 73, 114, -1, 35, 72, 113, -1, 34, 70, 112, -1, 32, 69, 110, -1, 31, 68, 109, -1, -41, -41, -41, -1, -15, -15, -15, -1, -7, -7, -7, -1, -7, -7, -7, -1, -19, -19, -19, -1, -49, -49, -49, -1, 26, 63, 104, -1, 24, 59, 97, -1, 47, 76, 114, -1, 57, 91, -123, -1, 59, 94, -119, -1, 59, 94, -119, -1, 52, 87, -126, -1, 41, 77, 119, -1, 33, 70, 111, -1, 29, 66, 107, -1, 28, 65, 106, -1, 26, 63, 104, -1, 23, 55, 92, -1, 29, 57, 91, -1, 44, 80, 122, -1, 45, 81, 123, -1, 47, 83, 125, -1, 48, 84, 126, -1, 48, 84, 126, -1, 48, 84, 126, -1, 47, 83, 125, -1, 46, 82, 124, -1, 44, 80, 122, -1, 42, 78, 120, -1, -53, -53, -53, -1, -12, -12, -12, -1, -10, -10, -10, -1, -10, -10, -10, -1, -10, -10, -10, -1, -10, -10, -10, -1, -10, -10, -10, -1, -10, -10, -10, -1, -16, -17, -16, -1, -66, -67, -66, -1, 26, 63, 103, -1, 23, 55, 92, -1, 51, 81, 121, -1, 59, 93, -120, -1, 59, 94, -119, -1, 54, 89, -124, -1, 42, 78, 120, -1, 33, 70, 111, -1, 29, 66, 107, -1, 28, 65, 106, -1, 22, 53, 89, -1, 0, 0, 0, 0, 47, 79, 120, -1, 55, 90, -123, -1, 57, 92, -121, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 57, 92, -121, -1, -88, -89, -87, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -57, -58, -57, -1, -58, -59, -59, -1, -97, -98, -97, -1, 27, 64, 104, -1, 25, 60, 100, -1, 45, 73, 111, -1, 57, 91, -122, -1, 59, 94, -119, -1, 53, 89, -125, -1, 41, 77, 119, -1, 33, 70, 111, -1, 28, 64, 104, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 53, 86, 127, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 92, 112, -117, -1, 92, 112, -117, -1, 92, 112, -117, -1, 92, 111, -117, -1, 88, 109, -120, -1, 85, 106, -124, -1, 81, 104, -126, -1, 79, 101, 127, -1, 77, 98, 125, -1, 75, 97, 123, -1, 74, 96, 122, -1, 73, 95, 121, -1, 28, 65, 106, -1, 28, 65, 105, -1, 27, 63, 103, -1, 20, 49, 84, -1, 55, 89, -126, -1, 59, 94, -119, -1, 50, 86, -128, -1, 37, 73, 113, -1, 23, 51, 86, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 43, 70, 107, -1, 48, 78, 118, -1, 50, 81, 120, -1, 52, 83, 123, -1, 54, 86, 127, -1, 56, 89, -125, -1, 58, 92, -122, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 56, 91, -122, -1, 50, 86, -128, -1, 45, 80, 123, -1, 40, 76, 118, -1, 35, 72, 113, -1, 32, 69, 110, -1, 30, 67, 108, -1, 28, 65, 106, -1, 28, 65, 106, -1, 27, 63, 104, -1, 20, 49, 83, -1, 53, 86, 127, -1, 51, 83, 123, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 48, 82, -11, 26, 60, 99, -1, 26, 60, 99, -1, 25, 58, 97, -1, 24, 56, 93, -1, -31, -32, -31, -1, -11, -11, -11, -1, -16, -16, -16, -1, -41, -42, -42, -1, 56, 90, -124, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 57, 93, -121, -1, -45, -46, -45, -1, -22, -23, -23, -1, -16, -17, -17, -1, -28, -29, -29, -1, 31, 68, 109, -1, 29, 66, 107, -1, 28, 65, 106, -1, 27, 63, 102, -1, 21, 50, 85, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 61, 100, -1, 33, 69, 111, -1, 33, 69, 111, -1, 32, 69, 110, -1, -30, -31, -31, -1, -2, -2, -2, -1, -2, -2, -2, -1, -2, -2, -2, -1, -3, -3, -3, -1, -46, -47, -46, -1, 20, 46, 79, -6, 50, 80, 120, -1, 56, 90, -124, -1, 59, 94, -119, -1, 59, 94, -119, -1, -7, -7, -7, -1, -2, -2, -2, -1, -2, -2, -2, -1, -2, -2, -2, -1, -26, -27, -27, -1, 33, 69, 111, -1, 30, 67, 108, -1, 28, 65, 106, -1, 26, 61, 101, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 71, 111, -1, 41, 77, 119, -1, 41, 77, 119, -1, 40, 76, 118, -1, -5, -5, -5, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -4, -4, -4, -1, -22, -23, -23, -1, 28, 64, 105, -1, 26, 60, 99, -1, 21, 51, 87, -1, 44, 72, 110, -1, -28, -29, -29, -1, -4, -4, -4, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -4, -4, -4, -1, -48, -49, -48, -1, 34, 71, 112, -1, 31, 68, 109, -1, 28, 63, 103, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 44, 76, 116, -1, 52, 87, -126, -1, 53, 88, -125, -1, -43, -44, -44, -1, -6, -7, -6, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -7, -8, -7, -1, 32, 69, 110, -1, 30, 67, 108, -1, 28, 65, 106, -1, 27, 63, 103, -1, -9, -9, -9, -1, -6, -6, -6, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -6, -6, -6, -1, -38, -38, -38, -1, 41, 77, 119, -1, 35, 72, 113, -1, 28, 63, 102, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 43, 71, 108, -1, 59, 93, -120, -1, 59, 94, -119, -1, -43, -44, -44, -1, -10, -10, -10, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -10, -10, -10, -1, 42, 78, 120, -1, 37, 74, 115, -1, 33, 70, 111, -1, 30, 67, 108, -1, -12, -12, -11, -1, -111, -111, -110, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -10, -10, -9, -1, -36, -37, -37, -1, 48, 84, 126, -1, 41, 77, 119, -1, 28, 58, 94, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 78, 118, -1, 58, 92, -121, -1, -55, -56, -54, -1, -15, -15, -15, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -15, -15, -15, -1, -19, -19, -19, -1, 58, 93, -120, -1, 50, 86, -128, -1, 43, 79, 121, -1, 37, 74, 115, -1, -27, -28, -27, -1, -14, -14, -14, -1, 17, 17, 16, -1, 17, 17, 16, -1, 17, 17, 16, -1, -14, -14, -14, -1, -47, -48, -48, -1, 56, 91, -123, -1, 42, 73, 113, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 78, 118, 42, 49, 80, 119, -1, -29, -29, -29, -1, -21, -21, -21, -1, 17, 17, 16, -1, 17, 17, 16, -1, -21, -21, -21, -1, -46, -47, -47, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 50, 86, -128, -1, -49, -50, -50, -1, -20, -20, -19, -1, 17, 17, 16, -1, 17, 17, 16, -1, -20, -20, -19, -1, -21, -21, -20, -1, 41, 68, 104, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -63, -64, -64, -1, -30, -30, -30, -1, -28, -28, -28, -1, -28, -28, -28, -1, -42, -43, -43, -1, 50, 81, 121, -1, 56, 90, -124, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 57, 92, -121, -1, -45, -46, -46, -1, -26, -26, -26, -1, -26, -26, -26, -1, -30, -30, -29, -1, -58, -59, -58, -1, 21, 50, 86, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 53, 89, -1, 31, 67, 109, -1, 30, 66, 108, -1, -61, -62, -62, -1, -64, -65, -64, -1, 28, 64, 105, -1, 25, 60, 98, -1, 21, 52, 88, -1, 42, 69, 106, -1, 52, 84, 125, -1, 59, 93, -120, -1, 59, 94, -119, -1, 59, 94, -119, -1, -66, -67, -66, -1, -61, -62, -60, -1, 108, 127, -106, -1, 31, 67, 109, -1, 23, 54, 91, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 30, 59, 96, -1, 36, 73, 114, -1, 35, 71, 113, -1, 33, 69, 111, -1, 30, 67, 108, -1, 29, 66, 107, -1, 28, 65, 106, -1, 28, 65, 105, -1, 27, 63, 103, -1, 23, 56, 93, -1, 41, 68, 105, -1, 55, 87, -127, -1, 59, 94, -119, -1, 59, 94, -119, -1, 54, 90, -124, -1, 44, 80, 122, -1, 36, 73, 113, -1, 23, 52, 87, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45, 80, 122, -1, 44, 80, 122, -1, 40, 77, 118, -1, 37, 73, 115, -1, 33, 70, 111, -1, 30, 67, 108, -1, 29, 66, 107, -1, 28, 65, 106, -1, 27, 64, 105, -1, 25, 61, 100, -1, 20, 50, 85, -1, 51, 82, 122, -1, 59, 94, -119, -1, 59, 94, -119, -1, 56, 91, -122, -1, 39, 70, 109, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 78, 117, -1, 56, 91, -122, -1, 52, 88, -126, -1, 47, 83, 125, -1, 42, 78, 120, -1, 36, 73, 114, -1, 32, 69, 110, -1, 29, 66, 107, -1, 28, 65, 106, -1, 27, 64, 105, -1, 26, 63, 103, -1, 22, 54, 90, -1, 48, 78, 117, -1, 52, 84, 125, -1, 46, 75, 113, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 81, 120, -1, 59, 94, -119, -1, 59, 94, -119, -1, 54, 89, -124, -1, 47, 82, 125, -1, 40, 76, 118, -1, 34, 71, 112, -1, 30, 67, 108, -1, 28, 65, 106, -1, 28, 65, 105, -1, 26, 63, 104, -1, 21, 53, 89, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 43, 70, 107, -1, 53, 86, 127, -1, 59, 94, -119, -1, 59, 94, -119, -1, 51, 87, -127, -1, 43, 79, 121, -1, 36, 72, 114, -1, 31, 68, 109, -1, 28, 65, 106, -1, 28, 65, 105, -1, 24, 57, 95, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 67, 104, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 55, 90, -123, -1, 45, 81, 123, -1, 37, 73, 115, -1, 31, 68, 109, -1, 28, 65, 106, -1, 23, 54, 90, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 47, 76, 115, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 57, 92, -121, -1, 46, 82, 124, -1, 37, 73, 115, -1, 30, 66, 107, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 43, 70, 107, -12, 59, 93, -120, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 58, 93, -120, -1, 45, 80, 122, -1, 29, 58, 95, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 52, 84, 125, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 58, 93, -120, -1, 44, 73, 112, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 59, 93, -120, -1, 59, 94, -119, -1, 59, 94, -119, -1, 56, 90, -124, -1, 43, 71, 108, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57, 91, -122, -1, 58, 93, -120, -1, 52, 83, 124, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 41, 67, 103, -1, 48, 77, 116, -1, 41, 68, 105, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -32, 0, 0, 7, -128, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 1, -64, 0, 0, 1, -32, 0, 0, 7, -16, 0, 0, 15, -16, 0, 0, 15, -16, 0, 0, 15, -16, 0, 0, 15, -16, 0, 0, 15, -8, 0, 0, 31, -4, 0, 0, 127, -1, 0, 0, 127, -2, 0, 0, 127, -2, 0, 0, 127, -1, 0, 0, -1, -1, 0, 1, -1, -1, -128, 7, -1, -1, -64, 7, -1, -1, -32, 7, -1, -1, -32, 15, -1, -1, -32, 15, -1, -1, -16, 31, -1, -1, -8, 63, -1, -1, -8, -1, -1, -1, -15, -1, -1, -1, -1, -1, -1),
                    contentType = null,
                    mediaType = "image/vnd.microsoft.icon",
                    extension = null
                )
            ),
            organizations = listOf(ContactProperty.Organization(value = "Organization1"), ContactProperty.Organization(value = "Organization2")),
            titles = listOf(ContactProperty.Title(value = "Title")),
            roles = listOf(ContactProperty.Role(value = "Role")),
            timezones = listOf(ContactProperty.Timezone(text = "Europe/Paris")),
            logos = listOf(
                ContactProperty.Logo(
                    data = byteArrayOf(0, 0, 1, 0, 1, 0, 16, 16, 0, 0, 1, 0, 32, 0, 104, 4, 0, 0, 22, 0, 0, 0, 40, 0, 0, 0, 16, 0, 0, 0, 32, 0, 0, 0, 1, 0, 32, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 49, 84, -47, 21, 51, 87, -106, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 48, 82, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 106, -1, 28, 65, 105, -1, 27, 64, 105, -1, 25, 61, 100, -1, 39, 66, 102, -1, 56, 90, -124, -1, 59, 94, -119, -1, 59, 94, -119, -1, 40, 76, 118, -1, 29, 66, 107, -1, 27, 64, 104, -1, 26, 63, 103, -1, 18, 46, 79, -1, 33, 68, 109, -1, 35, 71, 113, -1, 35, 72, 113, -1, 34, 71, 112, -1, 32, 69, 110, -1, 30, 67, 108, -1, 29, 66, 107, -1, -55, -55, -55, -1, -58, -59, -58, -1, 25, 62, 101, -1, 53, 85, 126, -1, 59, 94, -119, -1, 50, 86, -128, -1, 32, 69, 110, -1, 28, 65, 105, -1, 25, 62, 101, -1, 38, 69, 107, -1, 51, 86, -127, -1, 54, 89, -124, -1, 54, 89, -124, -1, 51, 87, -127, -1, -58, -58, -58, -1, -24, -24, -23, -1, -24, -24, -23, -1, -24, -24, -23, -1, -24, -24, -23, -1, -71, -71, -71, -1, 24, 58, 96, -1, 58, 92, -121, -1, 54, 89, -124, -1, 33, 70, 111, -1, 25, 60, 98, -1, 0, 0, 0, 0, 51, 82, 122, -1, 58, 93, -121, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 58, 93, -120, -1, 47, 82, 125, -1, 37, 74, 115, -1, 31, 68, 109, -1, 28, 65, 106, -1, 27, 63, 104, -1, 56, 89, -125, -1, 45, 79, 120, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28, 63, 102, -1, 30, 67, 108, -1, -27, -28, -28, -1, -1, -1, -1, -1, -40, -41, -41, -1, 56, 89, -125, -1, 59, 94, -119, -1, -45, -46, -45, -1, -1, -1, -1, -1, -23, -24, -24, -1, 30, 66, 108, -1, 27, 64, 104, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45, 81, 123, -1, 46, 82, 124, -1, -5, -5, -5, -1, 17, 17, 16, -1, -5, -5, -5, -1, 29, 66, 107, -1, 25, 59, 96, -1, -5, -5, -5, -1, 17, 17, 16, -1, -5, -5, -5, -1, 40, 76, 118, -1, 31, 68, 109, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 77, 116, -1, 59, 94, -119, -1, -12, -12, -12, -1, 17, 17, 16, -1, -12, -12, -12, -1, 46, 82, 124, -1, 35, 72, 113, -1, -12, -12, -12, -1, 17, 17, 16, -1, 17, 17, 16, -1, 58, 93, -120, -1, 35, 68, 106, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -25, -25, -25, -1, -24, -24, -24, -1, -41, -42, -42, -1, 59, 94, -119, -1, 59, 94, -119, -1, -45, -46, -46, -1, -23, -23, -23, -1, -23, -23, -23, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 69, 110, -1, 29, 66, 107, -1, 28, 65, 106, -1, 24, 58, 98, -1, 48, 77, 116, -1, 59, 94, -119, -1, 53, 88, -125, -1, 36, 72, 114, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 51, 86, -127, -1, 44, 80, 122, -1, 35, 71, 113, -1, 29, 66, 107, -1, 27, 64, 105, -1, 22, 53, 89, -1, 58, 92, -121, -1, 47, 78, 117, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57, 91, -122, -1, 58, 93, -120, -1, 41, 77, 119, -1, 31, 67, 109, -1, 28, 65, 105, -1, 20, 49, 84, -61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 59, 94, -119, -1, 59, 94, -119, -1, 45, 81, 123, -1, 31, 68, 109, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 59, 94, -119, -1, 59, 94, -119, -1, 59, 94, -119, -1, 36, 66, 104, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50, 80, 120, -1, 59, 94, -119, -1, 41, 67, 103, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128, 1, 0, 0, -64, 3, 0, 0, -64, 3, 0, 0, -64, 3, 0, 0, -16, 15, 0, 0, -16, 15, 0, 0, -16, 15, 0, 0, -8, 31, 0, 0, -4, 63, 0, 0, -4, 63, 0, 0, -4, 127, 0, 0, -1, -1, 0, 0),
                    contentType = null,
                    mediaType = "image/vnd.microsoft.icon",
                    extension = null
                )
            ),
            members = listOf(ContactProperty.Member(value = "Member")),
            languages = listOf(ContactProperty.Language(value = "English")),
            urls = listOf(ContactProperty.Url(value = "http://proton.me")),
            gender = ContactProperty.Gender(gender = "RATHER NOT SAY"),
            anniversary = ContactProperty.Anniversary(date = LocalDate.of(2023, 12, 6))
        ).right()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

    private fun expectDecryptContactCards(contactWithCards: ContactWithCards) {
        coEvery {
            decryptContactCardsMock.invoke(UserSample.Primary.userId, contactWithCards)
        } returns contactWithCards.contactCards.map {
            val data = when (it) {
                is ContactCard.ClearText -> it.data
                is ContactCard.Encrypted -> it.data
                is ContactCard.Signed -> it.data
            }
            DecryptedVCard(
                Ezvcard.parse(data).first(),
                VerificationStatus.Success
            )
        }.right()
    }

    private fun expectDecryptContactCardsFailureSignatureVerification(contactWithCards: ContactWithCards) {
        coEvery {
            decryptContactCardsMock.invoke(UserSample.Primary.userId, contactWithCards)
        } returns contactWithCards.contactCards.map {
            val data = when (it) {
                is ContactCard.ClearText -> it.data
                is ContactCard.Encrypted -> it.data
                is ContactCard.Signed -> it.data
            }
            DecryptedVCard(
                Ezvcard.parse(data).first(),
                VerificationStatus.Failure
            )
        }.right()
    }

    private fun expectDecryptContactCardsFailureDecryption(contactWithCards: ContactWithCards) {
        coEvery {
            decryptContactCardsMock.invoke(UserSample.Primary.userId, contactWithCards)
        } returns GetContactError.left()
    }

}
