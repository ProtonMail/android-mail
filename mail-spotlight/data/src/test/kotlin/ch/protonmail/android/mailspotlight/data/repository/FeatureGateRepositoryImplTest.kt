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

package ch.protonmail.android.mailspotlight.data.repository

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.ByoeGuidanceTreatment
import uniffi.mail_uniffi.Feature
import uniffi.mail_uniffi.FeatureGate
import uniffi.mail_uniffi.MailUserSession
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.mail_uniffi.Announcement as SdkAnnouncement
import uniffi.mail_uniffi.ByoeEntryPoint as SdkByoeEntryPoint

internal class FeatureGateRepositoryImplTest {

    private val userId = UserId("user")
    private val sidebarBanner = FeatureEntryPoint.Byoe(ByoeEntryPoint.SidebarBanner)
    private val sdkSidebarBanner = Feature.Byoe(SdkByoeEntryPoint.SIDEBAR_BANNER)

    private val featureGate = mockk<FeatureGate> {
        every { close() } just runs
    }
    private val rustSession = mockk<MailUserSession> {
        every { featureGate() } returns featureGate
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns mockk<MailUserSessionWrapper> {
            every { getRustUserSession() } returns rustSession
        }
    }

    private val repository = FeatureGateRepositoryImpl(
        ExecuteWithUserSession(userSessionRepository, UnconfinedTestDispatcher())
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the BYOE spotlight is next, when getting the next announcement, then it is mapped`() = runTest {
        // Given
        coEvery { featureGate.nextAnnouncement() } returns
            SdkAnnouncement.ByoeSpotlight(ByoeGuidanceTreatment.TREATMENT2)

        // When
        val result = repository.getNextAnnouncement(userId)

        // Then
        assertEquals(Announcement.ByoeSpotlight(ByoeTreatment.Treatment2).right(), result)
        verify(exactly = 1) { featureGate.close() }
    }

    @Test
    fun `given no announcement, when getting the next announcement, then it is null`() = runTest {
        // Given
        coEvery { featureGate.nextAnnouncement() } returns null

        // When
        val result = repository.getNextAnnouncement(userId)

        // Then
        assertEquals(null.right(), result)
    }

    @Test
    fun `given no user session, when getting the next announcement, then it fails with no user session`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = repository.getNextAnnouncement(userId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `given the SDK throws, when checking an entry point, then it fails with an unknown error`() = runTest {
        // Given
        coEvery { featureGate.isEnabled(sdkSidebarBanner) } throws IllegalStateException("boom")

        // When
        val result = repository.isEntryPointEnabled(userId, sidebarBanner)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `when marking the content search What's New shown and resolved, then the SDK is told`() = runTest {
        // Given
        coEvery { featureGate.markAnnouncementShown(SdkAnnouncement.ContentSearchWhatsNew) } just runs
        coEvery { featureGate.markAnnouncementResolved(SdkAnnouncement.ContentSearchWhatsNew) } just runs

        // When
        repository.markAnnouncementShown(userId, Announcement.ContentSearchWhatsNew)
        repository.markAnnouncementResolved(userId, Announcement.ContentSearchWhatsNew)

        // Then
        coVerify { featureGate.markAnnouncementShown(SdkAnnouncement.ContentSearchWhatsNew) }
        coVerify { featureGate.markAnnouncementResolved(SdkAnnouncement.ContentSearchWhatsNew) }
    }

    @Test
    fun `when migrating, then the seen announcements are passed to the SDK`() = runTest {
        // Given
        coEvery { featureGate.migrateClientState(listOf(SdkAnnouncement.ContentSearchWhatsNew)) } just runs

        // When
        val result = repository.migrateClientState(userId, listOf(Announcement.ContentSearchWhatsNew))

        // Then
        assertEquals(Unit.right(), result)
        coVerify { featureGate.migrateClientState(listOf(SdkAnnouncement.ContentSearchWhatsNew)) }
    }

    @Test
    fun `given a kept entry point, when an announcement is shown, then the entry point is asked again`() = runTest {
        // Given
        val announcement = SdkAnnouncement.ByoeSpotlight(ByoeGuidanceTreatment.TREATMENT1)
        coEvery { featureGate.isEnabled(sdkSidebarBanner) } returnsMany listOf(false, true)
        coEvery { featureGate.markAnnouncementShown(announcement) } just runs

        repository.observeEntryPointEnabled(userId, sidebarBanner).test {
            assertEquals(false, awaitItem())

            // When
            repository.markAnnouncementShown(userId, Announcement.ByoeSpotlight(ByoeTreatment.Treatment1))

            // Then
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `given a kept entry point, when a BYOE deep link is opened, then the entry point is asked again`() = runTest {
        // Given
        coEvery { featureGate.isEnabled(sdkSidebarBanner) } returnsMany listOf(false, true)
        coEvery { featureGate.markByoeDeepLinkOpened() } just runs

        repository.observeEntryPointEnabled(userId, sidebarBanner).test {
            assertEquals(false, awaitItem())

            // When
            repository.markByoeDeepLinkOpened(userId)

            // Then
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `given the read fails, when observing an entry point, then it is hidden`() = runTest {
        // Given
        coEvery { featureGate.isEnabled(sdkSidebarBanner) } throws IllegalStateException("boom")

        // When
        repository.observeEntryPointEnabled(userId, sidebarBanner).test {
            // Then
            assertEquals(false, awaitItem())
        }
    }
}
