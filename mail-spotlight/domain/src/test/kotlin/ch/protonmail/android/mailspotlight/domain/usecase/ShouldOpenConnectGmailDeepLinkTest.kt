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

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.usecase.GetGmailConnectionSettingsAccess
import ch.protonmail.android.mailsession.domain.usecase.RefreshFeatureFlags
import ch.protonmail.android.mailspotlight.domain.AnnouncementHold
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ShouldOpenConnectGmailDeepLinkTest {

    private val userId = UserId("user")
    private val settings = FeatureEntryPoint.Byoe(ByoeEntryPoint.Settings)

    private val refreshFeatureFlags = mockk<RefreshFeatureFlags> {
        coEvery { this@mockk(userId) } just runs
    }
    private val markByoeDeepLinkOpened = mockk<MarkByoeDeepLinkOpened> {
        coEvery { this@mockk(userId) } just runs
    }
    private val isFeatureEntryPointEnabled = mockk<IsFeatureEntryPointEnabled> {
        coEvery { this@mockk(userId, settings) } returns true
    }
    private val getGmailConnectionSettingsAccess = mockk<GetGmailConnectionSettingsAccess> {
        coEvery { this@mockk(userId) } returns GmailConnectionSettingsAccess.Full
    }

    private val announcementHold = AnnouncementHold()

    private val shouldOpen = ShouldOpenConnectGmailDeepLink(
        refreshFeatureFlags,
        markByoeDeepLinkOpened,
        isFeatureEntryPointEnabled,
        getGmailConnectionSettingsAccess,
        announcementHold
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given BYOE can be connected, when the link opens, then it is marked before the flags are awaited`() = runTest {
        // When
        val result = shouldOpen(flowOf(userId))

        // Then
        assertTrue(result)
        coVerifyOrder {
            markByoeDeepLinkOpened(userId)
            refreshFeatureFlags(userId)
            isFeatureEntryPointEnabled(userId, settings)
        }
    }

    @Test
    fun `when the link is being checked, then the announcements are held until it is done`() = runTest {
        // Given
        var wasHeldDuringCheck = false
        coEvery { markByoeDeepLinkOpened(userId) } coAnswers {
            wasHeldDuringCheck = announcementHold.observeIsHeld().first()
        }

        // When
        shouldOpen(flowOf(userId))

        // Then
        assertTrue(wasHeldDuringCheck)
        assertFalse(announcementHold.observeIsHeld().first())
    }

    @Test
    fun `given the BYOE settings are not enabled, when the link opens, then it is ignored`() = runTest {
        // Given
        coEvery { isFeatureEntryPointEnabled(userId, settings) } returns false

        // When
        val result = shouldOpen(flowOf(userId))

        // Then
        assertFalse(result)
    }

    @Test
    fun `given the connection killswitch leaves only the list, when the link opens, then it is ignored`() = runTest {
        // Given
        coEvery { getGmailConnectionSettingsAccess(userId) } returns GmailConnectionSettingsAccess.ListOnly

        // When
        val result = shouldOpen(flowOf(userId))

        // Then
        assertFalse(result)
    }

    @Test
    fun `given no primary account, when the link opens, then it is dropped without being recorded`() = runTest {
        // When
        val result = shouldOpen(flowOf(null))

        // Then
        assertFalse(result)
        coVerify(exactly = 0) { markByoeDeepLinkOpened(any()) }
    }
}
