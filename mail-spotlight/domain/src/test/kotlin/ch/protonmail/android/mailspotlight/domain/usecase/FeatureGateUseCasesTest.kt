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

package ch.protonmail.android.mailspotlight.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import ch.protonmail.android.mailspotlight.domain.repository.FeatureGateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FeatureGateUseCasesTest {

    private val userId = UserId("user")
    private val settings = FeatureEntryPoint.Byoe(ByoeEntryPoint.Settings)
    private val repository = mockk<FeatureGateRepository>()

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an announcement, when getting the next one, then it is returned`() = runTest {
        // Given
        val announcement = Announcement.ByoeSpotlight(ByoeTreatment.Treatment1)
        coEvery { repository.getNextAnnouncement(userId) } returns announcement.right()

        // When
        val result = GetNextAnnouncement(repository)(userId)

        // Then
        assertEquals(announcement, result)
    }

    @Test
    fun `given the read fails, when getting the next announcement, then nothing shows`() = runTest {
        // Given
        coEvery { repository.getNextAnnouncement(userId) } returns DataError.Local.Unknown.left()

        // When
        val result = GetNextAnnouncement(repository)(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `given the entry point is enabled, when checking it, then it shows`() = runTest {
        // Given
        coEvery { repository.isEntryPointEnabled(userId, settings) } returns true.right()

        // When
        val result = IsFeatureEntryPointEnabled(repository)(userId, settings)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given the read fails, when checking an entry point, then it is hidden`() = runTest {
        // Given
        coEvery { repository.isEntryPointEnabled(userId, settings) } returns DataError.Local.Unknown.left()

        // When
        val result = IsFeatureEntryPointEnabled(repository)(userId, settings)

        // Then
        assertFalse(result)
    }

    @Test
    fun `when marking an announcement shown and resolved, then the repository records both`() = runTest {
        // Given
        val announcement = Announcement.ContentSearchWhatsNew
        coEvery { repository.markAnnouncementShown(userId, announcement) } returns Unit.right()
        coEvery { repository.markAnnouncementResolved(userId, announcement) } returns DataError.Local.Unknown.left()

        // When
        MarkAnnouncementShown(repository)(userId, announcement)
        MarkAnnouncementResolved(repository)(userId, announcement)

        // Then
        coVerify { repository.markAnnouncementShown(userId, announcement) }
        coVerify { repository.markAnnouncementResolved(userId, announcement) }
    }

    @Test
    fun `when marking the BYOE deep link opened, then the repository records it`() = runTest {
        // Given
        coEvery { repository.markByoeDeepLinkOpened(userId) } returns Unit.right()

        // When
        MarkByoeDeepLinkOpened(repository)(userId)

        // Then
        coVerify { repository.markByoeDeepLinkOpened(userId) }
    }
}
