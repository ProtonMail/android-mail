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

package ch.protonmail.android.mailtrackingprotection.data.trackers

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailtrackingprotection.data.wrapper.PrivacyInfoState
import ch.protonmail.android.mailtrackingprotection.data.wrapper.PrivacyInfoWrapper
import ch.protonmail.android.mailtrackingprotection.domain.model.BlockedTracker
import ch.protonmail.android.mailtrackingprotection.domain.model.CleanedLink
import ch.protonmail.android.mailtrackingprotection.domain.model.PrivacyItemsResult
import ch.protonmail.android.mailtrackingprotection.domain.repository.PrivacyInfoRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.StrippedUtmInfo
import uniffi.mail_uniffi.TrackerDomain
import uniffi.mail_uniffi.TrackerInfo
import uniffi.mail_uniffi.UtmLink
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PrivacyInfoRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PrivacyInfoRepository

    private val mockUserSessionRepository = mockk<UserSessionRepository>()
    private val mockDataSource = mockk<RustPrivacyInfoDataSource>()
    private val mockUserSession = mockk<MailUserSessionWrapper>(relaxed = true)
    private val mockRustSession = mockk<MailUserSession>()

    private val testUserId = UserId("test-user-id")
    private val testMessageId = MessageId("123")

    @BeforeTest
    fun setup() {
        repository = PrivacyInfoRepositoryImpl(
            userSessionRepository = mockUserSessionRepository,
            dataSource = mockDataSource
        )

        every { mockUserSession.getRustUserSession() } returns mockRustSession
    }

    @Test
    fun `observe trackers returns flow of blocked privacy items successfully`() = runTest {
        // Given
        val trackerDomains = listOf(
            TrackerDomain("tracker1.com", listOf("https://tracker1.com/pixel")),
            TrackerDomain("tracker2.com", listOf("https://tracker2.com/pixel"))
        )
        val strippedUtmLinks = listOf(
            UtmLink("https://example.com?utm_source=test", "https://example.com"),
            UtmLink("https://other.com?utm_campaign=promo", "https://other.com")
        )

        val expectedTrackers = listOf(
            BlockedTracker("tracker1.com", listOf("https://tracker1.com/pixel")),
            BlockedTracker("tracker2.com", listOf("https://tracker2.com/pixel"))
        )
        val expectedCleanedLinks = listOf(
            CleanedLink("https://example.com?utm_source=test", "https://example.com"),
            CleanedLink("https://other.com?utm_campaign=promo", "https://other.com")
        )

        val trackerInfo = mockk<TrackerInfo> {
            every { trackers } returns trackerDomains
        }
        val strippedUtmInfo = mockk<StrippedUtmInfo> {
            every { links } returns strippedUtmLinks
        }
        val privacyInfoWrapper = PrivacyInfoWrapper(trackerInfo, strippedUtmInfo)

        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Detected(privacyInfoWrapper).right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Detected)
            assertEquals(expectedTrackers, result.items.trackers)
            assertEquals(expectedCleanedLinks, result.items.urls)
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns empty lists when no trackers or links found`() = runTest {
        // Given
        val trackerInfo = mockk<TrackerInfo> {
            every { trackers } returns emptyList()
        }
        val strippedUtmInfo = mockk<StrippedUtmInfo> {
            every { links } returns emptyList()
        }
        val privacyInfoWrapper = PrivacyInfoWrapper(trackerInfo, strippedUtmInfo)

        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Detected(privacyInfoWrapper).right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Detected)
            assertTrue(result.items.trackers.isEmpty())
            assertTrue(result.items.urls.isEmpty())
            assertTrue(result.items.isEmpty)
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns flow of error when user session is null`() = runTest {
        // Given
        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns null

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem()

            // Then
            assertTrue(result.isLeft())
            assertEquals(DataError.Local.NoUserSession, result.swap().getOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers propagates data source error`() = runTest {
        // Given
        val expectedError = DataError.Remote.NoNetwork
        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(expectedError.left())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem()

            // Then
            assertTrue(result.isLeft())
            assertEquals(expectedError, result.swap().getOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns only trackers when no cleaned links`() = runTest {
        // Given
        val trackerDomains = listOf(
            TrackerDomain("tracker.com", listOf("https://tracker.com/pixel"))
        )
        val expectedTrackers = listOf(
            BlockedTracker("tracker.com", listOf("https://tracker.com/pixel"))
        )

        val trackerInfo = mockk<TrackerInfo> {
            every { trackers } returns trackerDomains
        }
        val strippedUtmInfo = mockk<StrippedUtmInfo> {
            every { links } returns emptyList()
        }
        val privacyInfoWrapper = PrivacyInfoWrapper(trackerInfo, strippedUtmInfo)

        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Detected(privacyInfoWrapper).right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Detected)
            assertEquals(expectedTrackers, result.items.trackers)
            assertTrue(result.items.urls.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns only cleaned links when no trackers`() = runTest {
        // Given
        val strippedUtmLinks = listOf(
            UtmLink("https://example.com?utm_source=test", "https://example.com")
        )
        val expectedCleanedLinks = listOf(
            CleanedLink("https://example.com?utm_source=test", "https://example.com")
        )

        val trackerInfo = mockk<TrackerInfo> {
            every { trackers } returns emptyList()
        }
        val strippedUtmInfo = mockk<StrippedUtmInfo> {
            every { links } returns strippedUtmLinks
        }
        val privacyInfoWrapper = PrivacyInfoWrapper(trackerInfo, strippedUtmInfo)

        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Detected(privacyInfoWrapper).right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Detected)
            assertTrue(result.items.trackers.isEmpty())
            assertEquals(expectedCleanedLinks, result.items.urls)
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns Pending when data source emits Pending`() = runTest {
        // Given
        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Pending.right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Pending)
            awaitComplete()
        }
    }

    @Test
    fun `observe trackers returns Disabled when data source emits Disabled`() = runTest {
        // Given
        coEvery { mockUserSessionRepository.getUserSession(testUserId) } returns mockUserSession
        every {
            mockDataSource.observePrivacyInfo(any(), any())
        } returns flowOf(PrivacyInfoState.Disabled.right())

        // When/Then
        repository.observePrivacyItemsForMessage(testUserId, testMessageId).test {
            val result = awaitItem().getOrNull()

            // Then
            assertNotNull(result)
            assertTrue(result is PrivacyItemsResult.Disabled)
            awaitComplete()
        }
    }
}
