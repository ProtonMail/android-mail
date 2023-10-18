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

package ch.protonmail.android.mailcomposer.presentation.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.AddressSignature
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetAddressSignature
import ch.protonmail.android.mailcomposer.domain.usecase.InjectAddressSignature
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class InjectAddressSignatureTest {

    private val getAddressSignatureMock = mockk<GetAddressSignature>()

    private val injectAddressSignature = InjectAddressSignature(getAddressSignatureMock)

    @Test
    fun `returns draft body with injected signature when previous signature was found`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val previousSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedSignature = expectSignatureForSenderAddress(userId, senderEmail)
        val expectedPreviousSignature = expectSignatureForSenderAddress(userId, previousSenderEmail)
        val existingBody = DraftBody(
            """
                The body of my important message, originally with signature of previous sender.
                
                
                ${expectedPreviousSignature.plaintext}
            """.trimIndent()
        )

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail, previousSenderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(
            """
                The body of my important message, originally with signature of previous sender.
                
                
                ${expectedSignature.plaintext}
            """.trimIndent()
        )

        assertEquals(expectedBodyWithSignature, actual)
    }

    @Test
    fun `returns draft body with injected signature when previous signature was not found`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val previousSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedSignature = expectSignatureForSenderAddress(userId, senderEmail)
        expectSignatureForSenderAddress(userId, previousSenderEmail)
        val existingBody = DraftBody(
            """
                The body of my important message.
            """.trimIndent()
        )

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail, previousSenderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(
            """
                The body of my important message.
                
                
                ${expectedSignature.plaintext}
            """.trimIndent()
        )

        assertEquals(expectedBodyWithSignature, actual)
    }

    private fun expectSignatureForSenderAddress(
        expectedUserId: UserId,
        expectedSenderEmail: SenderEmail
    ): AddressSignature = AddressSignature(
        "<div>HTML signature ($expectedSenderEmail)</div>",
        "Plaintext signature ($expectedSenderEmail)"
    ).also {
        coEvery { getAddressSignatureMock(expectedUserId, expectedSenderEmail) } returns it.right()
    }

}
