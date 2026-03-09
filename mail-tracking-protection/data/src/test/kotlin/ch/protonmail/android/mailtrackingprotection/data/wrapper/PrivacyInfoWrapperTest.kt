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

package ch.protonmail.android.mailtrackingprotection.data.wrapper

import io.mockk.every
import io.mockk.mockk
import uniffi.mail_uniffi.PrivacyInfo
import uniffi.mail_uniffi.StrippedUtmInfo
import uniffi.mail_uniffi.TrackerDomain
import uniffi.mail_uniffi.TrackerInfo
import uniffi.mail_uniffi.TrackerInfoWithStatus
import uniffi.mail_uniffi.UtmLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PrivacyInfoWrapperTest {

    @Test
    fun `should return Pending when trackers status is Pending`() {
        // Given
        val links = StrippedUtmInfo(listOf())
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Pending
            every { utmLinks } returns links
        }

        // When
        val result = PrivacyInfoWrapper.fromPrivacyInfo(privacyInfo)

        // Then
        assertTrue(result is PrivacyInfoState.Pending)
    }

    @Test
    fun `should return Disabled when trackers status is Disabled`() {
        // Given
        val links = StrippedUtmInfo(listOf())
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Disabled
            every { utmLinks } returns links
        }

        // When
        val result = PrivacyInfoWrapper.fromPrivacyInfo(privacyInfo)

        // Then
        assertTrue(result is PrivacyInfoState.Disabled)
    }

    @Test
    fun `should return Pending when trackers is Detected but links is null`() {
        // Given
        val trackerInfo = TrackerInfo(listOf(), 0UL)
        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Detected(trackerInfo)
            every { utmLinks } returns null
        }

        // When
        val result = PrivacyInfoWrapper.fromPrivacyInfo(privacyInfo)

        // Then
        assertTrue(result is PrivacyInfoState.Pending)
    }

    @Test
    fun `should return Detected with proper wrapped value`() {
        // Given
        val trackerInfo = TrackerInfo(
            trackers = listOf(
                TrackerDomain("example.com", listOf("https://example.com"))
            ),
            lastCheckedAt = 0UL
        )

        val links = StrippedUtmInfo(
            links = listOf(UtmLink("example.com/?utm=123456", "example.com"))
        )

        val privacyInfo = mockk<PrivacyInfo> {
            every { trackers } returns TrackerInfoWithStatus.Detected(trackerInfo)
            every { utmLinks } returns links
        }

        // When
        val result = PrivacyInfoWrapper.fromPrivacyInfo(privacyInfo)

        // Then
        assertTrue(result is PrivacyInfoState.Detected)
        assertEquals(trackerInfo, result.info.trackerInfo)
        assertEquals(links, result.info.strippedUtmInfo)
    }
}
