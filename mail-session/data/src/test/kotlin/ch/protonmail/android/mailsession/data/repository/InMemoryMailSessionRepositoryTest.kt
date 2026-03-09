package ch.protonmail.android.mailsession.data.repository

import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.MailSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class InMemoryMailSessionRepositoryTest {

    private val mailSessionRepository = InMemoryMailSessionRepository()

    @Test
    fun `returns existing mail session when present`() = runTest {
        // Given
        val expected = mockk<MailSession>()
        mailSessionRepository.setMailSession(expected)

        // When
        val actual = mailSessionRepository.getMailSession()

        // Then
        assertIs<MailSessionWrapper>(actual)
        assertEquals(expected, actual.getRustMailSession())
    }

    @Test
    fun `throws when mail session is not present`() = runTest {
        // Then
        assertFailsWith<UninitializedPropertyAccessException> {
            // When
            mailSessionRepository.getMailSession()
        }
    }

}
