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

package ch.protonmail.android.presentation.composer

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetAddressSignature
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.blankSignatureHtml
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.malformedSignatureHtml
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.richSignatureHtml
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.senderAddressWithBlankSignature
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.senderAddressWithMalformedSignature
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.senderAddressWithRichSignature
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.senderAddressWithSimpleSignature
import ch.protonmail.android.presentation.composer.GetAddressSignatureTest.Companion.TestData.simpleSignatureHtml
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetAddressSignatureTest {

    private val resolveUserAddressMock = mockk<ResolveUserAddress>()

    private val getAddressSignature = GetAddressSignature(resolveUserAddressMock)

    @Test
    fun convertBlankSignature() = runTest {
        // Given
        expectResolveUserAddress()
        val expectedblankSignaturePlaintext = ""

        // When
        val actual = getAddressSignature(UserIdSample.Primary, SenderEmail(senderAddressWithBlankSignature.email))

        // Then
        assertEquals(actual.getOrNull()?.plaintext, expectedblankSignaturePlaintext)
    }

    @Test
    fun convertSimpleSignature() = runTest {
        // Given
        expectResolveUserAddress()
        val simpleSignaturePlaintext = """
            HTML signature
            Second line
            Third line
        """.trimIndent().trimEnd()

        // When
        val actual = getAddressSignature(
            UserIdSample.Primary, SenderEmail(senderAddressWithSimpleSignature.email)
        )

        // Then
        assertEquals(actual.getOrNull()?.plaintext, simpleSignaturePlaintext)
    }

    @Test
    fun convertRichSignature() = runTest {
        // Given
        expectResolveUserAddress()
        val richSignaturePlaintext = """
            My rich signature
            UL new line 1
            UL new line 2
        """.trimIndent()

        // When
        val actual = getAddressSignature(
            UserIdSample.Primary, SenderEmail(senderAddressWithRichSignature.email)
        )

        // Then
        assertEquals(actual.getOrNull()?.plaintext, richSignaturePlaintext)
    }

    @Test
    fun convertMalformedSignatureToBlank() = runTest {
        // Given
        expectResolveUserAddress()
        val malformedSignaturePlaintext = ""

        // When
        val actual = getAddressSignature(
            UserIdSample.Primary, SenderEmail(senderAddressWithMalformedSignature.email)
        )

        // Then
        assertEquals(actual.getOrNull()?.plaintext, malformedSignaturePlaintext)
    }

    private fun expectResolveUserAddress() {
        coEvery {
            resolveUserAddressMock(UserIdSample.Primary, SenderEmail(senderAddressWithBlankSignature.email))
        } returns senderAddressWithBlankSignature.copy(
            signature = blankSignatureHtml
        ).right()

        coEvery {
            resolveUserAddressMock(UserIdSample.Primary, SenderEmail(senderAddressWithSimpleSignature.email))
        } returns senderAddressWithSimpleSignature.copy(
            signature = simpleSignatureHtml
        ).right()

        coEvery {
            resolveUserAddressMock(UserIdSample.Primary, SenderEmail(senderAddressWithRichSignature.email))
        } returns senderAddressWithRichSignature.copy(
            signature = richSignatureHtml
        ).right()

        coEvery {
            resolveUserAddressMock(UserIdSample.Primary, SenderEmail(senderAddressWithMalformedSignature.email))
        } returns senderAddressWithMalformedSignature.copy(
            signature = malformedSignatureHtml
        ).right()
    }

    companion object {
        object TestData {
            const val blankSignatureHtml = "<div></div>"
            const val simpleSignatureHtml = "<div>HTML signature<br/>Second line<br/>Third line</div>"

            @Suppress("MaxLineLength")
            const val richSignatureHtml =
                "<div style=\"font-family: Arial, sans-serif; font-size: 14px; color: rgb(0, 0, 0); background-color: rgb(255, 255, 255);\">My <span style=\"color: rgb(237, 65, 57);\">rich</span> <b>signature</b></div><div style=\"font-family: Arial, sans-serif; font-size: 14px; color: rgb(0, 0, 0); background-color: rgb(255, 255, 255);\"><div><ul data-editing-info=\"{&quot;orderedStyleType&quot;:1,&quot;unorderedStyleType&quot;:1}\"><li style=\"list-style-type: disc;\">UL new line 1</li><li style=\"list-style-type: disc;\">UL new line 2</li></ul></div></div>"

            const val malformedSignatureHtml = "<div><input type=\"text\" placeholder=\"This tag is not closed properly</div>"

            val senderAddressWithBlankSignature = UserAddressSample.PrimaryAddress
            val senderAddressWithSimpleSignature = UserAddressSample.ExternalAddress
            val senderAddressWithRichSignature = UserAddressSample.DisabledAddress
            val senderAddressWithMalformedSignature = UserAddressSample.AliasAddress
        }
    }

}
