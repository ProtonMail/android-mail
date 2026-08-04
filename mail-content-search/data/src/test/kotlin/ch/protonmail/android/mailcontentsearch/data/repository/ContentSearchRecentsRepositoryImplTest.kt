/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAddressId
import ch.protonmail.android.mailcommon.data.mapper.LocalAvatarInformation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalRecentFoundItem
import ch.protonmail.android.mailcommon.data.mapper.LocalRecentSearchTerm
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateSearchRecents
import ch.protonmail.android.mailcontentsearch.data.wrapper.SearchRecentsWrapper
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.model.RecentSearchTerm
import ch.protonmail.android.mailmessage.data.mapper.toMessage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ExclusiveLocation
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.MessageFlags
import uniffi.mail_uniffi.MessageSender
import uniffi.mail_uniffi.SystemLabel
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContentSearchRecentsRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val sessionWrapper = mockk<MailUserSessionWrapper>()
    private val recents = mockk<SearchRecentsWrapper>()

    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns sessionWrapper
    }
    private val createSearchRecents = mockk<CreateSearchRecents> {
        every { this@mockk(sessionWrapper) } returns recents
    }

    private val repository = ContentSearchRecentsRepositoryImpl(
        executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher),
        createSearchRecents = createSearchRecents
    )

    @Test
    fun `getRecentSearchTerms maps the rust terms to domain terms`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recentSearchTerms(null, 3u) } returns listOf(
            LocalRecentSearchTerm(query = "invoice", lastUsedAt = LastUsedAt)
        ).right()

        // When
        val result = repository.getRecentSearchTerms(userId, prefix = null, limit = 3)

        // Then
        assertEquals(listOf(RecentSearchTerm(query = "invoice", lastUsedAt = LastUsedAt.toLong())).right(), result)
    }

    @Test
    fun `getRecentSearchTerms forwards the prefix so autocomplete filters in the rust layer`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recentSearchTerms("inv", 3u) } returns emptyList<LocalRecentSearchTerm>().right()

        // When
        repository.getRecentSearchTerms(userId, prefix = "inv", limit = 3)

        // Then
        coVerify { recents.recentSearchTerms("inv", 3u) }
    }

    @Test
    fun `getRecentSearchTerms coerces a negative limit to zero instead of wrapping to a huge UInt`() =
        runTest(dispatcher) {
            // Given
            coEvery { recents.recentSearchTerms(null, 0u) } returns emptyList<LocalRecentSearchTerm>().right()

            // When
            repository.getRecentSearchTerms(userId, prefix = null, limit = -1)

            // Then
            coVerify { recents.recentSearchTerms(null, 0u) }
        }

    @Test
    fun `getRecentFoundItems coerces a negative limit to zero instead of wrapping to a huge UInt`() =
        runTest(dispatcher) {
            // Given
            coEvery { recents.recentFoundItems(0u) } returns emptyList<LocalRecentFoundItem>().right()

            // When
            repository.getRecentFoundItems(userId, limit = -1)

            // Then
            coVerify { recents.recentFoundItems(0u) }
        }

    @Test
    fun `getRecentFoundItems maps the resolved message and keeps the recorded query`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recentFoundItems(5u) } returns listOf(localFoundItem(searchQuery = "invoice")).right()

        // When
        val result = repository.getRecentFoundItems(userId, limit = 5)

        // Then
        assertEquals(listOf(expectedFoundItem(searchQuery = "invoice")).right(), result)
    }

    @Test
    fun `getRecentFoundItems drops a blank recorded query so nothing is highlighted`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recentFoundItems(5u) } returns listOf(localFoundItem(searchQuery = "  ")).right()

        // When
        val result = repository.getRecentFoundItems(userId, limit = 5)

        // Then
        assertEquals(listOf(expectedFoundItem(searchQuery = null)).right(), result)
    }

    @Test
    fun `recordSearchOpen forwards the query with both local ids`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recordSearchOpen(any(), any(), any()) } returns Unit.right()

        // When
        val result = repository.recordSearchOpen(
            userId = userId,
            query = "invoice",
            messageId = MessageId("42"),
            conversationId = ConversationId("7")
        )

        // Then
        assertEquals(Unit.right(), result)
        coVerify { recents.recordSearchOpen("invoice", LocalMessageId(42uL), LocalConversationId(7uL)) }
    }

    @Test
    fun `dismissFoundItem forwards the local message id`() = runTest(dispatcher) {
        // Given
        coEvery { recents.dismissRecentFoundItem(any()) } returns Unit.right()

        // When
        repository.dismissFoundItem(userId, MessageId("42"))

        // Then
        coVerify { recents.dismissRecentFoundItem(LocalMessageId(42uL)) }
    }

    @Test
    fun `errors from the rust layer are surfaced instead of being flattened into unknown`() = runTest(dispatcher) {
        // Given
        coEvery { recents.recentFoundItems(5u) } returns DataError.Local.NotFound.left()

        // When
        val result = repository.getRecentFoundItems(userId, limit = 5)

        // Then
        assertEquals(DataError.Local.NotFound.left(), result)
    }

    @Test
    fun `a missing user session fails without touching the rust layer`() = runTest(dispatcher) {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = repository.touchSearchTerm(userId, "invoice")

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
        coVerify(exactly = 0) { recents.touchRecentSearchTerm(any()) }
    }

    private fun localFoundItem(searchQuery: String?) = LocalRecentFoundItem(
        message = message,
        searchQuery = searchQuery,
        lastOpenedAt = LastOpenedAt
    )

    // The message is already resolved by the Rust layer; mapping it is MessageMapper's job, already under
    // its own test, so the expectation here goes through the same real mapper rather than restating its shape.
    private fun expectedFoundItem(searchQuery: String?) = RecentFoundItem(
        message = message.toMessage(),
        searchQuery = searchQuery,
        lastOpenedAt = LastOpenedAt.toLong()
    )

    private companion object {

        const val LastUsedAt = 1_700_000_000uL
        const val LastOpenedAt = 1_700_000_000uL

        val message = LocalMessageMetadata(
            id = LocalMessageId(42uL),
            conversationId = LocalConversationId(7uL),
            time = 1_600_000_000u,
            size = 2048u,
            displayOrder = 1u,
            subject = "Your invoice",
            unread = true,
            sender = MessageSender(
                address = "billing@example.com",
                bimiSelector = null,
                displaySenderImage = false,
                isProton = false,
                isSimpleLogin = false,
                name = "Billing"
            ),
            toList = emptyList(),
            ccList = emptyList(),
            bccList = emptyList(),
            expirationTime = 0u,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            starred = false,
            addressId = LocalAddressId(1u),
            numAttachments = 0u,
            flags = MessageFlags(0u),
            avatar = LocalAvatarInformation("BI", "#FFFFFF"),
            attachmentsMetadata = emptyList(),
            customLabels = emptyList(),
            location = ExclusiveLocation.System(SystemLabel.INBOX, Id(1u)),
            category = SystemLabel.CATEGORY_DEFAULT,
            snoozedUntil = null,
            isDraft = false,
            isScheduled = false,
            canReply = true,
            displaySnoozeReminder = false
        )
    }
}
