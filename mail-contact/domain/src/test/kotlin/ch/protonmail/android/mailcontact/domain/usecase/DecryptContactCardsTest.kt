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
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcontact.domain.decryptContactCardTrailingSpacesFallback
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class DecryptContactCardsTest {

    private val cryptoContextMock = mockk<CryptoContext>()

    private val user = mockk<User> {
        every { keys } returns emptyList()
        every { userId } returns UserSample.Primary.userId
    }

    private val userManagerMock = mockk<UserManager> {
        coEvery { getUser(any()) } returns user
    }

    private val sut = DecryptContactCards(userManagerMock, cryptoContextMock)

    private val vCardSignature = "vCardSignature"

    @Test
    fun `returns error if decryption threw an Exception`() = runTest {
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

        mockkStatic(KeyHolderContext::decryptContactCardTrailingSpacesFallback)
        every {
            any<KeyHolderContext>().decryptContactCardTrailingSpacesFallback(contactWithCards.contactCards.first())
        } throws Exception("decryption exception")

        val expected = GetContactError.left()

        // When
        val actual = sut(user.userId, contactWithCards)

        // Then
        assertEquals(expected, actual)
    }

}
