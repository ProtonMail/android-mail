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

package ch.protonmail.android.mailbyoe.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeOnboardingStep
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
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
import uniffi.mail_uniffi.ByoeDismissMethod
import uniffi.mail_uniffi.ByoeGuidance
import uniffi.mail_uniffi.ByoeGuidanceTreatment
import uniffi.mail_uniffi.ByoeSettingsAccess
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.OnboardingStep
import uniffi.mail_uniffi.ScreenVisibilityStore
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod as DismissMethod

internal class ByoeGuidanceRepositoryImplTest {

    private val userId = UserId("user")
    private val byoeGuidance = mockk<ByoeGuidance> {
        every { close() } just runs
    }
    private val screenVisibilityStore = mockk<ScreenVisibilityStore> {
        every { close() } just runs
    }
    private val rustSession = mockk<MailUserSession> {
        every { byoeGuidance() } returns byoeGuidance
        every { screenVisibilityStore() } returns screenVisibilityStore
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns mockk<MailUserSessionWrapper> {
            every { getRustUserSession() } returns rustSession
        }
    }

    private val repository = ByoeGuidanceRepositoryImpl(
        ExecuteWithUserSession(userSessionRepository, UnconfinedTestDispatcher())
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given full settings access, when getting it, then it is mapped and the guidance is released`() = runTest {
        // Given
        coEvery { byoeGuidance.settings() } returns ByoeSettingsAccess.FULL

        // When
        val result = repository.getSettingsAccess(userId)

        // Then
        assertEquals(GmailConnectionSettingsAccess.Full.right(), result)
        verify(exactly = 1) { byoeGuidance.close() }
    }

    @Test
    fun `given list only settings access, when getting it, then it is mapped`() = runTest {
        // Given
        coEvery { byoeGuidance.settings() } returns ByoeSettingsAccess.LIST_ONLY

        // When
        val result = repository.getSettingsAccess(userId)

        // Then
        assertEquals(GmailConnectionSettingsAccess.ListOnly.right(), result)
    }

    @Test
    fun `given an onboarding treatment, when getting it, then it is mapped`() = runTest {
        // Given
        coEvery { byoeGuidance.onboarding() } returns ByoeGuidanceTreatment.TREATMENT2

        // When
        val result = repository.getOnboardingTreatment(userId)

        // Then
        assertEquals(ByoeTreatment.Treatment2.right(), result)
    }

    @Test
    fun `given no spotlight treatment, when getting it, then there is none`() = runTest {
        // Given
        coEvery { byoeGuidance.spotlight() } returns null

        // When
        val result = repository.getSpotlightTreatment(userId)

        // Then
        assertEquals(null.right(), result)
    }

    @Test
    fun `given no user session, when getting the settings access, then an error is returned`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = repository.getSettingsAccess(userId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `given the sidebar banner shows, when asking, then its treatment is returned`() = runTest {
        // Given
        coEvery { byoeGuidance.sidebarBanner() } returns ByoeGuidanceTreatment.TREATMENT2

        // When
        val result = repository.getSidebarBannerTreatment(userId)

        // Then
        assertEquals(ByoeTreatment.Treatment2.right(), result)
    }

    @Test
    fun `given the sidebar banner is hidden, when asking, then there is no treatment`() = runTest {
        // Given
        coEvery { byoeGuidance.sidebarBanner() } returns null

        // When
        val result = repository.getSidebarBannerTreatment(userId)

        // Then
        assertEquals(null.right(), result)
    }

    @Test
    fun `given the sidebar banner is closed, when recording it, then the guidance records it`() = runTest {
        // Given
        coEvery { byoeGuidance.markSidebarBannerClosed(ByoeDismissMethod.BUTTON) } just runs

        // When
        val result = repository.markSidebarBannerClosed(userId, DismissMethod.Button)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { byoeGuidance.markSidebarBannerClosed(ByoeDismissMethod.BUTTON) }
    }

    @Test
    fun `given the spotlight appeared, when recording it, then the guidance records it`() = runTest {
        // Given
        coEvery { byoeGuidance.markSpotlightShown() } just runs

        // When
        val result = repository.markSpotlightShown(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { byoeGuidance.markSpotlightShown() }
    }

    @Test
    fun `given the spotlight is swiped away, when recording it, then the guidance records the swipe`() = runTest {
        // Given
        coEvery { byoeGuidance.markSpotlightDismissed(ByoeDismissMethod.SWIPE_DOWN) } just runs

        // When
        val result = repository.markGuidanceDismissed(userId, ByoeSurface.Spotlight, DismissMethod.SwipeDown)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { byoeGuidance.markSpotlightDismissed(ByoeDismissMethod.SWIPE_DOWN) }
    }

    @Test
    fun `given the onboarding page is dismissed, when recording it, then the guidance records it`() = runTest {
        // Given
        coEvery { byoeGuidance.markOnboardingDismissed(ByoeDismissMethod.BUTTON) } just runs

        // When
        val result = repository.markGuidanceDismissed(userId, ByoeSurface.OnboardingScreen, DismissMethod.Button)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { byoeGuidance.markOnboardingDismissed(ByoeDismissMethod.BUTTON) }
    }

    @Test
    fun `given the onboarding is not recorded yet, when asking, then it is not recorded`() = runTest {
        // Given
        coEvery { screenVisibilityStore.isOnboardingStepShown(OnboardingStep.FINISHED) } returns false

        // When
        val result = repository.isOnboardingFinishedRecorded(userId)

        // Then
        assertEquals(false.right(), result)
    }

    @Test
    fun `given the onboarding is finished, when recording it, then the finished step is marked`() = runTest {
        // Given
        coEvery { screenVisibilityStore.markOnboardingStep(OnboardingStep.FINISHED) } just runs

        // When
        val result = repository.recordOnboardingFinished(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.FINISHED) }
    }

    @Test
    fun `given the onboarding page appeared, when recording it, then the BYOE step is recorded`() = runTest {
        // Given
        coEvery { screenVisibilityStore.markOnboardingStep(OnboardingStep.BYOE) } just runs

        // When
        val result = repository.recordOnboardingStep(userId, ByoeOnboardingStep.Byoe)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.BYOE) }
    }

    @Test
    fun `given each onboarding page, when recording it, then its step is recorded`() = runTest {
        // Given
        coEvery { screenVisibilityStore.markOnboardingStep(any()) } just runs

        // When
        ByoeOnboardingStep.entries.forEach { repository.recordOnboardingStep(userId, it) }

        // Then
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.UPSELL) }
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.WELCOME) }
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.OFFLINE) }
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.BYOE) }
        coVerify(exactly = 1) { screenVisibilityStore.markOnboardingStep(OnboardingStep.NOTIFICATIONS) }
    }

    @Test
    fun `given no user session, when reporting an event, then an error is returned`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = repository.report(userId, ByoeTelemetryEvent.InboxViewed)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }
}
