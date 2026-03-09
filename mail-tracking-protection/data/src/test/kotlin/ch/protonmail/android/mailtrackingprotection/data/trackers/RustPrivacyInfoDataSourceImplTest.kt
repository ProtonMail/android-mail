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
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailtrackingprotection.data.wrapper.PrivacyInfoState
import ch.protonmail.android.mailtrackingprotection.data.wrapper.PrivacyInfoStreamWrapper
import ch.protonmail.android.mailtrackingprotection.data.wrapper.RustPrivacyInfoWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.PrivacyInfo
import uniffi.mail_uniffi.StrippedUtmInfo
import uniffi.mail_uniffi.TrackerDomain
import uniffi.mail_uniffi.TrackerInfo
import uniffi.mail_uniffi.TrackerInfoWithStatus
import uniffi.mail_uniffi.UtmLink
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class RustPrivacyInfoDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: RustPrivacyInfoDataSource

    private val mockWrapper = mockk<RustPrivacyInfoWrapper>()
    private val mockStream = mockk<PrivacyInfoStreamWrapper>(relaxed = true)

    private val testMessageId: LocalMessageId = Id(123UL)
    private val testTrackerDomain = TrackerDomain("tracker.example.com", listOf("https://tracker.example.com/pixel"))
    private val testUtmLink = UtmLink("https://example.com?utm_source=test", "https://example.com")

    @BeforeTest
    fun setup() {
        dataSource = RustPrivacyInfoDataSourceImpl()
    }

    @Test
    fun `observe privacy info returns Detected state with initial info`() = runTest {
        // Given
        val trackerInfo = TrackerInfo(listOf(testTrackerDomain), 0UL)
        val strippedUtmInfo = StrippedUtmInfo(listOf(testUtmLink))
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Detected(trackerInfo)
            every { utmLinks } returns strippedUtmInfo
        }

        coEvery { mockWrapper.watchTrackerInfoStream(testMessageId) } returns mockStream.right()
        every { mockStream.initialInfo() } returns privacyInfo

        // When
        dataSource.observePrivacyInfo(mockWrapper, testMessageId).test {
            // Then
            val result = awaitItem().getOrNull()
            assertTrue(result is PrivacyInfoState.Detected)
            assertEquals(listOf(testTrackerDomain), result.info.trackerInfo.trackers)
            assertEquals(listOf(testUtmLink), result.info.strippedUtmInfo.links)
            awaitComplete()
        }
    }

    @Test
    fun `observe privacy info returns Pending when trackers is Detected but utm links is null`() = runTest {
        // Given
        val trackerInfo = TrackerInfo(listOf(testTrackerDomain), 0UL)
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Detected(trackerInfo)
            every { utmLinks } returns null
        }

        coEvery { mockWrapper.watchTrackerInfoStream(testMessageId) } returns mockStream.right()
        every { mockStream.initialInfo() } returns privacyInfo

        // When
        dataSource.observePrivacyInfo(mockWrapper, testMessageId).test {
            // Then
            val result = awaitItem().getOrNull()
            assertNotNull(result)
            assertTrue(result is PrivacyInfoState.Pending)
            awaitComplete()
        }
    }

    @Test
    fun `observe privacy info returns error when watch stream fails`() = runTest {
        // Given
        val expected = DataError.Local.NoUserSession
        coEvery { mockWrapper.watchTrackerInfoStream(testMessageId) } returns expected.left()

        // When
        dataSource.observePrivacyInfo(mockWrapper, testMessageId).test {
            // Then
            val result = awaitItem()
            assertTrue(result.isLeft())
            assertEquals(expected, result.swap().getOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `observe privacy info returns Pending when trackers status is Pending`() = runTest {
        // Given
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Pending
            every { utmLinks } returns StrippedUtmInfo(listOf(testUtmLink))
        }

        coEvery { mockWrapper.watchTrackerInfoStream(testMessageId) } returns mockStream.right()
        every { mockStream.initialInfo() } returns privacyInfo

        // When
        dataSource.observePrivacyInfo(mockWrapper, testMessageId).test {
            // Then
            val result = awaitItem().getOrNull()
            assertNotNull(result)
            assertTrue(result is PrivacyInfoState.Pending)
            awaitComplete()
        }
    }

    @Test
    fun `observe privacy info returns Disabled when trackers status is Disabled`() = runTest {
        // Given
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Disabled
            every { utmLinks } returns StrippedUtmInfo(listOf(testUtmLink))
        }

        coEvery { mockWrapper.watchTrackerInfoStream(testMessageId) } returns mockStream.right()
        every { mockStream.initialInfo() } returns privacyInfo

        // When
        dataSource.observePrivacyInfo(mockWrapper, testMessageId).test {
            // Then
            val result = awaitItem().getOrNull()
            assertNotNull(result)
            assertTrue(result is PrivacyInfoState.Disabled)
            awaitComplete()
        }
    }
}
