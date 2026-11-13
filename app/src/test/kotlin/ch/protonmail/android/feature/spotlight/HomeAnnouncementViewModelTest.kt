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

package ch.protonmail.android.feature.spotlight

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.AnnouncementHold
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.usecase.GetNextAnnouncement
import ch.protonmail.android.mailspotlight.domain.usecase.MarkAnnouncementResolved
import ch.protonmail.android.mailspotlight.domain.usecase.MarkAnnouncementShown
import ch.protonmail.android.mailspotlight.domain.usecase.MigrateAnnouncementState
import ch.protonmail.android.mailspotlight.domain.usecase.RefreshFeatureEntryPoints
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeAnnouncementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = UserId("user")
    private val spotlight = Announcement.ByoeSpotlight(ByoeTreatment.Treatment2)

    private val primaryUserId = MutableStateFlow<UserId?>(userId)
    private val isInBackground = MutableStateFlow(false)

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns primaryUserId
    }
    private val appInBackgroundState = mockk<AppInBackgroundState> {
        every { observe() } returns isInBackground
    }
    private val migrateAnnouncementState = mockk<MigrateAnnouncementState> {
        coEvery { this@mockk(any()) } just runs
    }
    private val getNextAnnouncement = mockk<GetNextAnnouncement> {
        coEvery { this@mockk(any()) } returns spotlight
    }
    private val markAnnouncementShown = mockk<MarkAnnouncementShown> {
        coEvery { this@mockk(any(), any()) } just runs
    }
    private val markAnnouncementResolved = mockk<MarkAnnouncementResolved> {
        coEvery { this@mockk(any(), any()) } just runs
    }
    private val refreshFeatureEntryPoints = mockk<RefreshFeatureEntryPoints> {
        coEvery { this@mockk(any()) } just runs
    }
    private val dismissContentSearchPrompt = mockk<DismissContentSearchPrompt> {
        coEvery { this@mockk(any(), any()) } returns Unit.right()
    }
    private val dismissByoeGuidance = mockk<DismissByoeGuidance> {
        coEvery { this@mockk(any(), any(), any()) } just runs
    }

    private val announcementHold = AnnouncementHold()

    private fun viewModel() = HomeAnnouncementViewModel(
        observePrimaryUserId,
        appInBackgroundState,
        announcementHold,
        migrateAnnouncementState,
        getNextAnnouncement,
        markAnnouncementShown,
        markAnnouncementResolved,
        refreshFeatureEntryPoints,
        dismissContentSearchPrompt,
        dismissByoeGuidance,
        HomeAnnouncementReducer()
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given Home is not ready, when the app is in the foreground, then the SDK is not asked`() = runTest {
        // When
        val viewModel = viewModel()
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = false))

        // Then
        coVerify(exactly = 0) { getNextAnnouncement(any()) }
    }

    @Test
    fun `given Home is ready, when the app is in the foreground, then it migrates and then asks`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // Then
        coVerifyOrder {
            migrateAnnouncementState(userId)
            getNextAnnouncement(userId)
        }
        assertEquals(Effect.of<Announcement>(spotlight), viewModel.state.value.navigateTo)
    }

    @Test
    fun `given a BYOE deep link is being handled, when Home is ready, then the SDK is asked once it is done`() =
        runTest {
            // Given
            val deepLinkHandled = CompletableDeferred<Unit>()
            val viewModel = viewModel()
            val deepLink = launch { announcementHold.hold { deepLinkHandled.await() } }
            testScheduler.runCurrent()

            // When
            viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

            // Then
            coVerify(exactly = 0) { getNextAnnouncement(any()) }

            // When
            deepLinkHandled.complete(Unit)
            deepLink.join()

            // Then
            coVerify(exactly = 1) { getNextAnnouncement(userId) }
        }

    @Test
    fun `given an announcement popped before it appeared, when the mailbox is back, then the SDK is asked again`() =
        runTest {
            // Given
            val viewModel = viewModel()
            viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))
            viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = false))
            coEvery { getNextAnnouncement(userId) } returns null

            // When
            viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

            // Then
            coVerify(exactly = 2) { getNextAnnouncement(userId) }
            assertEquals(null, viewModel.state.value.pending)
        }

    @Test
    fun `given it was asked, when the app comes back to the foreground, then it asks again`() = runTest {
        // Given
        coEvery { getNextAnnouncement(userId) } returns null
        val viewModel = viewModel()
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // When
        isInBackground.value = true
        isInBackground.value = false

        // Then
        coVerify(exactly = 2) { getNextAnnouncement(userId) }
    }

    @Test
    fun `given it was asked, when another account becomes primary, then it asks for that account`() = runTest {
        // Given
        val otherUserId = UserId("other")
        coEvery { getNextAnnouncement(any()) } returns null
        val viewModel = viewModel()
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // When
        primaryUserId.value = otherUserId

        // Then
        coVerify { getNextAnnouncement(otherUserId) }
        coVerify { refreshFeatureEntryPoints(otherUserId) }
    }

    @Test
    fun `given no announcement, when asking, then Home does not navigate`() = runTest {
        // Given
        coEvery { getNextAnnouncement(userId) } returns null
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // Then
        assertEquals(Effect.empty(), viewModel.state.value.navigateTo)
    }

    @Test
    fun `given an announcement was navigated to, when it has not appeared yet, then it is not recorded as shown`() =
        runTest {
            // When
            viewModel().submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

            // Then
            coVerify(exactly = 0) { markAnnouncementShown(any(), any()) }
        }

    @Test
    fun `when the announcement appears, then it is recorded as shown`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))

        // When
        viewModel.submit(HomeAnnouncementAction.Shown(spotlight))

        // Then
        coVerify(exactly = 1) { markAnnouncementShown(userId, spotlight) }
    }

    @Test
    fun `when the content search What's New is answered, then the offer is dismissed and it is resolved`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.ContentSearchAnswered)

        // Then
        coVerify { dismissContentSearchPrompt(userId, ContentSearchPrompt.SetUpOffer) }
        coVerify { markAnnouncementResolved(userId, Announcement.ContentSearchWhatsNew) }
    }

    @Test
    fun `when the BYOE spotlight is dismissed, then the dismissal is recorded and it is resolved`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.ByoeSpotlightDismissed(spotlight, ByoeDismissMethod.SwipeDown))

        // Then
        coVerify { dismissByoeGuidance(userId, ByoeSurface.Spotlight, ByoeDismissMethod.SwipeDown) }
        coVerify { markAnnouncementResolved(userId, spotlight) }
    }

    @Test
    fun `when Connect is tapped on the BYOE spotlight, then it is resolved without a dismissal`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.ByoeSpotlightConnected(spotlight))

        // Then
        coVerify { markAnnouncementResolved(userId, spotlight) }
        coVerify(exactly = 0) { dismissByoeGuidance(any(), any(), any()) }
    }

    @Test
    fun `when the BYOE onboarding page is declined, then the onboarding dismissal is recorded`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(HomeAnnouncementAction.ByoeOnboardingPageDismissed(ByoeDismissMethod.Button))

        // Then
        coVerify { dismissByoeGuidance(userId, ByoeSurface.OnboardingScreen, ByoeDismissMethod.Button) }
        coVerify(exactly = 0) { markAnnouncementResolved(any(), any()) }
    }

    @Test
    fun `when the app comes to the foreground, then the entry points are refreshed`() = runTest {
        // When
        viewModel().state.test {
            awaitItem()

            // Then
            coVerify { refreshFeatureEntryPoints(userId) }
        }
    }

    @Test
    fun `given the announcement is on screen, when it appears again, then it is recorded as shown once`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))
        viewModel.submit(HomeAnnouncementAction.Shown(spotlight))

        // When
        viewModel.submit(HomeAnnouncementAction.Shown(spotlight))

        // Then
        coVerify(exactly = 1) { markAnnouncementShown(userId, spotlight) }
    }

    @Test
    fun `given the account changed after it appeared, when it is dismissed, then it is recorded for its own account`() =
        runTest {
            // Given
            val otherUserId = UserId("other")
            val viewModel = viewModel()
            viewModel.submit(HomeAnnouncementAction.HomeReadinessChanged(isReady = true))
            viewModel.submit(HomeAnnouncementAction.Shown(spotlight))
            primaryUserId.value = otherUserId

            // When
            viewModel.submit(HomeAnnouncementAction.ByoeSpotlightDismissed(spotlight, ByoeDismissMethod.Button))

            // Then
            coVerify { dismissByoeGuidance(userId, ByoeSurface.Spotlight, ByoeDismissMethod.Button) }
            coVerify { markAnnouncementResolved(userId, spotlight) }
            coVerify(exactly = 0) { markAnnouncementResolved(otherUserId, any()) }
        }
}
