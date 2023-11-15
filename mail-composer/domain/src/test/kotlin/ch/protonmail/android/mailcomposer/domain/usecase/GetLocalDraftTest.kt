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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

class GetLocalDraftTest {

    private val createEmptyDraftMock = mockk<CreateEmptyDraft>()
    private val findLocalDraftMock = mockk<FindLocalDraft>()
    private val resolveUserAddressMock = mockk<ResolveUserAddress>()

    private val getLocalDraft = GetLocalDraft(
        createEmptyDraftMock,
        findLocalDraftMock,
        resolveUserAddressMock
    )

    @Test
    fun `returns error when sender email fails being resolved`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val senderEmail = SenderEmail("unresolvable@sender.email")
        val draftMessageId = MessageIdSample.build()
        expectResolveUserAddressFailure(userId, senderEmail)

        // When
        val actualEither = getLocalDraft(userId, draftMessageId, senderEmail)

        // Then
        assertEquals(GetLocalDraft.Error.ResolveUserAddressError.left(), actualEither)
    }

    @Test
    fun `returns existing draft when it exists locally`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val userAddress = UserAddressSample.PrimaryAddress
        val senderEmail = SenderEmail(userAddress.email)
        val draftMessageId = MessageIdSample.build()
        val expectedExistingDraft = expectedExistingDraft(userId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        expectedResolvedUserAddress(userId, senderEmail) { userAddress }

        // When
        val actualEither = getLocalDraft(userId, draftMessageId, senderEmail)

        // Then
        assertEquals(expectedExistingDraft.right(), actualEither)
    }

    @Test
    fun `create and returns new empty draft when it does not exist locally`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val userAddress = UserAddressSample.PrimaryAddress
        val senderEmail = SenderEmail(userAddress.email)
        val draftMessageId = MessageIdSample.build()
        val expectedNewDraft = expectedNewDraft(userId, draftMessageId, userAddress) {
            MessageWithBodySample.EmptyDraft
        }
        expectedResolvedUserAddress(userId, senderEmail) { userAddress }

        // When
        val actualEither = getLocalDraft(userId, draftMessageId, senderEmail)

        // Then
        assertEquals(expectedNewDraft.right(), actualEither)
    }

    private fun expectedNewDraft(
        userId: UserId,
        messageId: MessageId,
        senderAddress: UserAddress,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { findLocalDraftMock(userId, messageId) } returns null
        every { createEmptyDraftMock(messageId, userId, senderAddress) } returns it
    }

    private fun expectedExistingDraft(
        userId: UserId,
        messageId: MessageId,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { findLocalDraftMock(userId, messageId) } returns it
    }

    private fun expectedResolvedUserAddress(
        userId: UserId,
        email: SenderEmail,
        address: () -> UserAddress
    ) = address().also { coEvery { resolveUserAddressMock(userId, email.value) } returns it.right() }

    private fun expectResolveUserAddressFailure(userId: UserId, email: SenderEmail) {
        coEvery {
            resolveUserAddressMock(
                userId,
                email.value
            )
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }
}
