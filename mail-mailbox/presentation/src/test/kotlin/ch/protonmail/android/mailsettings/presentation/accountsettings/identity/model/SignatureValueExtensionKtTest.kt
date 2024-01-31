package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model

import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import kotlin.test.Test
import kotlin.test.assertEquals

class SignatureValueExtensionKtTest {

    @Test
    fun `should add new lines where there are div tags in the signature`() {
        // Given
        val signatureValue = SignatureValue("<div>Signature first line</div><div>Signature second line</div>")

        // When
        val actual = signatureValue.toPlainText()

        // Then
        val expected = """
            Signature first line
            Signature second line
        """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun `should not add new lines when div tags contain br tags inside`() {
        // Given
        val signatureValue = SignatureValue(
            "<div>Signature first line</div><div><br></div><div>Signature second line after empty line</div>"
        )

        // When
        val actual = signatureValue.toPlainText()

        // Then
        val expected = """
            Signature first line

            Signature second line after empty line
        """.trimIndent()
        assertEquals(expected, actual)
    }
}
