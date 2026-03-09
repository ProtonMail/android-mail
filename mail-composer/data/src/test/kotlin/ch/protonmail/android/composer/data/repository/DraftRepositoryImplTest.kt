package ch.protonmail.android.composer.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.local.LocalDraftWithSyncStatus
import ch.protonmail.android.composer.data.local.RustDraftDataSource
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DiscardDraftError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFieldsWithSyncStatus
import ch.protonmail.android.mailcomposer.domain.model.DraftSenderValidationError
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.ScheduleSendOptions
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.composer.DraftFieldsTestData
import ch.protonmail.android.testdata.composer.LocalDraftTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.DraftAddressValidationError
import uniffi.mail_uniffi.DraftAddressValidationResult
import uniffi.mail_uniffi.DraftScheduleSendOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class DraftRepositoryImplTest {

    private val draftDataSource = mockk<RustDraftDataSource>()
    private val dispatcher = StandardTestDispatcher()

    private val draftRepository = DraftRepositoryImpl(draftDataSource, dispatcher)

    @Test
    fun `returns success when open draft succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val localFieldsWithSyncStatus = LocalDraftWithSyncStatus.Local(LocalDraftTestData.BasicLocalDraft)
        coEvery { draftDataSource.open(userId, messageId) } returns localFieldsWithSyncStatus.right()

        // When
        val actual = draftRepository.openDraft(userId, messageId)

        // Then
        val expected = DraftFieldsWithSyncStatus.Local(DraftFieldsTestData.BasicDraftFields)
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns error when open draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val expected = OpenDraftError.MissingMessageBody
        coEvery { draftDataSource.open(userId, messageId) } returns expected.left()

        // When
        val actual = draftRepository.openDraft(userId, messageId)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when create draft succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val localFields = LocalDraftTestData.BasicLocalDraft
        val action = DraftAction.Compose
        coEvery { draftDataSource.create(userId, action) } returns localFields.right()

        // When
        val actual = draftRepository.createDraft(userId, action)

        // Then
        val expected = DraftFieldsTestData.BasicDraftFields
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns error when create draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expected = OpenDraftError.Other(DataError.Local.UnsupportedOperation)
        val action = DraftAction.Compose
        coEvery { draftDataSource.create(userId, action) } returns expected.left()

        // When
        val actual = draftRepository.createDraft(userId, action)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when discard draft succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        coEvery { draftDataSource.discard(userId, messageId) } returns Unit.right()

        // When
        val actual = draftRepository.discardDraft(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `returns error when discard draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val expectedError = DiscardDraftError.DeleteDraftFailed
        coEvery { draftDataSource.discard(userId, messageId) } returns expectedError.left()

        // When
        val actual = draftRepository.discardDraft(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `returns success when save draft subject succeeds`() = runTest {
        // Given
        val subject = Subject("test subject")
        coEvery { draftDataSource.saveSubject(subject) } returns Unit.right()

        // When
        val actual = draftRepository.saveSubject(subject)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `returns error when save draft subject fails`() = runTest {
        // Given
        val expected = SaveDraftError.SaveFailed
        val subject = Subject("test subject")
        coEvery { draftDataSource.saveSubject(subject) } returns expected.left()

        // When
        val actual = draftRepository.saveSubject(subject)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when save draft body succeeds`() = runTest {
        // Given
        val body = DraftBody("test body")
        coEvery { draftDataSource.saveBody(body) } returns Unit.right()

        // When
        val actual = draftRepository.saveBody(body)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `returns error when save draft body fails`() = runTest {
        // Given
        val expected = SaveDraftError.SaveFailed
        val body = DraftBody("test body")
        coEvery { draftDataSource.saveBody(body) } returns expected.left()

        // When
        val actual = draftRepository.saveBody(body)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when send draft succeeds`() = runTest {
        // Given
        coEvery { draftDataSource.send() } returns Unit.right()

        // When
        val actual = draftRepository.send()

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `returns error when send draft fails`() = runTest {
        // Given
        val expected = SendDraftError.AlreadySent
        coEvery { draftDataSource.send() } returns expected.left()

        // When
        val actual = draftRepository.send()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when get schedule send options succeeds`() = runTest(dispatcher) {
        // Given
        val tomorrowTime = 123uL
        val mondayTime = 456uL
        val localOptions = DraftScheduleSendOptions(tomorrowTime, mondayTime, false)
        val expected = ScheduleSendOptions(
            Instant.fromEpochSeconds(tomorrowTime.toLong()),
            Instant.fromEpochSeconds(mondayTime.toLong()),
            isCustomTimeOptionAvailable = false
        )
        coEvery { draftDataSource.getScheduleSendOptions() } returns localOptions.right()

        // When
        val actual = draftRepository.getScheduleSendOptions()

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns error when get schedule send options fails`() = runTest(dispatcher) {
        // Given
        val expected = DataError.Local.CryptoError
        coEvery { draftDataSource.getScheduleSendOptions() } returns expected.left()

        // When
        val actual = draftRepository.getScheduleSendOptions()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns success when schedule send succeeds`() = runTest {
        // Given
        val timestamp = 123L
        val time = Instant.fromEpochSeconds(timestamp)
        coEvery { draftDataSource.scheduleSend(timestamp) } returns Unit.right()

        // When
        val actual = draftRepository.scheduleSend(time)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `returns error when schedule send fails`() = runTest {
        // Given
        val timestamp = 123L
        val time = Instant.fromEpochSeconds(timestamp)
        val expected = SendDraftError.ScheduleSendError
        coEvery { draftDataSource.scheduleSend(timestamp) } returns expected.left()

        // When
        val actual = draftRepository.scheduleSend(time)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `returns image data when load image was successful`() = runTest {
        // Given
        val url = "url"
        every { draftDataSource.loadImage(url) } returns LocalAttachmentData(byteArrayOf(), "").right()

        // When
        val actual = draftRepository.loadImage(url)

        // Then
        assertEquals(MessageBodyImage(byteArrayOf(), "").right(), actual)
    }

    @Test
    fun `returns error when load image failed`() = runTest {
        // Given
        val url = "url"
        every { draftDataSource.loadImage(url) } returns AttachmentDataError.Other(DataError.Local.CryptoError).left()

        // When
        val actual = draftRepository.loadImage(url)

        // Then
        assertEquals(AttachmentDataError.Other(DataError.Local.CryptoError).left(), actual)
    }

    @Test
    fun `returns change sender result when validate change sender returns it`() = runTest {
        // Given
        val email = "address@pm.me"
        coEvery { draftDataSource.validateDraftSenderAddress() } returns DraftAddressValidationResult(
            email,
            DraftAddressValidationError.DISABLED
        )

        // When
        val actual = draftRepository.getDraftSenderValidationError()

        // Then
        assertEquals(DraftSenderValidationError.AddressDisabled(email), actual)
    }

    @Test
    fun `returns null when validate change sender returns nothing`() = runTest {
        coEvery { draftDataSource.validateDraftSenderAddress() } returns null

        // When
        val actual = draftRepository.getDraftSenderValidationError()

        // Then
        assertNull(actual)
    }

}
