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

package ch.protonmail.android.mailconversation.data.local

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationBottomBarActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationBottomSheetActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationLabelAsActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationListBottomBarActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationMoveToActions
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import ch.protonmail.android.mailmessage.data.model.LocalConversationWithMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.conversation.rust.LocalConversationIdSample
import ch.protonmail.android.testdata.conversation.rust.LocalConversationTestData
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import ch.protonmail.android.testdata.message.rust.LocalMessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.ConversationActionSheet
import uniffi.mail_uniffi.CustomFolderAction
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.IsSelected
import uniffi.mail_uniffi.LabelAsAction
import uniffi.mail_uniffi.LabelAsOutput
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MovableSystemFolderAction
import uniffi.mail_uniffi.MoveAction
import uniffi.mail_uniffi.Undo
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RustConversationDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rustMailboxFactory: RustMailboxFactory = mockk()
    private val rustConversationDetailQuery: RustConversationDetailQuery = mockk()
    private val rustConversationsQuery: RustConversationsQuery = mockk()
    private val getRustConversationBottomSheetActions = mockk<GetRustConversationBottomSheetActions>()
    private val getRustConversationListBottomBarActions = mockk<GetRustConversationListBottomBarActions>()

    private val getRustConversationBottomBarActions = mockk<GetRustConversationBottomBarActions>()
    private val getRustConversationMoveToActions = mockk<GetRustConversationMoveToActions>()
    private val getRustConversationLabelAsActions = mockk<GetRustConversationLabelAsActions>()
    private val rustDeleteConversations = mockk<RustDeleteConversations>()
    private val rustMoveConversations = mockk<RustMoveConversations>()
    private val rustLabelConversations = mockk<RustLabelConversations>()
    private val rustMarkConversationsAsRead = mockk<RustMarkConversationsAsRead>()
    private val rustMarkConversationsAsUnread = mockk<RustMarkConversationsAsUnread>()
    private val executeWithUserSession = mockk<ExecuteWithUserSession>()

    private val dataSource = RustConversationDataSourceImpl(
        rustMailboxFactory,
        rustMoveConversations,
        rustLabelConversations,
        rustConversationDetailQuery,
        rustConversationsQuery,
        getRustConversationListBottomBarActions,
        getRustConversationBottomBarActions,
        getRustConversationBottomSheetActions,
        getRustConversationMoveToActions,
        getRustConversationLabelAsActions,
        rustDeleteConversations,
        rustMarkConversationsAsRead,
        rustMarkConversationsAsUnread,
        executeWithUserSession,
        mainDispatcherRule.testDispatcher
    )

    @Test
    fun `get conversations should return list of conversations`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val pageKey = PageKey.DefaultPageKey(labelId.toLabelId())
        val conversations = listOf(
            LocalConversationTestData.AugConversation,
            LocalConversationTestData.SepConversation,
            LocalConversationTestData.OctConversation
        )
        coEvery { rustConversationsQuery.getConversations(userId, pageKey) } returns conversations.right()

        // When
        val result = dataSource.getConversations(userId, pageKey)

        // Then
        coVerify { rustConversationsQuery.getConversations(userId, pageKey) }
        assertEquals(conversations.right(), result)
    }

    @Test
    fun `observeConversationWithMessages should return conversation + messages`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val conversationId = LocalConversationId(1uL)
        val messages = listOf(
            LocalMessageTestData.AugWeatherForecast,
            LocalMessageTestData.SepWeatherForecast,
            LocalMessageTestData.OctWeatherForecast
        )

        val expectedResult = LocalConversationWithMessages(
            conversation = LocalConversationTestData.AugConversation,
            messages = LocalConversationMessages(
                messageIdToOpen = LocalMessageIdSample.AugWeatherForecast,
                messages = messages
            )
        )

        val localLabelId = LocalLabelId(3uL)
        val entryPoint = ConversationDetailEntryPoint.PushNotification
        val showAll = false
        coEvery {
            rustConversationDetailQuery.observeConversationWithMessages(
                userId, conversationId, localLabelId, entryPoint, showAll
            )
        } returns flowOf(expectedResult.right())

        // When
        dataSource.observeConversationWithMessages(userId, conversationId, localLabelId, entryPoint, showAll).test {

            // Then
            val result = awaitItem()
            assertEquals(expectedResult.right(), result)
            coVerify {
                rustConversationDetailQuery.observeConversationWithMessages(
                    userId,
                    conversationId,
                    localLabelId,
                    entryPoint,
                    showAll
                )
            }

            awaitComplete()
        }
    }

    @Test
    fun `get available actions should return available conversation actions`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationId = LocalConversationIdSample.OctConversation
        val expected = ConversationActionSheet(emptyList(), emptyList())

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustConversationBottomSheetActions(mailbox, conversationId) } returns expected.right()

        // When
        val result = dataSource.getAvailableBottomSheetActions(userId, labelId, conversationId)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available system move to actions should return only available actions towards system folders`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationIdSample.OctConversation)
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
        coEvery { getRustConversationMoveToActions(mailbox, conversationIds) } returns allMoveToActions.right()

        // When
        val result = dataSource.getAvailableSystemMoveToActions(userId, labelId, conversationIds)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available label as actions should return available conversation actions`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationIdSample.AugConversation)
        val expected = listOf(
            LabelAsAction(Id(1uL), "label", LabelColor("#fff"), 0.toUInt(), IsSelected.UNSELECTED),
            LabelAsAction(Id(2uL), "label2", LabelColor("#000"), 0.toUInt(), IsSelected.SELECTED)
        )

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustConversationLabelAsActions(mailbox, conversationIds) } returns expected.right()

        // When
        val result = dataSource.getAvailableLabelAsActions(userId, labelId, conversationIds)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get all available actions should return all available bottom bar actions`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationIdSample.OctConversation)
        val expected = AllListActions(emptyList(), emptyList())

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustConversationListBottomBarActions(mailbox, conversationIds) } returns expected.right()

        // When
        val result = dataSource.getAllAvailableListBottomBarActions(userId, labelId, conversationIds)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get all available actions should return error when rust returns error`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationIdSample.OctConversation)
        val expectedError = DataError.Local.NoDataCached

        coEvery { rustMailboxFactory.create(userId, labelId) } returns mailbox.right()
        coEvery { getRustConversationListBottomBarActions(mailbox, conversationIds) } returns
            expectedError.left()

        // When
        val result = dataSource.getAllAvailableListBottomBarActions(userId, labelId, conversationIds)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `move conversations invokes rust move conversations`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = LocalLabelId(1uL)
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationIdSample.AugConversation)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery { rustMoveConversations(mailbox, labelId, conversationIds) } returns mockk<Undo>().right()

        // When
        dataSource.moveConversations(userId, conversationIds, labelId)

        // Then
        coVerify { rustMoveConversations(mailbox, labelId, conversationIds) }
    }

    @Test
    fun `should label conversations when mailbox is available`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val conversationIds = listOf(LocalConversationId(1uL), LocalConversationId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false
        val mailbox = mockk<MailboxWrapper>()
        val labelAsOutput = mockk<LabelAsOutput>()

        coEvery {
            rustLabelConversations(mailbox, conversationIds, selectedLabelIds, partiallySelectedLabelIds, shouldArchive)
        } returns labelAsOutput.right()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.labelConversations(
            userId,
            conversationIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertTrue { result is Either.Right }
        coVerify {
            rustLabelConversations(
                mailbox,
                conversationIds,
                selectedLabelIds,
                partiallySelectedLabelIds,
                shouldArchive
            )
        }
    }

    @Test
    fun `should not label conversations when mailbox creation fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val conversationIds = listOf(LocalConversationId(1uL), LocalConversationId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false

        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.labelConversations(
            userId,
            conversationIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertEquals(DataError.Local.IllegalStateError.left(), result)
        verify { rustLabelConversations wasNot Called }
    }

    @Test
    fun `should handle error when labelling conversations`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val mailbox = mockk<MailboxWrapper>()
        val conversationIds = listOf(LocalConversationId(1uL), LocalConversationId(2uL))
        val selectedLabelIds = listOf(LocalLabelId(3uL), LocalLabelId(4uL))
        val partiallySelectedLabelIds = listOf(LocalLabelId(5uL))
        val shouldArchive = false
        val error = DataError.Local.NoDataCached

        coEvery {
            rustLabelConversations(mailbox, conversationIds, selectedLabelIds, partiallySelectedLabelIds, shouldArchive)
        } returns error.left()
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()

        // When
        val result = dataSource.labelConversations(
            userId,
            conversationIds,
            selectedLabelIds,
            partiallySelectedLabelIds,
            shouldArchive
        )

        // Then
        assertEquals(error.left(), result)
    }
}
