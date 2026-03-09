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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessageAccessor
import ch.protonmail.android.mailmessage.data.usecase.GetRustAllMessageBottomBarActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustAllMessageListBottomBarActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustAvailableMessageActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustMessageLabelAsActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustMessageMoveToActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustSenderImage
import ch.protonmail.android.mailmessage.data.usecase.RustBlockAddress
import ch.protonmail.android.mailmessage.data.usecase.RustDeleteAllMessagesInLabel
import ch.protonmail.android.mailmessage.data.usecase.RustDeleteMessages
import ch.protonmail.android.mailmessage.data.usecase.RustIsMessageSenderBlocked
import ch.protonmail.android.mailmessage.data.usecase.RustLabelMessages
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessageAsLegitimate
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessagesRead
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessagesUnread
import ch.protonmail.android.mailmessage.data.usecase.RustMoveMessages
import ch.protonmail.android.mailmessage.data.usecase.RustReportPhishing
import ch.protonmail.android.mailmessage.data.usecase.RustStarMessages
import ch.protonmail.android.mailmessage.data.usecase.RustUnblockAddress
import ch.protonmail.android.mailmessage.data.usecase.RustUnstarMessages
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import ch.protonmail.android.testdata.message.rust.LocalMessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.CustomFolderAction
import uniffi.mail_uniffi.DraftCancelScheduledSendInfo
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.IsSelected
import uniffi.mail_uniffi.LabelAsAction
import uniffi.mail_uniffi.LabelAsOutput
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MailTheme
import uniffi.mail_uniffi.MessageActionSheet
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MovableSystemFolderAction
import uniffi.mail_uniffi.MoveAction
import uniffi.mail_uniffi.ThemeOpts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

internal class RustMessageDataSourceImplTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val rustMailboxFactory: RustMailboxFactory = mockk()
    private val rustMessageListQuery: RustMessageListQuery = mockk()
    private val rustMessageQuery: RustMessageQuery = mockk()
    private val createRustMessageAccessor = mockk<CreateRustMessageAccessor>()
    private val getRustSenderImage = mockk<GetRustSenderImage>()
    private val rustMarkMessagesRead = mockk<RustMarkMessagesRead>()
    private val rustMarkMessagesUnread = mockk<RustMarkMessagesUnread>()
    private val rustStarMessages = mockk<RustStarMessages>()
    private val rustUnstarMessages = mockk<RustUnstarMessages>()
    private val getRustAllListBottomBarActions = mockk<GetRustAllMessageListBottomBarActions>()
    private val getRustAllMessageBottomBarActions = mockk<GetRustAllMessageBottomBarActions>()
    private val rustDeleteMessages = mockk<RustDeleteMessages>()
    private val rustMoveMessages = mockk<RustMoveMessages>()
    private val rustLabelMessages = mockk<RustLabelMessages>()
    private val getRustAvailableMessageActions = mockk<GetRustAvailableMessageActions>()
    private val getRustMessageMoveToActions = mockk<GetRustMessageMoveToActions>()
    private val getRustMessageLabelAsActions = mockk<GetRustMessageLabelAsActions>()
    private val rustMarkMessageAsLegitimate = mockk<RustMarkMessageAsLegitimate>()
    private val rustUnblockAddress = mockk<RustUnblockAddress>()
    private val rustBlockAddress = mockk<RustBlockAddress>()
    private val rustIsMessageSenderBlocked = mockk<RustIsMessageSenderBlocked>()
    private val rustReportPhishing = mockk<RustReportPhishing>()
    private val rustDeleteAllMessagesInLabel = mockk<RustDeleteAllMessagesInLabel>()
    private val rustCancelScheduleSend = mockk<RustCancelScheduleSendMessage>()
    private val executeWithUserSession = mockk<ExecuteWithUserSession>()

    private val testDispatcher = StandardTestDispatcher()

    private val dataSource = RustMessageDataSourceImpl(
        userSessionRepository,
        rustMailboxFactory,
        rustMessageListQuery,
        rustMessageQuery,
        createRustMessageAccessor,
        getRustSenderImage,
        rustMarkMessagesRead,
        rustMarkMessagesUnread,
        rustStarMessages,
        rustUnstarMessages,
        getRustAllListBottomBarActions,
        getRustAllMessageBottomBarActions,
        rustDeleteMessages,
        rustMoveMessages,
        rustLabelMessages,
        getRustAvailableMessageActions,
        getRustMessageMoveToActions,
        getRustMessageLabelAsActions,
        rustMarkMessageAsLegitimate,
        rustUnblockAddress,
        rustBlockAddress,
        rustIsMessageSenderBlocked,
        rustReportPhishing,
        rustDeleteAllMessagesInLabel,
        rustCancelScheduleSend,
        executeWithUserSession,
        testDispatcher
    )

    @Test
    fun `get message should return message metadata`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val messageId = LocalMessageIdSample.AugWeatherForecast
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { createRustMessageAccessor(mailSession, messageId) } returns
            LocalMessageTestData.AugWeatherForecast.right()

        // When
        val result = dataSource.getMessage(userId, messageId)

        // Then
        assertEquals(LocalMessageTestData.AugWeatherForecast.right(), result)
    }

    @Test
    fun `get message should handle error`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val expectedError = DataError.Local.NoDataCached
        coEvery {
            createRustMessageAccessor.invoke(mailSession, messageId)
        } returns expectedError.left()

        // When
        val result = dataSource.getMessage(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `get messages should return list of message metadata`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        val pageKey = PageKey.DefaultPageKey()
        val messages = listOf(
            LocalMessageTestData.AugWeatherForecast,
            LocalMessageTestData.SepWeatherForecast,
            LocalMessageTestData.OctWeatherForecast
        )
        coEvery { rustMessageListQuery.getMessages(userId, pageKey) } returns messages.right()

        // When
        val result = dataSource.getMessages(userId, pageKey)

        // Then
        coVerify { rustMessageListQuery.getMessages(userId, pageKey) }
        assertEquals(messages.right(), result)
    }

    @Test
    fun `getSenderImage should return sender image when session is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val address = "test@example.com"
        val bimi = "bimiSelector"
        val expectedImage = "image.png"

        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { getRustSenderImage(mailSession, address, bimi) } returns expectedImage.right()

        // When
        val result = dataSource.getSenderImage(userId, address, bimi)

        // Then
        coVerify { getRustSenderImage(mailSession, address, bimi) }
        assertEquals(expectedImage, result)
    }

    @Test
    fun `getSenderImage should return null when session is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val address = "test@example.com"
        val bimi = "bimiSelector"

        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = dataSource.getSenderImage(userId, address, bimi)

        // Then
        coVerify(exactly = 0) { getRustSenderImage(any(), any(), any()) }
        assertNull(result)
    }

    @Test
    fun `getSenderImage should return null when error occurs`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val address = "test@example.com"
        val bimi = "bimiSelector"

        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery {
            getRustSenderImage(
                mailSession,
                address,
                bimi
            )
        } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.getSenderImage(userId, address, bimi)

        // Then
        assertNull(result)
    }

    @Test
    fun `should mark messages as read when session and labelId are available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.createAllMail(userId) } returns mailbox.right()
        coEvery { rustMarkMessagesRead(mailbox, messageIds) } returns Unit.right()

        // When
        val result = dataSource.markRead(userId, messageIds)

        // Then
        assertTrue(result.isRight())
        coVerify { rustMarkMessagesRead(mailbox, messageIds) }
    }

    @Test
    fun `should not mark messages as read when mailbox is null`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.createAllMail(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.markRead(userId, messageIds)

        // Then
        assertTrue(result.isLeft())
        verify { rustMarkMessagesRead wasNot Called }
    }

    @Test
    fun `should handle error when marking messages as read`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMarkMessagesRead(mailbox, messageIds) } returns DataError.Local.CryptoError.left()
        coEvery { rustMailboxFactory.createAllMail(userId) } returns mailbox.right()

        // When
        val result = dataSource.markRead(userId, messageIds)

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `should mark messages as unread when session and labelId are available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.createAllMail(userId) } returns mailbox.right()
        coEvery { rustMarkMessagesUnread(mailbox, messageIds) } returns Unit.right()

        // When
        val result = dataSource.markUnread(userId, messageIds)

        // Then
        assertTrue(result.isRight())
        coVerify { rustMarkMessagesUnread(mailbox, messageIds) }
    }

    @Test
    fun `should star messages when session is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { rustStarMessages(mailSession, messageIds) } returns Unit.right()

        // When
        val result = dataSource.starMessages(userId, messageIds)

        // Then
        coVerify { rustStarMessages(mailSession, messageIds) }
        assert(result.isRight())
    }

    @Test
    fun `should not mark messages as unread when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.createAllMail(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.markUnread(userId, messageIds)

        // Then
        assertTrue(result.isLeft())
        verify { rustMarkMessagesUnread wasNot Called }
    }

    @Test
    fun `should handle error when marking messages as unread`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMarkMessagesUnread(mailbox, messageIds) } returns DataError.Local.NoDataCached.left()
        coEvery { rustMailboxFactory.createAllMail(userId) } returns mailbox.right()

        // When
        val result = dataSource.markUnread(userId, messageIds)

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `should unstar messages when session is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { rustUnstarMessages(mailSession, messageIds) } returns Unit.right()

        // When
        val result = dataSource.unStarMessages(userId, messageIds)

        // Then
        coVerify { rustUnstarMessages(mailSession, messageIds) }
        assert(result.isRight())
    }

    @Test
    fun `should not unstar messages when session is null`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = dataSource.unStarMessages(userId, messageIds)

        // Then
        coVerify(exactly = 0) { rustUnstarMessages(any(), any()) }
        assert(result.isLeft())
    }

    @Test
    fun `should handle error when unstarring messages`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailSession = mockk<MailUserSessionWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { rustUnstarMessages(mailSession, messageIds) } returns DataError.Local.NoDataCached.left()

        // When
        val result = dataSource.unStarMessages(userId, messageIds)

        // Then
        coVerify { rustUnstarMessages(mailSession, messageIds) }
        assert(result.isLeft())
    }

    @Test
    fun `get available actions should return available message actions`() = runTest(testDispatcher) {
        // Given
        val themeOptions = ThemeOpts(MailTheme.DARK_MODE)
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val expected = MessageActionSheet(emptyList(), emptyList(), emptyList(), emptyList())

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustAvailableMessageActions(mailbox, messageId, themeOptions) } returns expected.right()

        // When
        val result = dataSource.getAvailableActions(userId, labelId, messageId, themeOptions)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available system move to actions should return only available actions towards system folders`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LocalLabelId(1uL)
            val mailbox = mockk<MailboxWrapper>()
            val messageId = LocalMessageIdSample.AugWeatherForecast
            val archive = MovableSystemFolderAction(Id(2uL), MovableSystemFolder.ARCHIVE)
            val customFolder = CustomFolderAction(
                Id(100uL),
                "custom",
                LabelColor("#fff"),
                emptyList()
            )
            val allMoveToActions = listOf(MoveAction.SystemFolder(archive), MoveAction.CustomFolder(customFolder))
            val expected = listOf(MoveAction.SystemFolder(archive))

            coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
            coEvery { getRustMessageMoveToActions(mailbox, listOf(messageId)) } returns allMoveToActions.right()

            // When
            val result = dataSource.getAvailableSystemMoveToActions(userId, labelId, listOf(messageId))

            // Then
            assertEquals(expected.right(), result)
        }

    @Test
    fun `get available label as actions should return available message actions`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast)
        val expected = listOf(
            LabelAsAction(Id(1uL), "label", LabelColor("#fff"), 0.toUInt(), IsSelected.UNSELECTED),
            LabelAsAction(Id(2uL), "label2", LabelColor("#000"), 0.toUInt(), IsSelected.SELECTED)
        )

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustMessageLabelAsActions(mailbox, messageIds) } returns expected.right()

        // When
        val result = dataSource.getAvailableLabelAsActions(userId, labelId, messageIds)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get all available bottom bar actions should return all available bottom bar actions`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LocalLabelId(1uL)
            val mailbox = mockk<MailboxWrapper>()
            val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast)
            val expected = AllListActions(emptyList(), emptyList())

            coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
            coEvery { getRustAllListBottomBarActions(mailbox, messageIds) } returns expected.right()

            // When
            val result = dataSource.getAllAvailableListBottomBarActions(userId, labelId, messageIds)

            // Then
            assertEquals(expected.right(), result)
        }

    @Test
    fun `get all available bottom bar actions should return error when rust throws exception`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LocalLabelId(1uL)
            val mailbox = mockk<MailboxWrapper>()
            val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast)
            val expected = DataError.Local.CryptoError

            coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
            coEvery { getRustAllListBottomBarActions(mailbox, messageIds) } returns expected.left()

            // When
            val result = dataSource.getAllAvailableListBottomBarActions(userId, labelId, messageIds)

            // Then
            assertEquals(expected.left(), result)
        }

    @Test
    fun `should not delete messages when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.createAllMail(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.deleteMessages(userId, messageIds)

        // Then
        assertTrue(result.isLeft())
        verify { rustDeleteMessages wasNot Called }
    }

    @Test
    fun `should handle exception when deleting messages`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustDeleteMessages(mailbox, messageIds) } returns expectedError.left()
        coEvery { rustMailboxFactory.createAllMail(userId) } returns mailbox.right()

        // When
        val result = dataSource.deleteMessages(userId, messageIds)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should not move messages when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.moveMessages(userId, messageIds, labelId)

        // Then
        assertTrue(result.isLeft())
        verify { rustMoveMessages wasNot Called }
    }

    @Test
    fun `should handle exception when moving messages`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val labelId = LocalLabelId(1uL)
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustMoveMessages(mailbox, labelId, messageIds) } returns expectedError.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.moveMessages(userId, messageIds, labelId)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should label messages when mailbox is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false
        val mailbox = mockk<MailboxWrapper>()

        coEvery {
            rustLabelMessages(mailbox, messageIds, selectedLabelIds, partiallySelectedLabelIds, shouldArchive)
        } returns mockk<LabelAsOutput>().right()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.labelMessages(
            userId,
            messageIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertTrue { result is Either.Right }
        coVerify { rustLabelMessages(mailbox, messageIds, selectedLabelIds, partiallySelectedLabelIds, shouldArchive) }
    }

    @Test
    fun `should not label messages when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.labelMessages(
            userId,
            messageIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertTrue(result.isLeft())
        verify { rustLabelMessages wasNot Called }
    }

    @Test
    fun `should handle error when labelling messages`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageIds = listOf(LocalMessageId(1uL), LocalMessageId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false
        val expectedError = DataError.Local.NoDataCached

        coEvery {
            rustLabelMessages(mailbox, messageIds, selectedLabelIds, partiallySelectedLabelIds, shouldArchive)
        } returns expectedError.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.labelMessages(
            userId,
            messageIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should mark message as legitimate when mailbox is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageId(1uL)
        val mailbox = mockk<MailboxWrapper>()

        coEvery { rustMarkMessageAsLegitimate(mailbox, messageId) } returns Unit.right()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.markMessageAsLegitimate(userId, messageId)

        // Then
        assertTrue(result.isRight())
        coVerify { rustMarkMessageAsLegitimate(mailbox, messageId) }
    }

    @Test
    fun `should not mark message as legitimate when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageId(1uL)

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.markMessageAsLegitimate(userId, messageId)

        // Then
        assertTrue(result.isLeft())
        verify { rustMarkMessageAsLegitimate wasNot Called }
    }

    @Test
    fun `should handle error when marking message as legitimate`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageId = LocalMessageId(1uL)
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustMarkMessageAsLegitimate(mailbox, messageId) } returns expectedError.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.markMessageAsLegitimate(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should unblock address when mailbox is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val email = "abc@pm.me"
        val mailbox = mockk<MailboxWrapper>()

        coEvery { rustUnblockAddress(mailbox, email) } returns Unit.right()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.unblockSender(userId, email)

        // Then
        assertTrue(result.isRight())
        coVerify { rustUnblockAddress(mailbox, email) }
    }

    @Test
    fun `should not unblock address when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val email = "abc@pm.me"

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.unblockSender(userId, email)

        // Then
        assertTrue(result.isLeft())
        verify { rustUnblockAddress wasNot Called }
    }

    @Test
    fun `should handle error when unblocking address`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val email = "abc@pm.me"
        val mailbox = mockk<MailboxWrapper>()
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustUnblockAddress(mailbox, email) } returns expectedError.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.unblockSender(userId, email)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should report phishing when mailbox is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val mailbox = mockk<MailboxWrapper>()

        coEvery { rustReportPhishing(mailbox, messageId) } returns Unit.right()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.reportPhishing(userId, messageId)

        // Then
        assertTrue(result.isRight())
        coVerify { rustReportPhishing(mailbox, messageId) }
    }

    @Test
    fun `should not report phishing when mailbox is not available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.reportPhishing(userId, messageId)

        // Then
        assertTrue(result.isLeft())
        verify { rustReportPhishing wasNot Called }
    }

    @Test
    fun `should handle error when report phishing`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast
        val mailbox = mockk<MailboxWrapper>()
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustReportPhishing(mailbox, messageId) } returns expectedError.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.reportPhishing(userId, messageId)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `should delete all messages in location when session is available and use case returns success`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LabelIdSample.Trash.toLocalLabelId()
            val mailSession = mockk<MailUserSessionWrapper>()

            coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
            coEvery { rustDeleteAllMessagesInLabel(mailSession, labelId) } returns Unit.right()

            // When
            val result = dataSource.deleteAllMessagesInLocation(userId, labelId)

            // Then
            coVerify { rustDeleteAllMessagesInLabel(mailSession, labelId) }
            assertEquals(Unit.right(), result)
        }

    @Test
    fun `delete all messages in location should return error when session is not available`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LabelIdSample.Trash.toLocalLabelId()

            coEvery { userSessionRepository.getUserSession(userId) } returns null

            // When
            val result = dataSource.deleteAllMessagesInLocation(userId, labelId)

            // Then
            coVerify(exactly = 0) { rustDeleteAllMessagesInLabel(any(), any()) }
            assertEquals(DataError.Local.NoUserSession.left(), result)
        }

    @Test
    fun `delete all messages in location should return error when operation was unsuccessful`() =
        runTest(testDispatcher) {
            // Given
            val userId = UserIdTestData.userId
            val labelId = LabelIdSample.Trash.toLocalLabelId()
            val mailSession = mockk<MailUserSessionWrapper>()

            coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
            coEvery { rustDeleteAllMessagesInLabel(mailSession, labelId) } returns DataError.Local.CryptoError.left()

            // When
            val result = dataSource.deleteAllMessagesInLocation(userId, labelId)

            // Then
            coVerify { rustDeleteAllMessagesInLabel(mailSession, labelId) }
            assertEquals(DataError.Local.CryptoError.left(), result)
        }

    @Test
    fun `cancel schedule send returns error when session is null`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = MessageIdSample.LocalDraft
        val expected = UndoSendError.Other(DataError.Local.NoUserSession)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val actual = dataSource.cancelScheduleSendMessage(userId, messageId)

        // Then
        assertEquals(expected.left(), actual)
    }

    @Test
    fun `cancel schedule send returns error when rustDraftUndoSend fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = MessageId("810")
        val expectedError = UndoSendError.UndoSendFailed
        val mailSession = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { rustCancelScheduleSend(mailSession, messageId.toLocalMessageId()) } returns expectedError.left()

        // When
        val actual = dataSource.cancelScheduleSendMessage(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `cancel schedule send returns previous schedule time when successful`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = MessageId("810")
        val mailSession = mockk<MailUserSessionWrapper>()
        val lastScheduledTime = 123uL
        val scheduleInfo = DraftCancelScheduledSendInfo(lastScheduledTime)
        val lastScheduleTime = PreviousScheduleSendTime(Instant.fromEpochSeconds(lastScheduledTime.toLong()))
        coEvery { userSessionRepository.getUserSession(userId) } returns mailSession
        coEvery { rustCancelScheduleSend(mailSession, messageId.toLocalMessageId()) } returns scheduleInfo.right()

        // When
        val actual = dataSource.cancelScheduleSendMessage(userId, messageId)

        // Then
        assertEquals(lastScheduleTime.right(), actual)
    }

    @Test
    fun `isMessageSenderBlocked returns result when mailbox is available`() = runTest(testDispatcher) {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val messageId = LocalMessageIdSample.AugWeatherForecast

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery { rustIsMessageSenderBlocked(mailbox, messageId) } returns true.right()

        // When
        val result = dataSource.isMessageSenderBlocked(userId, messageId)

        // Then
        assertEquals(true.right(), result)
        coVerify { rustIsMessageSenderBlocked(mailbox, messageId) }
    }
}
