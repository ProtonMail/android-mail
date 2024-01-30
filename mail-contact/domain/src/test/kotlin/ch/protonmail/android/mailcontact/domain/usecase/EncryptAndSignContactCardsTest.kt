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
import ch.protonmail.android.mailcontact.domain.mapper.mapToClearTextContactCard
import ch.protonmail.android.mailcontact.domain.mapper.mapToEncryptedAndSignedContactCard
import ch.protonmail.android.mailcontact.domain.mapper.mapToSignedContactCard
import ch.protonmail.android.mailcontact.domain.model.ContactProperty
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ch.protonmail.android.testdata.user.UserIdTestData
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.property.Uid
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.encryptAndSignContactCard
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.contact.domain.signContactCard
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

    private val sut = EncryptAndSignContactCards(
        userManagerMock,
        cryptoContextMock,
        contactRepositoryMock,
        decryptContactCardsMock
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
    fun `return empty list for empty list of original VCards`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(
            ContactSample.Francesco.id // Francesco has no ContactCards
        )

        expectContactRepositorySuccess()

        // When
        val actual = sut(userId, expectedDecryptedContact)

        // Then
        assertEquals(listOf<DecryptedVCard>().right(), actual)
        mockkStatic(::mapToClearTextContactCard) {
            verify { mapToClearTextContactCard(any(), any())?.wasNot(Called) }
        }
        mockkStatic(::mapToSignedContactCard) {
            verify { mapToSignedContactCard(any(), any(), any(), any()).wasNot(Called) }
        }
        mockkStatic(::mapToEncryptedAndSignedContactCard) {
            verify { mapToEncryptedAndSignedContactCard(any(), any(), any()).wasNot(Called) }
        }
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

        val cardToSign = expectMapToSignedContactCard(
            fallbackUid,
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
        fallbackUid: Uid,
        fallbackName: String,
        decryptedContact: DecryptedContact,
        existingVCard: VCard?,
        mappedVCard: VCard
    ): VCard {
        mockkStatic(::mapToSignedContactCard) {
            every {
                mapToSignedContactCard(fallbackUid, fallbackName, decryptedContact, existingVCard)
            } returns mappedVCard
        }

        return mappedVCard
    }

    private fun expectMapToEncryptedAndSignedContactCard(
        fallbackUid: Uid,
        decryptedContact: DecryptedContact,
        existingVCard: VCard?,
        mappedVCard: VCard
    ): VCard {
        mappedVCard.apply {
            uid = uid ?: fallbackUid
        }

        mockkStatic(::mapToEncryptedAndSignedContactCard) {
            every {
                mapToEncryptedAndSignedContactCard(fallbackUid, decryptedContact, existingVCard)
            } returns mappedVCard
        }

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
        mockkStatic(KeyHolderContext::signContactCard)
        every {
            any<KeyHolderContext>().signContactCard(vCard)
        } returns ContactCard.Signed("signed vCard UID: ${vCard.uid.value}", "signature")
    }

    private fun expectEncryptAndSignContactCard(vCard: VCard) {
        mockkStatic(KeyHolderContext::encryptAndSignContactCard)
        every {
            any<KeyHolderContext>().encryptAndSignContactCard(vCard)
        } returns ContactCard.Encrypted("encrypted and signed vCard UID: ${vCard.uid.value}", "signature")
    }

}
