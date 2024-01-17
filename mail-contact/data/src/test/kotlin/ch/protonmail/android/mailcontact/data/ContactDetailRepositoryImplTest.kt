package ch.protonmail.android.mailcontact.data

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontact.data.local.ContactDetailLocalDataSource
import ch.protonmail.android.mailcontact.data.remote.ContactDetailRemoteDataSource
import ch.protonmail.android.mailcontact.domain.repository.ContactDetailRepository.ContactDetailErrors.ContactDetailLocalDataSourceError
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactDetailRepositoryImplTest {

    private val userId = UserId("userId")
    private val contactId = ContactId("contactId")

    private val contactDetailLocalDataSource: ContactDetailLocalDataSource = mockk()
    private val contactDetailRemoteDataSource: ContactDetailRemoteDataSource = mockk()

    private val contactDetailRepository by lazy {
        ContactDetailRepositoryImpl(
            contactDetailLocalDataSource,
            contactDetailRemoteDataSource
        )
    }

    @Test
    fun `delete contact should call local and remote data source`() = runTest {
        // When
        coEvery { contactDetailLocalDataSource.deleteContact(contactId) } returns Unit
        every { contactDetailRemoteDataSource.deleteContact(userId, contactId) } returns Unit
        val actual = contactDetailRepository.deleteContact(userId, contactId)

        // Then
        coVerify { contactDetailLocalDataSource.deleteContact(contactId) }
        verify { contactDetailRemoteDataSource.deleteContact(userId, contactId) }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `delete contact should return error when local data source fails`() = runTest {
        // When
        coEvery { contactDetailLocalDataSource.deleteContact(contactId) } throws Exception()
        val actual = contactDetailRepository.deleteContact(userId, contactId)

        // Then
        coVerify { contactDetailLocalDataSource.deleteContact(contactId) }
        verify { contactDetailRemoteDataSource wasNot Called }
        assertEquals(ContactDetailLocalDataSourceError.left(), actual)
    }

}
