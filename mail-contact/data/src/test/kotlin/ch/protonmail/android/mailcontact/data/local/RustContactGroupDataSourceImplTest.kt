package ch.protonmail.android.mailcontact.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalContactGroupId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcontact.data.mapper.ContactGroupItemMapper
import ch.protonmail.android.mailcontact.data.usecase.RustGetContactGroupDetails
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.ContactGroupItem
import kotlin.test.Test
import kotlin.test.assertEquals

class RustContactGroupDataSourceImplTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val rustGetContactGroupDetails = mockk<RustGetContactGroupDetails>()

    private val rustContactGroupDataSource = RustContactGroupDataSourceImpl(
        userSessionRepository = userSessionRepository,
        rustGetContactGroupDetails = rustGetContactGroupDetails,
        contactGroupItemMapper = ContactGroupItemMapper()
    )

    @Test
    fun `should get contact group details when session is available`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactGroupId = LocalContactGroupId(100u)
        val session = mockk<MailUserSessionWrapper>()
        val contactGroupItem = ContactGroupItem(
            id = contactGroupId,
            name = "name",
            avatarColor = "color",
            contactEmails = emptyList()
        )
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustGetContactGroupDetails(session, contactGroupId) } returns contactGroupItem.right()

        // When
        val result = rustContactGroupDataSource.getContactGroupDetails(userId, contactGroupId)

        // Then
        assertTrue(result.isRight())
        coVerify { rustGetContactGroupDetails(session, contactGroupId) }
    }

    @Test
    fun `getting contact group details should return error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactGroupId = LocalContactGroupId(100u)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = rustContactGroupDataSource.getContactGroupDetails(userId, contactGroupId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `getting contact group details should return error when use case fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactGroupId = LocalContactGroupId(100u)
        val session = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustGetContactGroupDetails(session, contactGroupId) } returns DataError.Local.CryptoError.left()

        // When
        val result = rustContactGroupDataSource.getContactGroupDetails(userId, contactGroupId)

        // Then
        assertEquals(DataError.Local.CryptoError.left(), result)
    }
}
