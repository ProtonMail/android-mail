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

package ch.protonmail.android.mailmessage.data.usecase

import arrow.core.right
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import uniffi.mail_uniffi.AvailableMoveToDestinationsForMessagesResult
import uniffi.mail_uniffi.CustomFolderDestination
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.InboxDestination
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.Mailbox
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MoveDestination
import uniffi.mail_uniffi.SystemFolderDestination
import uniffi.mail_uniffi.availableMoveToDestinationsForMessages
import kotlin.test.assertEquals

internal class GetRustMessageMoveToActionsTest {

    private val rustMailbox = mockk<Mailbox>()
    private val mailbox = mockk<MailboxWrapper> {
        every { getRustMailbox() } returns rustMailbox
    }
    private val messageIds = listOf(Id(1uL))

    private val getRustMessageMoveToActions = GetRustMessageMoveToActions()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `inbox destination is converted to a system folder`() = runTest {
        // Given
        val inbox = MoveDestination.Inbox(InboxDestination(Id(1uL), MovableSystemFolder.INBOX, emptyList()))
        val archive = MoveDestination.SystemFolder(SystemFolderDestination(Id(2uL), MovableSystemFolder.ARCHIVE))
        mockkStatic(::availableMoveToDestinationsForMessages)
        coEvery { availableMoveToDestinationsForMessages(rustMailbox, messageIds) } returns
            AvailableMoveToDestinationsForMessagesResult.Ok(listOf(inbox, archive))
        val expected = listOf(
            MoveDestination.SystemFolder(SystemFolderDestination(Id(1uL), MovableSystemFolder.INBOX)),
            archive
        )

        // When
        val result = getRustMessageMoveToActions(mailbox, messageIds)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `non-inbox destinations are returned unchanged`() = runTest {
        // Given
        val customFolder = MoveDestination.CustomFolder(
            CustomFolderDestination(Id(100uL), "custom", LabelColor("#fff"), emptyList())
        )
        val trash = MoveDestination.SystemFolder(SystemFolderDestination(Id(3uL), MovableSystemFolder.TRASH))
        mockkStatic(::availableMoveToDestinationsForMessages)
        coEvery { availableMoveToDestinationsForMessages(rustMailbox, messageIds) } returns
            AvailableMoveToDestinationsForMessagesResult.Ok(listOf(customFolder, trash))

        // When
        val result = getRustMessageMoveToActions(mailbox, messageIds)

        // Then
        assertEquals(listOf(customFolder, trash).right(), result)
    }
}
