package ch.protonmail.android.mailcontact.data

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcontact.data.local.ContactGroupLocalDataSource
import ch.protonmail.android.mailcontact.data.remote.ContactGroupRemoteDataSource
import ch.protonmail.android.mailcontact.domain.repository.ContactGroupRepository
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class ContactGroupRepositoryImplTest {

    private val userId = UserIdTestData.userId
    private val labelId = LabelIdSample.LabelCoworkers
    private val contactEmailIds = setOf(
        ContactEmailSample.contactEmail1.id
    )

    private val contactGroupLocalDataSourceMock: ContactGroupLocalDataSource = mockk()
    private val contactGroupRemoteDataSourceMock: ContactGroupRemoteDataSource = mockk()

    private val contactGroupRepository by lazy {
        ContactGroupRepositoryImpl(
            contactGroupLocalDataSourceMock,
            contactGroupRemoteDataSourceMock
        )
    }

    @Test
    fun `addContactEmailIdsToContactGroup should call local and remote data source`() = runTest {
        // Given
        coEvery {
            contactGroupLocalDataSourceMock.addContactEmailIdsToContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        } returns Unit
        every {
            contactGroupRemoteDataSourceMock.addContactEmailIdsToContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        } returns Unit

        // When
        val actual = contactGroupRepository.addContactEmailIdsToContactGroup(userId, labelId, contactEmailIds)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `addContactEmailIdsToContactGroup should return RemoteDataSourceError when remote data source fails`() =
        runTest {
            // Given
            coEvery {
                contactGroupLocalDataSourceMock.addContactEmailIdsToContactGroup(
                    userId,
                    labelId,
                    contactEmailIds
                )
            } returns Unit
            every {
                contactGroupRemoteDataSourceMock.addContactEmailIdsToContactGroup(
                    userId,
                    labelId,
                    contactEmailIds
                )
            } throws Exception()

            // When
            val actual = contactGroupRepository.addContactEmailIdsToContactGroup(userId, labelId, contactEmailIds)

            // Then
            assertEquals(ContactGroupRepository.ContactGroupErrors.RemoteDataSourceError.left(), actual)
        }

    @Test
    fun `removeContactEmailIdsFromContactGroup should call local and remote data source`() = runTest {
        // Given
        coEvery {
            contactGroupLocalDataSourceMock.removeContactEmailIdsFromContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        } returns Unit
        every {
            contactGroupRemoteDataSourceMock.removeContactEmailIdsFromContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        } returns Unit

        // When
        val actual = contactGroupRepository.removeContactEmailIdsFromContactGroup(userId, labelId, contactEmailIds)

        // Then
        assertEquals(Unit.right(), actual)
    }


    @Test
    fun `removeContactEmailIdsFromContactGroup should return RemoteDataSourceError when remote data source fails`() =
        runTest {
            // Given
            coEvery {
                contactGroupLocalDataSourceMock.removeContactEmailIdsFromContactGroup(
                    userId,
                    labelId,
                    contactEmailIds
                )
            } returns Unit
            every {
                contactGroupRemoteDataSourceMock.removeContactEmailIdsFromContactGroup(
                    userId,
                    labelId,
                    contactEmailIds
                )
            } throws Exception()

            // When
            val actual = contactGroupRepository.removeContactEmailIdsFromContactGroup(userId, labelId, contactEmailIds)

            // Then
            assertEquals(ContactGroupRepository.ContactGroupErrors.RemoteDataSourceError.left(), actual)
        }

}
