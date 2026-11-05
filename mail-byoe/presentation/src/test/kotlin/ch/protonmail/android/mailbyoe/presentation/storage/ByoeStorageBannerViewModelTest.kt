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

package ch.protonmail.android.mailbyoe.presentation.storage

import app.cash.turbine.test
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerCta
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerOrigin
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.usecase.ObserveByoeImportPausedForStorage
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ByoeStorageBannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val primaryUserId = MutableStateFlow<UserId?>(userId)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns primaryUserId
    }
    private val isPaused = MutableStateFlow(true)
    private val observeByoeImportPausedForStorage = mockk<ObserveByoeImportPausedForStorage> {
        every { this@mockk(userId) } returns isPaused
        every { this@mockk(otherUserId) } returns flowOf(true)
    }
    private val upsellingVisibility = MutableStateFlow<UpsellingVisibility>(UpsellingVisibility.Normal.MailPlus)
    private val observeUpsellingVisibility = mockk<ObserveUpsellingVisibility> {
        every { this@mockk(UpsellingEntryPoint.Feature.ByoeStorage) } returns upsellingVisibility
    }

    private val reportByoeTelemetryEvent = mockk<ReportByoeTelemetryEvent>(relaxUnitFun = true)

    private fun viewModel() = ByoeStorageBannerViewModel(
        observePrimaryUserId,
        observeByoeImportPausedForStorage,
        ByoeStorageBannerDismissal(),
        reportByoeTelemetryEvent,
        observeUpsellingVisibility
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an import paused for storage, when observing, then the banner shows`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.state.test {

            // Then
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())
        }
    }

    @Test
    fun `given the banner shows, when the import resumes, then the banner hides`() = runTest {
        // Given
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())

            // When
            isPaused.value = false

            // Then
            assertEquals(ByoeStorageBannerState.Hidden, awaitItem())
        }
    }

    @Test
    fun `given the banner shows, when the user taps not now, then the banner hides`() = runTest {
        // Given
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())

            // When
            viewModel.submit(ByoeStorageBannerAction.NotNowClicked)

            // Then
            assertEquals(ByoeStorageBannerState.Hidden, awaitItem())
        }
    }

    @Test
    fun `given an import paused for storage but no upsell to offer, when observing, then the banner is hidden`() =
        runTest {
            // Given
            upsellingVisibility.value = UpsellingVisibility.Hidden
            val viewModel = viewModel()

            // When
            viewModel.state.test {

                // Then
                assertEquals(ByoeStorageBannerState.Hidden, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `given the banner shows, when there is no longer an upsell to offer, then the banner hides`() = runTest {
        // Given
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())

            // When
            upsellingVisibility.value = UpsellingVisibility.Hidden

            // Then
            assertEquals(ByoeStorageBannerState.Hidden, awaitItem())
        }
    }

    @Test
    fun `given the user tapped not now, when switching to another account, then its banner shows`() = runTest {
        // Given
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())
            viewModel.submit(ByoeStorageBannerAction.NotNowClicked)
            assertEquals(ByoeStorageBannerState.Hidden, awaitItem())

            // When
            primaryUserId.value = otherUserId

            // Then
            assertEquals(ByoeStorageBannerState.Visible, awaitItem())
        }
    }

    @Test
    fun `given the banner shows, when it scrolls into view again, then its appearance is reported once`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible = true))
        viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible = true))

        // Then
        verify(exactly = 1) { reportByoeTelemetryEvent(shown) }
    }

    @Test
    fun `given the banner hid, when it shows again, then the new appearance is reported`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible = true))
        viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible = false))

        // When
        viewModel.submit(ByoeStorageBannerAction.VisibilityChanged(isVisible = true))

        // Then
        verify(exactly = 2) { reportByoeTelemetryEvent(shown) }
    }

    @Test
    fun `given the banner shows, when the user taps upgrade, then an upgrade click is reported`() = runTest {
        // When
        viewModel().submit(ByoeStorageBannerAction.UpgradeClicked)

        // Then
        verify(exactly = 1) {
            reportByoeTelemetryEvent(
                ByoeTelemetryEvent.UpgradeBannerCtaClicked(ByoeBannerOrigin.InboxStorageLimit, ByoeBannerCta.Upgrade)
            )
        }
    }

    @Test
    fun `given the banner shows, when the user taps not now, then a not now click is reported`() = runTest {
        // When
        viewModel().submit(ByoeStorageBannerAction.NotNowClicked)

        // Then
        verify(exactly = 1) {
            reportByoeTelemetryEvent(
                ByoeTelemetryEvent.UpgradeBannerCtaClicked(ByoeBannerOrigin.InboxStorageLimit, ByoeBannerCta.NotNow)
            )
        }
    }

    private companion object {

        val shown = ByoeTelemetryEvent.UpgradeBannerShown(ByoeBannerOrigin.InboxStorageLimit)

        val userId = UserId("user")
        val otherUserId = UserId("other")
    }
}
