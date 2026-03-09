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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalAddressId
import ch.protonmail.android.mailcommon.data.mapper.LocalAvatarInformation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import uniffi.mail_uniffi.ExclusiveLocation
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.MessageFlags
import uniffi.mail_uniffi.MessageRecipient
import uniffi.mail_uniffi.MessageSender
import uniffi.mail_uniffi.SystemLabel
import kotlin.test.Test

class MessageMapperTest {
    @Test
    fun `should map LocalMessageMetadata to Message correctly`() {
        val exclusiveLocation = ExclusiveLocation.System(SystemLabel.SENT, Id(100u))

        // Given
        val localMessageMetadata = LocalMessageMetadata(
            id = LocalMessageId(1234u),
            conversationId = LocalConversationId(5678u),
            time = 1_625_247_600u,
            size = 2048u,
            displayOrder = 1u,
            subject = "Test Subject",
            unread = true,
            sender = MessageSender(
                address = "sender@test.com",
                bimiSelector = "sender bimiSelector",
                displaySenderImage = false,
                isProton = true,
                isSimpleLogin = false,
                name = "Sender Name"
            ),
            toList = listOf(
                MessageRecipient(
                    address = "to@test.com",
                    isProton = false,
                    name = "Recipient Name",
                    group = null
                )
            ),
            ccList = emptyList(),
            bccList = emptyList(),
            expirationTime = 0u,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            starred = true,
            addressId = LocalAddressId(91011u),
            numAttachments = 2u,
            flags = MessageFlags(12_345u),
            avatar = LocalAvatarInformation("SN", "#FFFFFF"),
            attachmentsMetadata = emptyList(),
            customLabels = emptyList(),
            location = exclusiveLocation,
            snoozedUntil = 12345u,
            isDraft = false,
            isScheduled = false,
            canReply = false,
            displaySnoozeReminder = false
        )

        // When
        val result = localMessageMetadata.toMessage()

        // Then
        assertEquals("1234", result.messageId.id)
        assertEquals("5678", result.conversationId.id)
        assertEquals(1_625_247_600L, result.time)
        assertEquals(2048L, result.size)
        assertEquals(1L, result.order)
        assertEquals("Test Subject", result.subject)
        assertTrue(result.isUnread)
        assertEquals("sender@test.com", result.sender.address)
        assertEquals("Sender Name", result.sender.name)
        assertEquals("sender bimiSelector", result.sender.bimiSelector)
        assertEquals(1, result.toList.size)
        assertEquals("to@test.com", result.toList.first().address)
        assertEquals("Recipient Name", result.toList.first().name)
        assertEquals(0, result.ccList.size)
        assertEquals(0, result.bccList.size)
        assertEquals(0L, result.expirationTime)
        assertFalse(result.isReplied)
        assertFalse(result.isRepliedAll)
        assertFalse(result.isForwarded)
        assertTrue(result.isStarred)
        assertEquals("91011", result.addressId.id)
        assertEquals(2, result.numAttachments)
        assertEquals(12_345L, result.flags)
        assertEquals("SN", result.avatarInformation.initials)
        assertEquals("#FFFFFF", result.avatarInformation.color)
    }

}
