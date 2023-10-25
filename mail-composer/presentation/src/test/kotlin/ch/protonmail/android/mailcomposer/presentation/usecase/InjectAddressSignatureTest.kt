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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class InjectAddressSignatureTest {

    private val getAddressSignatureMock = mockk<GetAddressSignature>()
    private val getMobileFooterMock = mockk<GetMobileFooter>()

    private val injectAddressSignature = InjectAddressSignature(getAddressSignatureMock, getMobileFooterMock)

    private val paidMobileFooter = ""
    private val freeMobileFooter = "\n\nSent from Proton Mail mobile"

    @Test
    fun `returns draft body with injected signature when previous signature was found, free user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val previousSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedSignature = expectSignatureForSenderAddress(userId, senderEmail)
        val expectedPreviousSignature = expectSignatureForSenderAddress(userId, previousSenderEmail)
        val expectedMobileFooter = expectMobileFooter(userId, isUserPaid = false)
        val existingBody = DraftBody(
            "The body of my important message, originally with signature of previous sender." +
                expectedPreviousSignature.plaintext +
                expectedMobileFooter
        )

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail, previousSenderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(
            "The body of my important message, originally with signature of previous sender." +
                expectedSignature.plaintext +
                expectedMobileFooter
        )

        assertEquals(expectedBodyWithSignature, actual)
    }

    @Test
    fun `returns draft body with injected signature when previous signature was not found, free user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val previousSenderEmail = SenderEmail(UserAddressSample.AliasAddress.email)
        val expectedSignature = expectSignatureForSenderAddress(userId, senderEmail)
        expectSignatureForSenderAddress(userId, previousSenderEmail)
        val expectedMobileFooter = expectMobileFooter(userId, isUserPaid = false)
        val existingBody = DraftBody(
            "The body of my important message."
        )

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail, previousSenderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(
            "The body of my important message." +
                expectedSignature.plaintext +
                expectedMobileFooter
        )

        assertEquals(expectedBodyWithSignature, actual)
    }

    @Test
    fun `returns draft body with injected blank signature into blank draft body, paid user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSignature = expectBlankSignatureForSenderAddress(userId, senderEmail)
        val existingBody = DraftBody("")
        expectMobileFooter(userId, isUserPaid = true)

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(paidMobileFooter)

        assertEquals(expectedBodyWithSignature, actual)
    }

    @Test
    fun `returns draft body with injected blank signature into blank draft body, free user`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
        val expectedSignature = expectBlankSignatureForSenderAddress(userId, senderEmail)
        val existingBody = DraftBody("")
        expectMobileFooter(userId, isUserPaid = false)

        // When
        val actual = injectAddressSignature(userId, existingBody, senderEmail).getOrNull()!!

        // Then
        val expectedBodyWithSignature = DraftBody(freeMobileFooter)

        assertEquals(expectedBodyWithSignature, actual)
    }

    private fun expectSignatureForSenderAddress(
        expectedUserId: UserId,
        expectedSenderEmail: SenderEmail
    ): AddressSignature = AddressSignature(
        "<div>HTML signature ($expectedSenderEmail)</div>",
        "${AddressSignature.SeparatorPlaintext}Plaintext signature ($expectedSenderEmail)"
    ).also {
        coEvery { getAddressSignatureMock(expectedUserId, expectedSenderEmail) } returns it.right()
    }

    private fun expectBlankSignatureForSenderAddress(
        expectedUserId: UserId,
        expectedSenderEmail: SenderEmail
    ): AddressSignature = AddressSignature(
        "<div></div>",
        ""
    ).also {
        coEvery { getAddressSignatureMock(expectedUserId, expectedSenderEmail) } returns it.right()
    }

    private fun expectMobileFooter(expectedUserId: UserId, isUserPaid: Boolean): String =
        (if (isUserPaid) paidMobileFooter else freeMobileFooter).also {
            coEvery { getMobileFooterMock(expectedUserId) } returns it.right()
        }

}
