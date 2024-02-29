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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcontact.domain.encryptAndSignNoTrailingSpacesTrim
import ch.protonmail.android.mailcontact.domain.mapper.DecryptedContactMapper
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.sanitizeAndBuildVCard
import ch.protonmail.android.mailcontact.domain.signNoTrailingSpacesTrim
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ch.protonmail.android.testdata.user.UserIdTestData
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.property.Uid
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.user.domain.UserManager
import org.junit.Test
import kotlin.test.assertNotNull

@Suppress("MaxLineLength")
class EncryptAndSignContactCardsTest {

    private val userId = UserSample.Primary.userId
    private val contactId = ContactWithCardsSample.Mario.id

    private val fallbackUid = Uid("Fallback-Uid")

    private val userManagerMock = mockk<UserManager> {
        coEvery { getUser(any()) } returns UserSample.UserWithKeys
    }

    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns mockk(relaxed = true)
        every { keyStoreCrypto } returns mockk(relaxed = true)
    }

    private val contactRepositoryMock = mockk<ContactRepository>()

    private val decryptContactCardsMock = mockk<DecryptContactCards>()

    private val decryptedContactMapperMock = mockk<DecryptedContactMapper>()

    private val sut = EncryptAndSignContactCards(
        userManagerMock,
        cryptoContextMock,
        contactRepositoryMock,
        decryptContactCardsMock,
        decryptedContactMapperMock
    )

    @Test
    fun `return error when ContactRepository fails`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(
            contactId
        )

        expectContactRepositoryError()

        // When
        val actual = sut(userId, expectedDecryptedContact)

        // Then
        assertEquals(EncryptingContactCardsError.ContactNotFoundInDB.left(), actual)
    }

    @Test
    fun `return error when DecryptContactCards fails`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(
            contactId
        )

        expectContactRepositorySuccess()
        expectDecryptContactCardsError()

        // When
        val actual = sut(userId, expectedDecryptedContact)

        // Then
        assertEquals(EncryptingContactCardsError.DecryptingContactCardError.left(), actual)
    }

    @Test
    fun `return Signed + EncryptedAndSigned for DecryptedContact`() = runTest {
        // Given
        expectContactRepositorySuccess()

        val expectedDecryptedContact = DecryptedContact(
            ContactSample.Mario.id,
            emails = listOf(
                ContactProperty.Email(
                    ContactProperty.Email.Type.Home,
                    "mario_secret_email@proton.me"
                )
            )
        )

        val expectedSignedVCard = expectDecryptContactCardsSuccess(
            ContactWithCardsSample.Mario.copy(
                contactCards = listOf(ContactWithCardsSample.Mario.contactCards[0])
            )
        )
        val expectedEncryptedVCard = expectDecryptContactCardsSuccess(
            ContactWithCardsSample.Mario.copy(
                contactCards = listOf(ContactWithCardsSample.Mario.contactCards[1])
            )
        )

        val fallbackUid = expectedSignedVCard.first().card.uid
            ?: expectedEncryptedVCard.first().card.uid
            ?: fallbackUid

        coEvery {
            decryptedContactMapperMock.mapToClearTextContactCard(any(), any())
        } returns null

        val cardToSign = expectMapToSignedContactCard(
            ContactSample.Mario.name,
            expectedDecryptedContact,
            expectedSignedVCard.first().card,
            expectedSignedVCard.first().card
        )

        val cardToEncrypt = expectMapToEncryptedAndSignedContactCard(
            fallbackUid,
            expectedDecryptedContact,
            expectedEncryptedVCard.first().card,
            expectedEncryptedVCard.first().card
        )

        expectSignContactCard(cardToSign)
        expectEncryptAndSignContactCard(cardToEncrypt)

        // When
        val actual = sut(userId, expectedDecryptedContact)

        // Then
        assertEquals(actual.getOrNull()!!.size, 2)
        assertNotNull(actual.getOrNull()!!.find { it is ContactCard.Signed })
        assertNotNull(actual.getOrNull()!!.find { it is ContactCard.Encrypted && it.signature!!.isNotBlank() })
    }

    @Test
    fun `return Signed + EncryptedAndSigned for DecryptedContact with null contactId`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(
            null,
            formattedName = ContactProperty.FormattedName("Mario@protonmail.com"),
            emails = listOf(
                ContactProperty.Email(
                    ContactProperty.Email.Type.Home,
                    "mario_secret_email@proton.me"
                )
            )
        )

        mockkStatic(Uid::random)
        every {
            Uid.random()
        } returns fallbackUid

        val emptySanitizedVCard = VCard().sanitizeAndBuildVCard()

        coEvery {
            decryptedContactMapperMock.mapToClearTextContactCard(emptySanitizedVCard, any())
        } returns null

        val mappedSignedVCard = DecryptedVCard(
            Ezvcard.parse(
                ContactCard.Signed(
                    """
                        BEGIN:VCARD
                        VERSION:4.0
                        FN;PREF=1:Mario@protonmail.com
                        UID:Fallback-Uid
                        ITEM1.EMAIL;TYPE=home;PREF=1:mario_secret_email@proton.me
                        END:VCARD
                    """.trimIndent(),
                    "signature"
                ).data
            ).first(),
            VerificationStatus.Success
        ).card
        expectMapToSignedContactCard(
            expectedDecryptedContact.formattedName!!.value,
            expectedDecryptedContact,
            emptySanitizedVCard,
            mappedSignedVCard
        )

        val mappedEncryptedVCard = DecryptedVCard(
            Ezvcard.parse(
                ContactCard.Encrypted(
                    """
                        BEGIN:VCARD
                        VERSION:4.0
                        UID:Fallback-Uid
                        END:VCARD
                    """.trimIndent(),
                    "signature"
                ).data
            ).first(),
            VerificationStatus.Success
        ).card
        expectMapToEncryptedAndSignedContactCard(
            fallbackUid,
            expectedDecryptedContact,
            emptySanitizedVCard,
            mappedEncryptedVCard
        )

        expectSignContactCard(mappedSignedVCard)
        expectEncryptAndSignContactCard(mappedEncryptedVCard)

        // When
        val actual = sut(userId, expectedDecryptedContact)

        // Then
        assertEquals(actual.getOrNull()!!.size, 2)
        assertNotNull(actual.getOrNull()!!.find { it is ContactCard.Signed })
        assertNotNull(actual.getOrNull()!!.find { it is ContactCard.Encrypted && it.signature!!.isNotBlank() })
    }

    private fun expectContactRepositoryError() {
        every {
            contactRepositoryMock.observeContactWithCards(userId, contactId)
        } returns flowOf(DataResult.Error.Local("local error", null))
    }

    private fun expectContactRepositorySuccess() {
        every {
            contactRepositoryMock.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Mario.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Mario)
        )
        every {
            contactRepositoryMock.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Francesco.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Francesco)
        )
    }

    private fun expectDecryptContactCardsError() {
        coEvery {
            decryptContactCardsMock.invoke(UserSample.Primary.userId, any())
        } returns GetContactError.left()
    }

    private fun expectMapToSignedContactCard(
        fallbackName: String,
        decryptedContact: DecryptedContact,
        existingVCard: VCard,
        mappedVCard: VCard
    ): VCard {
        coEvery {
            decryptedContactMapperMock.mapToSignedContactCard(fallbackName, decryptedContact, existingVCard)
        } returns mappedVCard

        return mappedVCard
    }

    private fun expectMapToEncryptedAndSignedContactCard(
        fallbackUid: Uid,
        decryptedContact: DecryptedContact,
        existingVCard: VCard,
        mappedVCard: VCard
    ): VCard {
        mappedVCard.apply {
            uid = uid ?: fallbackUid
        }

        coEvery {
            decryptedContactMapperMock.mapToEncryptedAndSignedContactCard(decryptedContact, existingVCard)
        } returns mappedVCard

        return mappedVCard
    }

    private fun expectDecryptContactCardsSuccess(contactWithCards: ContactWithCards): List<DecryptedVCard> {
        val cards = contactWithCards.contactCards.map {
            val data = when (it) {
                is ContactCard.ClearText -> it.data
                is ContactCard.Encrypted -> it.data
                is ContactCard.Signed -> it.data
            }
            DecryptedVCard(
                Ezvcard.parse(data).first(),
                VerificationStatus.Success
            )
        }

        coEvery {
            decryptContactCardsMock.invoke(UserSample.Primary.userId, contactWithCards)
        } returns cards.right()

        return cards
    }

    private fun expectSignContactCard(vCard: VCard) {
        mockkStatic(KeyHolderContext::signNoTrailingSpacesTrim)
        every {
            any<KeyHolderContext>().signNoTrailingSpacesTrim(vCard)
        } returns ContactCard.Signed("signed vCard UID: ${vCard.uid.value}", "signature")
    }

    private fun expectEncryptAndSignContactCard(vCard: VCard) {
        mockkStatic(KeyHolderContext::encryptAndSignNoTrailingSpacesTrim)
        every {
            any<KeyHolderContext>().encryptAndSignNoTrailingSpacesTrim(vCard)
        } returns ContactCard.Encrypted("encrypted and signed vCard UID: ${vCard.uid.value}", "signature")
    }

}
