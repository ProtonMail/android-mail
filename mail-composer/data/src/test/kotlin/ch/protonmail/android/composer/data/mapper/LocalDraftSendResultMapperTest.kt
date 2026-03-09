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

package ch.protonmail.android.composer.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalDraftSendResult
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import org.junit.Test
import uniffi.mail_uniffi.DraftSendResultOrigin
import uniffi.mail_uniffi.DraftSendStatus
import uniffi.mail_uniffi.Id
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

class LocalDraftSendResultMapperTest {

    @Test
    fun `maps to message sent undoable when source send and there is time remaining to undo`() {
        // Given
        val messageId = Id(123uL)
        val timestamp = 456uL
        val secondsLeftToUndo = 3uL
        val deliveryTime = 678uL
        val localDraftSendResult = LocalDraftSendResult(
            messageId,
            timestamp,
            error = DraftSendStatus.Success(secondsLeftToUndo, deliveryTime),
            origin = DraftSendResultOrigin.SEND
        )
        val expected = MessageSendingStatus.MessageSentUndoable(
            messageId.toMessageId(),
            secondsLeftToUndo.toInt().toDuration(DurationUnit.SECONDS)
        )

        // When
        val actual = localDraftSendResult.toMessageSendingStatus()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps to message sent final when source send and no time to undo left`() {
        // Given
        val messageId = Id(123uL)
        val timestamp = 456uL
        val secondsLeftToUndo = 0uL
        val deliveryTime = 678uL
        val localDraftSendResult = LocalDraftSendResult(
            messageId,
            timestamp,
            error = DraftSendStatus.Success(secondsLeftToUndo, deliveryTime),
            origin = DraftSendResultOrigin.SEND
        )
        val expected = MessageSendingStatus.MessageSentFinal(messageId.toMessageId())

        // When
        val actual = localDraftSendResult.toMessageSendingStatus()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps to no status when source not a send event`() {
        // Given
        val messageId = Id(123uL)
        val timestamp = 456uL
        val secondsLeftToUndo = 0uL
        val deliveryTime = 678uL
        val localDraftSendResult = LocalDraftSendResult(
            messageId,
            timestamp,
            error = DraftSendStatus.Success(secondsLeftToUndo, deliveryTime),
            origin = DraftSendResultOrigin.ATTACHMENT_UPLOAD
        )
        val expected = MessageSendingStatus.NoStatus(messageId.toMessageId())

        // When
        val actual = localDraftSendResult.toMessageSendingStatus()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps to message scheduled send undoable when source schedule and there is time remaining to undo`() {
        // Given
        val messageId = Id(123uL)
        val timestamp = 456uL
        val secondsLeftToUndo = 3uL
        val deliveryTime = 678uL
        val localDraftSendResult = LocalDraftSendResult(
            messageId,
            timestamp,
            error = DraftSendStatus.Success(secondsLeftToUndo, deliveryTime),
            origin = DraftSendResultOrigin.SCHEDULE_SEND
        )
        val expected = MessageSendingStatus.MessageScheduledUndoable(
            messageId.toMessageId(),
            Instant.fromEpochSeconds(deliveryTime.toLong())
        )

        // When
        val actual = localDraftSendResult.toMessageSendingStatus()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps to message sent final when source schedule send and no time to undo left`() {
        // Given
        val messageId = Id(123uL)
        val timestamp = 456uL
        val secondsLeftToUndo = 0uL
        val deliveryTime = 678uL
        val localDraftSendResult = LocalDraftSendResult(
            messageId,
            timestamp,
            error = DraftSendStatus.Success(secondsLeftToUndo, deliveryTime),
            origin = DraftSendResultOrigin.SCHEDULE_SEND
        )
        val expected = MessageSendingStatus.MessageSentFinal(messageId.toMessageId())

        // When
        val actual = localDraftSendResult.toMessageSendingStatus()

        // Then
        assertEquals(expected, actual)
    }
}
