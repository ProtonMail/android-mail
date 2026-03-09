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

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.RustMessageDataSource
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toLocalThemeOptions
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.testdata.label.rust.LabelAsActionsTestData
import ch.protonmail.android.testdata.label.rust.LocalLabelAsActionTestData
import ch.protonmail.android.testdata.message.MessageThemeOptionsTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.ListActions
import uniffi.mail_uniffi.MessageAction
import uniffi.mail_uniffi.MessageActionSheet
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MovableSystemFolderAction
import uniffi.mail_uniffi.MoveAction
import kotlin.test.assertEquals

class RustMessageActionRepositoryTest {

    private val rustMessageDataSource: RustMessageDataSource = mockk()

    private val repository = RustMessageActionRepository(rustMessageDataSource)

    @Test
    fun `get available actions should return supported available actions when data source exposes them`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkOverrideLight
        val rustThemeOpts = themeOptions.toLocalThemeOptions()
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageId = MessageId("1")
        val rustAvailableActions = MessageActionSheet(
            listOf(MessageAction.Reply, MessageAction.Forward),
            listOf(MessageAction.Star, MessageAction.LabelAs),
            listOf(
                MessageAction.MoveToSystemFolder(MovableSystemFolderAction(Id(5uL), MovableSystemFolder.SPAM)),
                MessageAction.MoveToSystemFolder(
                    MovableSystemFolderAction(Id(10uL), MovableSystemFolder.ARCHIVE)
                )
            ),
            listOf(MessageAction.ViewInDarkMode, MessageAction.ViewInLightMode)
        )

        coEvery {
            rustMessageDataSource.getAvailableActions(
                userId,
                labelId.toLocalLabelId(),
                messageId.toLocalMessageId(),
                rustThemeOpts
            )
        } returns rustAvailableActions.right()

        // When
        val result = repository.getAvailableActions(userId, labelId, messageId, themeOptions)

        // Then
        val expected = AvailableActions(
            listOf(Action.Reply, Action.Forward),
            listOf(Action.Star, Action.Label),
            listOf(Action.Spam, Action.Archive),
            listOf(Action.ViewInDarkMode, Action.ViewInLightMode)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available actions should return error when data source fails`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkOverrideLight
        val rustThemeOpts = themeOptions.toLocalThemeOptions()
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageId = MessageId("1")
        val expectedError = DataError.Local.NoDataCached
        coEvery {
            rustMessageDataSource.getAvailableActions(
                userId,
                labelId.toLocalLabelId(),
                messageId.toLocalMessageId(),
                rustThemeOpts
            )
        } returns expectedError.left()

        // When
        val result = repository.getAvailableActions(userId, labelId, messageId, themeOptions)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `get available system move to actions should return actions when data source exposes them`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val rustMoveToActions = listOf(
            MoveAction.SystemFolder(
                MovableSystemFolderAction(Id(2uL), MovableSystemFolder.ARCHIVE)
            ),
            MoveAction.SystemFolder(
                MovableSystemFolderAction(Id(3uL), MovableSystemFolder.TRASH)
            )
        )

        coEvery {
            rustMessageDataSource.getAvailableSystemMoveToActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns rustMoveToActions.right()

        // When
        val result = repository.getSystemMoveToLocations(userId, labelId, messageIds)

        // Then
        val expected = listOf(
            MailLabel.System(MailLabelId.System(LabelId("2")), SystemLabelId.Archive, 0),
            MailLabel.System(MailLabelId.System(LabelId("3")), SystemLabelId.Trash, 0)
        )
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available system move to actions should return error when data source fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val expectedError = DataError.Local.NoDataCached
        coEvery {
            rustMessageDataSource.getAvailableSystemMoveToActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns expectedError.left()

        // When
        val result = repository.getSystemMoveToLocations(userId, labelId, messageIds)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `get available label as actions should return actions when data source exposes them`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val rustLabelAsActions = listOf(
            LocalLabelAsActionTestData.selectedAction,
            LocalLabelAsActionTestData.unselectedAction,
            LocalLabelAsActionTestData.partiallySelectedAction
        )

        coEvery {
            rustMessageDataSource.getAvailableLabelAsActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns rustLabelAsActions.right()

        // When
        val result = repository.getAvailableLabelAsActions(userId, labelId, messageIds)

        // Then
        val expected = LabelAsActionsTestData.actions
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get available label as actions should return error when data source fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val expectedError = DataError.Local.NoDataCached

        coEvery {
            rustMessageDataSource.getAvailableLabelAsActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns expectedError.left()

        // When
        val result = repository.getAvailableLabelAsActions(userId, labelId, messageIds)

        // Then
        assertEquals(expectedError.left(), result)
    }

    @Test
    fun `get all available bottom bar actions should return all actions when data source exposes them`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val rustAvailableActions = AllListActions(
            listOf(ListActions.Star),
            listOf(ListActions.MarkRead)
        )

        coEvery {
            rustMessageDataSource.getAllAvailableListBottomBarActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns rustAvailableActions.right()

        // When
        val result = repository.getAllListBottomBarActions(userId, labelId, messageIds)

        // Then
        val expected = AllBottomBarActions(listOf(Action.Star), listOf(Action.MarkRead))
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get all available bottom bar actions should return error when data source fails`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val labelId = SystemLabelId.Inbox.labelId
        val messageIds = listOf(MessageId("1"))
        val expected = DataError.Local.CryptoError.left()

        coEvery {
            rustMessageDataSource.getAllAvailableListBottomBarActions(
                userId,
                labelId.toLocalLabelId(),
                messageIds.map { it.toLocalMessageId() }
            )
        } returns expected

        // When
        val result = repository.getAllListBottomBarActions(userId, labelId, messageIds)

        // Then
        assertEquals(expected, result)
    }

}
