package ch.protonmail.android.composer.data.local

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.local.RustDraftDataSourceImpl.Companion.InitialDelayForSendingStatusWorker
import ch.protonmail.android.composer.data.mapper.toSingleRecipientEntry
import ch.protonmail.android.composer.data.usecase.CreateRustDraft
import ch.protonmail.android.composer.data.usecase.DiscardRustDraft
import ch.protonmail.android.composer.data.usecase.OpenRustDraft
import ch.protonmail.android.composer.data.usecase.RustDraftUndoSend
import ch.protonmail.android.composer.data.worker.SendingStatusWorker
import ch.protonmail.android.composer.data.wrapper.ComposerRecipientListWrapper
import ch.protonmail.android.composer.data.wrapper.DraftWrapper
import ch.protonmail.android.composer.data.wrapper.DraftWrapperWithSyncStatus
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentDataErrorOther
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeType
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.DiscardDraftError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftHead
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.MessagePasswordError
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.testdata.composer.DraftRecipientTestData
import ch.protonmail.android.testdata.composer.LocalComposerRecipientTestData
import ch.protonmail.android.testdata.composer.LocalDraftTestData
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.AttachmentDataResult
import uniffi.mail_uniffi.ComposerRecipientValidationCallback
import uniffi.mail_uniffi.DraftChangeSenderAddressResult
import uniffi.mail_uniffi.DraftCreateMode
import uniffi.mail_uniffi.DraftExpirationError
import uniffi.mail_uniffi.DraftExpirationErrorReason
import uniffi.mail_uniffi.DraftExpirationTime
import uniffi.mail_uniffi.DraftExpirationTimeResult
import uniffi.mail_uniffi.DraftIsPasswordProtectedResult
import uniffi.mail_uniffi.DraftListSenderAddressesResult
import uniffi.mail_uniffi.DraftMessageIdResult
import uniffi.mail_uniffi.DraftPasswordError
import uniffi.mail_uniffi.DraftPasswordErrorReason
import uniffi.mail_uniffi.DraftSaveError
import uniffi.mail_uniffi.DraftSaveErrorReason
import uniffi.mail_uniffi.DraftScheduleSendOptions
import uniffi.mail_uniffi.DraftScheduleSendOptionsResult
import uniffi.mail_uniffi.DraftSendError
import uniffi.mail_uniffi.DraftSendErrorReason
import uniffi.mail_uniffi.DraftSenderAddressList
import uniffi.mail_uniffi.DraftSyncStatus
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.UnexpectedError
import uniffi.mail_uniffi.VoidDraftExpirationResult
import uniffi.mail_uniffi.VoidDraftPasswordResult
import uniffi.mail_uniffi.VoidDraftSaveResult
import uniffi.mail_uniffi.VoidDraftSendResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RustDraftDataSourceImplTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val createRustDraft = mockk<CreateRustDraft>()
    private val openRustDraft = mockk<OpenRustDraft>()
    private val discardRustDraft = mockk<DiscardRustDraft>()

    private val mockUserSession = mockk<MailUserSessionWrapper>()
    private val enqueuer = mockk<Enqueuer>()
    private val rustDraftUndoSend = mockk<RustDraftUndoSend>()
    private val draftCache = mockk<DraftCache>()
    private val composerSignals = mockk<ComposerSignals>(relaxUnitFun = true)

    private val dataSource = RustDraftDataSourceImpl(
        userSessionRepository,
        createRustDraft,
        openRustDraft,
        discardRustDraft,
        rustDraftUndoSend,
        enqueuer,
        draftCache,
        composerSignals
    )

    @Test
    fun `open draft returns error when there is no user session`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val expected = OpenDraftError.Other(DataError.Local.NoUserSession)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val actual = dataSource.open(userId, messageId)

        // Then
        assertEquals(actual, expected.left())
    }

    @Test
    fun `open draft returns Local Draft with sync status when opened successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val localMessageId = MessageIdSample.RustJobApplication.toLocalMessageId()
        val expectedSyncStatus = DraftSyncStatus.SYNCED
        val expected = LocalDraftWithSyncStatus.Remote(LocalDraftTestData.JobApplicationDraftWithRecipients)
        val toRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val ccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = expected.localDraft.subject,
            sender = expected.localDraft.sender,
            head = expected.localDraft.bodyFields.head.value,
            body = expected.localDraft.bodyFields.body.value,
            toRecipientsWrapper = toRecipientsWrapperMock,
            ccRecipientsWrapper = ccRecipientsWrapperMock,
            bccRecipientsWrapper = bccRecipientsWrapperMock,
            messageId = localMessageId
        )
        val expectedWrapperWithSyncStatus = DraftWrapperWithSyncStatus(expectedDraftWrapper, expectedSyncStatus)
        coEvery { toRecipientsWrapperMock.recipients() } returns listOf(LocalComposerRecipientTestData.Alice)
        coEvery { ccRecipientsWrapperMock.recipients() } returns listOf(LocalComposerRecipientTestData.Bob)
        coEvery { bccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { toRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { ccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { bccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { openRustDraft(mockUserSession, localMessageId) } returns expectedWrapperWithSyncStatus.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        val actual = dataSource.open(userId, messageId)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `draft instance is cached to draft cache when opened successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val localMessageId = MessageIdSample.RustJobApplication.toLocalMessageId()
        val expected = LocalDraftTestData.JobApplicationDraft
        val recipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = expected.subject,
            sender = expected.sender,
            head = expected.bodyFields.head.value,
            body = expected.bodyFields.body.value,
            toRecipientsWrapper = recipientsWrapperMock,
            ccRecipientsWrapper = recipientsWrapperMock,
            bccRecipientsWrapper = recipientsWrapperMock
        )
        val expectedWrapperWithSyncStatus = DraftWrapperWithSyncStatus(expectedDraftWrapper, DraftSyncStatus.SYNCED)
        coEvery { recipientsWrapperMock.recipients() } returns emptyList()
        coEvery { recipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { openRustDraft(mockUserSession, localMessageId) } returns expectedWrapperWithSyncStatus.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.open(userId, messageId)

        // Then
        verify { draftCache.add(expectedDraftWrapper) }
    }

    @Test
    fun `recipient validation callbacks are registered when draft is opened successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val localMessageId = MessageIdSample.RustJobApplication.toLocalMessageId()
        val expected = LocalDraftTestData.JobApplicationDraft
        val toRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val ccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = expected.subject,
            sender = expected.sender,
            head = expected.bodyFields.head.value,
            body = expected.bodyFields.body.value,
            toRecipientsWrapper = toRecipientsWrapperMock,
            ccRecipientsWrapper = ccRecipientsWrapperMock,
            bccRecipientsWrapper = bccRecipientsWrapperMock
        )
        val expectedWrapperWithSyncStatus = DraftWrapperWithSyncStatus(expectedDraftWrapper, DraftSyncStatus.SYNCED)
        coEvery { toRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { ccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { bccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { toRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { ccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { bccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { openRustDraft(mockUserSession, localMessageId) } returns expectedWrapperWithSyncStatus.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.open(userId, messageId)

        // Then
        verify { toRecipientsWrapperMock.registerCallback(any()) }
        verify { ccRecipientsWrapperMock.registerCallback(any()) }
        verify { bccRecipientsWrapperMock.registerCallback(any()) }
    }

    @Test
    fun `create draft returns error when there is no user session`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val action = DraftAction.Reply(messageId)
        val expected = OpenDraftError.Other(DataError.Local.NoUserSession)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val actual = dataSource.create(userId, action)

        // Then
        assertEquals(actual, expected.left())
    }

    @Test
    fun `create draft returns error when draft create mode is not supported`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val action = DraftAction.ComposeToAddresses(emptyList())
        val expected = OpenDraftError.Other(DataError.Local.UnsupportedOperation)
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession

        // When
        val actual = dataSource.create(userId, action)

        // Then
        assertEquals(actual, expected.left())
    }

    @Test
    fun `create draft returns Local Draft data when created successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val action = DraftAction.ReplyAll(messageId)
        val localDraftCreateMode = DraftCreateMode.ReplyAll(messageId.toLocalMessageId())
        val expected = LocalDraftTestData.JobApplicationDraft
        val subject = expected.subject
        val sender = expected.sender
        val recipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = subject,
            sender = sender,
            head = expected.bodyFields.head.value,
            body = expected.bodyFields.body.value,
            toRecipientsWrapper = recipientsWrapperMock,
            ccRecipientsWrapper = recipientsWrapperMock,
            bccRecipientsWrapper = recipientsWrapperMock,
            messageId = messageId.toLocalMessageId()
        )
        coEvery { recipientsWrapperMock.recipients() } returns emptyList()
        coEvery { recipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { createRustDraft(mockUserSession, localDraftCreateMode) } returns expectedDraftWrapper.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        val actual = dataSource.create(userId, action)

        // Then
        assertEquals(actual, expected.right())
    }

    @Test
    fun `draft instance is cached to draft cache when created successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val action = DraftAction.Forward(messageId)
        val localDraftCreateMode = DraftCreateMode.Forward(messageId.toLocalMessageId())
        val expected = LocalDraftTestData.JobApplicationDraft
        val recipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = expected.subject,
            sender = expected.sender,
            head = expected.bodyFields.head.value,
            body = expected.bodyFields.body.value,
            toRecipientsWrapper = recipientsWrapperMock,
            ccRecipientsWrapper = recipientsWrapperMock,
            bccRecipientsWrapper = recipientsWrapperMock
        )
        coEvery { recipientsWrapperMock.recipients() } returns emptyList()
        coEvery { recipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { createRustDraft(mockUserSession, localDraftCreateMode) } returns expectedDraftWrapper.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.create(userId, action)

        // Then
        verify { draftCache.add(expectedDraftWrapper) }
    }

    @Test
    fun `recipient validation callbacks are registered when draft is created successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val action = DraftAction.Forward(messageId)
        val localDraftCreateMode = DraftCreateMode.Forward(messageId.toLocalMessageId())
        val expected = LocalDraftTestData.JobApplicationDraft
        val toRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val ccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            subject = expected.subject,
            sender = expected.sender,
            head = expected.bodyFields.head.value,
            body = expected.bodyFields.body.value,
            toRecipientsWrapper = toRecipientsWrapperMock,
            ccRecipientsWrapper = ccRecipientsWrapperMock,
            bccRecipientsWrapper = bccRecipientsWrapperMock
        )
        coEvery { toRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { ccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { bccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { toRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { ccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { bccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { createRustDraft(mockUserSession, localDraftCreateMode) } returns expectedDraftWrapper.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.create(userId, action)

        // Then
        verify { toRecipientsWrapperMock.registerCallback(any()) }
        verify { ccRecipientsWrapperMock.registerCallback(any()) }
        verify { bccRecipientsWrapperMock.registerCallback(any()) }
    }

    @Test
    fun `discard draft returns error when there is no user session`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.PlainTextMessage
        val expected = DiscardDraftError.Other(DataError.Local.NoUserSession)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val actual = dataSource.discard(userId, messageId)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `discard draft returns Unit when discarding successfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { discardRustDraft(mockUserSession, messageId.toLocalMessageId()) } returns Unit.right()

        // When
        val actual = dataSource.discard(userId, messageId)

        // Then
        assertEquals(actual, Unit.right())
    }

    @Test
    fun `discard draft returns DataError when discarding unsuccessfully`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val discardDraftError = DiscardDraftError.DeleteDraftFailed
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery {
            discardRustDraft(mockUserSession, messageId.toLocalMessageId())
        } returns discardDraftError.left()

        // When
        val actual = dataSource.discard(userId, messageId)

        // Then
        assertEquals(discardDraftError.left(), actual)
    }

    @Test
    fun `save subject calls rust draft wrapper to set subject and save`() = runTest {
        // Given
        val messageId = MessageIdSample.RustJobApplication
        val draft = LocalDraftTestData.JobApplicationDraft
        val subject = Subject("saving a draft...")
        val expectedDraftWrapper = expectDraftWrapperReturns(
            draft.subject,
            draft.sender,
            draft.bodyFields.body.value,
            draft.bodyFields.head.value,
            messageId = messageId.toLocalMessageId()
        )
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setSubject(subject.value) } returns VoidDraftSaveResult.Ok
        coEvery { expectedDraftWrapper.save() } returns VoidDraftSaveResult.Ok

        // When
        val actual = dataSource.saveSubject(subject)

        // Then
        assertEquals(actual, Unit.right())
    }

    @Test
    fun `save subject returns error when rust draft wrapper call fails`() = runTest {
        // Given
        val draft = LocalDraftTestData.JobApplicationDraft
        val expectedDraftWrapper = expectDraftWrapperReturns(
            draft.subject,
            draft.sender,
            draft.bodyFields.head.value,
            draft.bodyFields.body.value
        )
        val subject = Subject("saving a draft...")
        every { draftCache.get() } returns expectedDraftWrapper
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setSubject(subject.value) } returns VoidDraftSaveResult.Error(
            DraftSaveError.Reason(DraftSaveErrorReason.MessageIsNotADraft)
        )
        val expected = SaveDraftError.MessageIsNotADraft

        // When
        val actual = dataSource.saveSubject(subject)

        // Then
        assertEquals(actual, expected.left())
    }

    @Test
    fun `save body calls rust draft wrapper to set body and save`() = runTest {
        // Given
        val messageId = MessageIdSample.RustJobApplication
        val draft = LocalDraftTestData.JobApplicationDraft
        val body = DraftBody("saving a draft's body...")
        val expectedDraftWrapper = expectDraftWrapperReturns(
            draft.subject,
            draft.sender,
            draft.bodyFields.body.value,
            draft.bodyFields.head.value,
            messageId = messageId.toLocalMessageId()
        )
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setBody(body.value) } returns VoidDraftSaveResult.Ok
        coEvery { expectedDraftWrapper.save() } returns VoidDraftSaveResult.Ok

        // When
        val actual = dataSource.saveBody(body)

        // Then
        assertEquals(actual, Unit.right())
    }

    @Test
    fun `save body returns error when rust draft wrapper call fails`() = runTest {
        // Given
        val draft = LocalDraftTestData.JobApplicationDraft
        val body = DraftBody("saving a draft's body...")
        val expectedDraftWrapper = expectDraftWrapperReturns(
            draft.subject,
            draft.sender,
            draft.bodyFields.head.value,
            draft.bodyFields.body.value
        )
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setBody(body.value) } returns VoidDraftSaveResult.Error(
            DraftSaveError.Reason(DraftSaveErrorReason.MessageIsNotADraft)
        )
        val expected = SaveDraftError.MessageIsNotADraft

        // When
        val actual = dataSource.saveBody(body)

        // Then
        assertEquals(actual, expected.left())
    }

    @Test
    fun `returns success when send draft succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val expectedDraftWrapper = expectDraftWrapperReturns(
            messageId = messageId
        )
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery {
            enqueuer.enqueueUniqueWork<SendingStatusWorker>(
                userId = userId,
                workerId = SendingStatusWorker.id(userId, messageId.toMessageId()),
                params = SendingStatusWorker.params(userId, messageId.toMessageId()),
                backoffCriteria = Enqueuer.BackoffCriteria.DefaultLinear,
                initialDelay = InitialDelayForSendingStatusWorker
            )
        } returns Unit

        // When
        val actual = dataSource.send()

        // Then
        assertEquals(Unit.right(), actual)
        coVerify {
            enqueuer.enqueueUniqueWork<SendingStatusWorker>(
                userId = userId,
                workerId = SendingStatusWorker.id(userId, messageId.toMessageId()),
                params = SendingStatusWorker.params(userId, messageId.toMessageId()),
                backoffCriteria = Enqueuer.BackoffCriteria.DefaultLinear,
                initialDelay = InitialDelayForSendingStatusWorker
            )
        }
    }

    @Test
    fun `returns error when send draft fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val expectedError = SendDraftError.InvalidRecipient

        val expectedDraftWrapper = expectDraftWrapperReturns(
            messageId = messageId,
            sendResult = VoidDraftSendResult.Error(
                DraftSendError.Reason(DraftSendErrorReason.RecipientEmailInvalid("test!"))
            )
        )
        every { draftCache.get() } returns expectedDraftWrapper

        coEvery { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)

        // When
        val actual = dataSource.send()

        // Then
        assertEquals(expectedError.left(), actual)
        coVerify(exactly = 0) {
            enqueuer.enqueueUniqueWork<SendingStatusWorker>(
                any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `undoSend returns error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val expected = UndoSendError.Other(DataError.Local.NoUserSession)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val actual = dataSource.undoSend(userId, messageId)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `undoSend returns error when rustDraftUndoSend fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageId("110")
        val expectedError = UndoSendError.UndoSendFailed
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { rustDraftUndoSend(mockUserSession, messageId.toLocalMessageId()) } returns expectedError.left()

        // When
        val actual = dataSource.undoSend(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `undoSend cancels sending status worker when successful`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageId("110")
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { rustDraftUndoSend(mockUserSession, messageId.toLocalMessageId()) } returns Unit.right()
        coEvery { enqueuer.cancelWork(SendingStatusWorker.id(userId, messageId)) } just Runs

        // When
        val actual = dataSource.undoSend(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { enqueuer.cancelWork(SendingStatusWorker.id(userId, messageId)) }
    }

    @Test
    fun `update To recipients adds and removes recipients as needed`() = runTest {
        // Given
        val bob = DraftRecipientTestData.Bob // will be removed
        val john = DraftRecipientTestData.John // will be added
        val alice = DraftRecipientTestData.Alice // unchanged
        val updatedRecipients = listOf(john, alice)
        val currentRecipients = listOf(
            LocalComposerRecipientTestData.Alice, LocalComposerRecipientTestData.Bob
        )

        val toRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            toRecipientsWrapper = toRecipientsWrapperMock
        )
        coEvery { toRecipientsWrapperMock.recipients() } returns currentRecipients
        coEvery { toRecipientsWrapperMock.addSingleRecipient(any()) } returns Unit.right()
        coEvery { toRecipientsWrapperMock.removeSingleRecipient(any()) } returns Unit.right()
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.updateToRecipients(updatedRecipients)

        // Then
        verifyOrder {
            toRecipientsWrapperMock.addSingleRecipient(john.toSingleRecipientEntry())
            toRecipientsWrapperMock.removeSingleRecipient(bob.toSingleRecipientEntry())
        }
    }

    @Test
    fun `update Cc recipients adds and removes recipients as needed`() = runTest {
        // Given
        val bob = DraftRecipientTestData.Bob // will be removed
        val john = DraftRecipientTestData.John // will be added
        val alice = DraftRecipientTestData.Alice // unchanged
        val updatedRecipients = listOf(john, alice)
        val currentRecipients = listOf(
            LocalComposerRecipientTestData.Alice, LocalComposerRecipientTestData.Bob
        )

        val ccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            ccRecipientsWrapper = ccRecipientsWrapperMock
        )
        coEvery { ccRecipientsWrapperMock.recipients() } returns currentRecipients
        coEvery { ccRecipientsWrapperMock.addSingleRecipient(any()) } returns Unit.right()
        coEvery { ccRecipientsWrapperMock.removeSingleRecipient(any()) } returns Unit.right()
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.updateCcRecipients(updatedRecipients)

        // Then
        verifyOrder {
            ccRecipientsWrapperMock.addSingleRecipient(john.toSingleRecipientEntry())
            ccRecipientsWrapperMock.removeSingleRecipient(bob.toSingleRecipientEntry())
        }
    }

    @Test
    fun `update Bcc recipients adds and removes recipients as needed`() = runTest {
        // Given
        val bob = DraftRecipientTestData.Bob // will be removed
        val john = DraftRecipientTestData.John // will be added
        val alice = DraftRecipientTestData.Alice // unchanged
        val updatedRecipients = listOf(john, alice)
        val currentRecipients = listOf(
            LocalComposerRecipientTestData.Alice, LocalComposerRecipientTestData.Bob
        )

        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            bccRecipientsWrapper = bccRecipientsWrapperMock
        )
        coEvery { bccRecipientsWrapperMock.recipients() } returns currentRecipients
        coEvery { bccRecipientsWrapperMock.addSingleRecipient(any()) } returns Unit.right()
        coEvery { bccRecipientsWrapperMock.removeSingleRecipient(any()) } returns Unit.right()
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.updateBccRecipients(updatedRecipients)

        // Then
        verifyOrder {
            bccRecipientsWrapperMock.recipients()
            bccRecipientsWrapperMock.addSingleRecipient(john.toSingleRecipientEntry())
            bccRecipientsWrapperMock.removeSingleRecipient(bob.toSingleRecipientEntry())
        }
    }

    @Test
    fun `update recipients compares recipients by address`() = runTest {
        // Given
        // Due to some contact-suggestions related features not done yet, it can happen that the contact
        // we pass from UI has a different name than the one that's saved in rust (ie. in reply case where
        // rust save the contact before UI passes it); To keep this working, we compare contacts by address.
        val johnNameless = DraftRecipientTestData.John.copy(name = "") // won't be (re) added
        val updatedRecipients = listOf(johnNameless)
        val currentRecipients = listOf(
            LocalComposerRecipientTestData.John, LocalComposerRecipientTestData.Bob
        )

        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>()
        val expectedDraftWrapper = expectDraftWrapperReturns(
            bccRecipientsWrapper = bccRecipientsWrapperMock
        )
        coEvery { bccRecipientsWrapperMock.recipients() } returns currentRecipients
        coEvery { bccRecipientsWrapperMock.addSingleRecipient(any()) } returns Unit.right()
        coEvery { bccRecipientsWrapperMock.removeSingleRecipient(any()) } returns Unit.right()
        every { draftCache.get() } returns expectedDraftWrapper

        // When
        dataSource.updateBccRecipients(updatedRecipients)

        // Then
        coVerify(exactly = 0) {
            bccRecipientsWrapperMock.addSingleRecipient(johnNameless.toSingleRecipientEntry())
        }
    }

    @Test
    fun `load image returns the image info when successful`() = runTest {
        // Given
        val localEmbeddedImage = LocalAttachmentData(
            "data".toByteArray(),
            "image/jpg"
        )
        val cid = "image-content-id"
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.loadImage(cid) } returns AttachmentDataResult.Ok(
            localEmbeddedImage
        )

        // When
        val actual = dataSource.loadImage(url = cid)

        // Then
        assertEquals(localEmbeddedImage.right(), actual)
    }

    @Test
    fun `load image returns DataError when unsuccessful`() = runTest {
        // Given
        val expected = AttachmentDataError.Other(DataError.Remote.NoNetwork)
        val cid = "image-content-id"
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.loadImage(cid) } returns AttachmentDataResult.Error(
            LocalAttachmentDataErrorOther(ProtonError.Network)
        )

        // When
        val actual = dataSource.loadImage(url = cid)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `get schedule send options returns schedule options when successful`() = runTest {
        // Given
        val scheduleSendOptions = DraftScheduleSendOptions(
            tomorrowTime = 123uL,
            mondayTime = 456uL,
            isCustomOptionAvailable = false
        )
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.scheduleSendOptions() } returns DraftScheduleSendOptionsResult.Ok(
            scheduleSendOptions
        )

        // When
        val actual = dataSource.getScheduleSendOptions()

        // Then
        assertEquals(scheduleSendOptions.right(), actual)
    }

    @Test
    fun `get schedule send options returns DataError when unsuccessful`() = runTest {
        // Given
        val expected = DataError.Remote.NoNetwork
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.scheduleSendOptions() } returns DraftScheduleSendOptionsResult.Error(
            ProtonError.Network
        )

        // When
        val actual = dataSource.getScheduleSendOptions()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `schedule send returns success when successful`() = runTest {
        // Given
        val timestamp = 1234L
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.scheduleSend(timestamp.toULong()) } returns VoidDraftSendResult.Ok

        // When
        val actual = dataSource.scheduleSend(timestamp)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `schedule send returns DataError when unsuccessful`() = runTest {
        // Given
        val timestamp = 1234L
        val expected = SendDraftError.InvalidRecipient
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.scheduleSend(timestamp.toULong()) } returns VoidDraftSendResult.Error(
            DraftSendError.Reason(DraftSendErrorReason.NoRecipients)
        )

        // When
        val actual = dataSource.scheduleSend(timestamp)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `list sender addresses returns addresses when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        val addresses = listOf("test1@pm.me", "test2@pm.me")
        val expected = LocalSenderAddresses(addresses, addresses[0])
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.listSenderAddresses() } returns DraftListSenderAddressesResult.Ok(
            DraftSenderAddressList(addresses, addresses[0])
        )

        // When
        val actual = dataSource.listSenderAddresses()

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `list sender addresses returns error when unsuccessful`() = runTest {
        // Given
        val expected = DataError.Local.Unknown
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.listSenderAddresses() } returns DraftListSenderAddressesResult.Error(
            ProtonError.Unexpected(UnexpectedError.DRAFT)
        )

        // When
        val actual = dataSource.listSenderAddresses()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `change sender address returns success when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        val address = "test1@pm.me"
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.changeSender(address) } returns DraftChangeSenderAddressResult.Ok

        // When
        val actual = dataSource.changeSender(SenderEmail(address))

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `change sender address returns specific error when unsuccessful`() = runTest {
        // Given
        val expected = DataError.Local.Unknown
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.listSenderAddresses() } returns DraftListSenderAddressesResult.Error(
            ProtonError.Unexpected(UnexpectedError.DRAFT)
        )

        // When
        val actual = dataSource.listSenderAddresses()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `is password protected returns value when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.isPasswordProtected() } returns DraftIsPasswordProtectedResult.Ok(true)

        // When
        val actual = dataSource.isPasswordProtected()

        // Then
        assertEquals(true.right(), actual)
    }

    @Test
    fun `is password protected returns error when unsuccessful`() = runTest {
        // Given
        val expected = DataError.Local.Unknown
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.isPasswordProtected() } returns DraftIsPasswordProtectedResult.Error(
            ProtonError.Unexpected(UnexpectedError.DRAFT)
        )

        // When
        val actual = dataSource.isPasswordProtected()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `set external encryption password returns Unit when successful`() = runTest {
        // Given
        val password = "password"
        val hint = "hint"
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setPassword(password, hint) } returns VoidDraftPasswordResult.Ok

        // When
        val actual = dataSource.setMessagePassword(MessagePassword(password, hint))

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `emits password updated signal when set external encryption password`() = runTest {
        // Given
        val password = "password"
        val hint = "hint"
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setPassword(password, hint) } returns VoidDraftPasswordResult.Ok

        // When
        dataSource.setMessagePassword(MessagePassword(password, hint))

        // Then
        coVerify { composerSignals.emitPasswordChanged() }
    }

    @Test
    fun `set external encryption password returns error when unsuccessful`() = runTest {
        // Given
        val expected = MessagePasswordError.PasswordTooShort
        val password = "password"
        val hint = "hint"
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.setPassword(password, hint) } returns VoidDraftPasswordResult.Error(
            DraftPasswordError.Reason(DraftPasswordErrorReason.PASSWORD_TOO_SHORT)
        )

        // When
        val actual = dataSource.setMessagePassword(MessagePassword(password, hint))

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `remove external encryption password returns Unit when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.removePassword() } returns VoidDraftPasswordResult.Ok

        // When
        val actual = dataSource.removeMessagePassword()

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `emits password updated signal when removed external encryption password`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.removePassword() } returns VoidDraftPasswordResult.Ok

        // When
        dataSource.removeMessagePassword()

        // Then
        coVerify { composerSignals.emitPasswordChanged() }
    }

    @Test
    fun `remove external encryption password returns error when unsuccessful`() = runTest {
        // Given
        val expected = MessagePasswordError.Other(DataError.Local.Unknown)
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.removePassword() } returns VoidDraftPasswordResult.Error(
            DraftPasswordError.Other(ProtonError.Unexpected(UnexpectedError.DRAFT))
        )

        // When
        val actual = dataSource.removeMessagePassword()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `get message expiration returns value when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        val expected = DraftExpirationTime.OneDay
        coEvery { expectedDraftWrapper.getMessageExpiration() } returns DraftExpirationTimeResult.Ok(expected)

        // When
        val actual = dataSource.getMessageExpiration()

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `get message expiration returns error when unsuccessful`() = runTest {
        // Given
        val expected = DataError.Local.Unknown
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { expectedDraftWrapper.getMessageExpiration() } returns DraftExpirationTimeResult.Error(
            ProtonError.Unexpected(UnexpectedError.DRAFT)
        )

        // When
        val actual = dataSource.getMessageExpiration()

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `set message expiration returns unit when successful`() = runTest {
        // Given
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        val time = MessageExpirationTime.OneHour
        val localExpiration = DraftExpirationTime.OneHour
        coEvery { expectedDraftWrapper.setMessageExpiration(localExpiration) } returns VoidDraftExpirationResult.Ok

        // When
        val actual = dataSource.setMessageExpiration(time)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `set message expiration returns error when unsuccessful`() = runTest {
        // Given
        val expected = MessageExpirationError.ExpirationTimeTooFarAhead
        val expectedDraftWrapper = expectDraftWrapperReturns()
        every { draftCache.get() } returns expectedDraftWrapper
        val time = MessageExpirationTime.Custom(Instant.DISTANT_FUTURE)
        coEvery { expectedDraftWrapper.setMessageExpiration(any()) } returns VoidDraftExpirationResult.Error(
            DraftExpirationError.Reason(DraftExpirationErrorReason.EXPIRATION_TIME_EXCEEDS30_DAYS)
        )

        // When
        val actual = dataSource.setMessageExpiration(time)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `ignore recipients updates when callback is fired after rust draft was already closed`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.RustJobApplication
        val action = DraftAction.Compose
        val localDraftCreateMode = DraftCreateMode.Empty
        val toRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>(relaxed = true)
        val ccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>(relaxed = true)
        val bccRecipientsWrapperMock = mockk<ComposerRecipientListWrapper>(relaxed = true)
        val expectedDraftWrapper = expectDraftWrapperReturns(
            toRecipientsWrapper = toRecipientsWrapperMock,
            ccRecipientsWrapper = ccRecipientsWrapperMock,
            bccRecipientsWrapper = bccRecipientsWrapperMock
        )

        // Setup: capture the callback during draft creation
        val callbackSlot = slot<ComposerRecipientValidationCallback>()
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        coEvery { createRustDraft(mockUserSession, localDraftCreateMode) } returns expectedDraftWrapper.right()
        every { draftCache.add(expectedDraftWrapper) } just Runs
        every { draftCache.get() } returns expectedDraftWrapper
        coEvery { toRecipientsWrapperMock.registerCallback(capture(callbackSlot)) } just Runs
        coEvery { ccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { bccRecipientsWrapperMock.registerCallback(any()) } just Runs
        coEvery { toRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { ccRecipientsWrapperMock.recipients() } returns emptyList()
        coEvery { bccRecipientsWrapperMock.recipients() } returns emptyList()

        dataSource.create(userId, action)

        // Simulate draft being closed
        every { draftCache.get() } answers { throw IllegalStateException("No draft cached") }

        // When
        callbackSlot.captured.onUpdate()

        // Then
        dataSource.observeRecipientsValidationEvents().test {
            expectNoEvents()
        }
    }

    private fun expectDraftWrapperReturns(
        subject: String = "",
        sender: String = "",
        body: String = "",
        head: String = "",
        mimeType: LocalMimeType = LocalMimeType.TEXT_HTML,
        toRecipientsWrapper: ComposerRecipientListWrapper = mockk(),
        ccRecipientsWrapper: ComposerRecipientListWrapper = mockk(),
        bccRecipientsWrapper: ComposerRecipientListWrapper = mockk(),
        messageId: LocalMessageId = LocalMessageIdSample.AugWeatherForecast,
        sendResult: VoidDraftSendResult = VoidDraftSendResult.Ok
    ) = mockk<DraftWrapper> {
        every { subject() } returns subject
        every { sender() } returns sender
        every { bodyFields() } returns BodyFields(DraftHead(head), DraftBody(body))
        every { mimeType() } returns mimeType
        every { recipientsTo() } returns toRecipientsWrapper
        every { recipientsCc() } returns ccRecipientsWrapper
        every { recipientsBcc() } returns bccRecipientsWrapper
        coEvery { send() } returns sendResult
        coEvery { messageId() } returns DraftMessageIdResult.Ok(messageId)
    }

}
