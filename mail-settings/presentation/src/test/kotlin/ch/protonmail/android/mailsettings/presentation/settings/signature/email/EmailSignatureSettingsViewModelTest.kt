/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.signature.email

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsession.domain.model.CookieSessionId
import ch.protonmail.android.mailsession.domain.model.Fork
import ch.protonmail.android.mailsession.domain.model.Selector
import ch.protonmail.android.mailsession.domain.model.SessionError
import ch.protonmail.android.mailsession.domain.usecase.ForkSession
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.WebSettingsConfig
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.usecase.HandleCloseWebSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveWebSettingsConfig
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsState
import ch.protonmail.android.mailsettings.presentation.websettings.model.WebSettingsAction
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailSignatureSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fork = Fork(Selector("test-selector-id"), CookieSessionId("test-session-id"))
    private val testTheme = Theme.DARK
    private val testWebSettingsConfig = WebSettingsConfig(
        baseUrl = "https://test.com",
        accountSettingsAction = "account-settings",
        emailSettingsAction = "email-settings",
        labelSettingsAction = "label-settings",
        spamFilterSettingsAction = "spam-settings",
        privacySecuritySettingsAction = "privacy-settings",
        subscriptionDetailsAction = "subscription",
        emailSignatureAction = "email-signature"
    )

    private val primaryUserId = UserIdTestData.Primary

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val forkSession = mockk<ForkSession> {
        coEvery { this@mockk(primaryUserId) } returns fork.right()
    }
    private val appSettingsRepository = mockk<AppSettingsRepository>()
    private val observeWebSettingsConfig = mockk<ObserveWebSettingsConfig> {
        every { this@mockk.invoke() } returns flowOf(testWebSettingsConfig)
    }
    private val handleCloseWebSettings = mockk<HandleCloseWebSettings> {
        coEvery { this@mockk() } just Runs
    }

    private fun buildViewModel() = EmailSignatureSettingsViewModel(
        observePrimaryUserId = observePrimaryUserId,
        forkSession = forkSession,
        appSettingsRepository = appSettingsRepository,
        observeWebSettingsConfig = observeWebSettingsConfig,
        handleCloseWebSettings = handleCloseWebSettings
    )

    @Test
    fun `emits loading state when initialized`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf(null)
        every { appSettingsRepository.observeTheme() } returns flowOf(testTheme)
        val viewModel = buildViewModel()

        // When & Then
        viewModel.state.test {
            assertEquals(WebSettingsState.Loading, awaitItem())
        }
    }

    @Test
    fun `emits Data state when valid data is provided`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf(primaryUserId)
        every { appSettingsRepository.observeTheme() } returns flowOf(testTheme)
        val viewModel = buildViewModel()

        // When
        viewModel.state.test {

            // Then
            val actualState = awaitItem() as WebSettingsState.Data
            assertEquals(testTheme, actualState.theme)
        }
    }

    @Test
    fun `emits Error state when user session fork fails`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf(primaryUserId)
        every { appSettingsRepository.observeTheme() } returns flowOf(testTheme)
        coEvery {
            forkSession(primaryUserId)
        } returns SessionError.Local.KeyChainError.left()
        val viewModel = buildViewModel()

        // When
        viewModel.state.test {

            // Then
            assertTrue(awaitItem() is WebSettingsState.Error)
        }
    }

    @Test
    fun `Calls handleCloseWebSettings use case when settings page is closed`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf(primaryUserId)
        every { appSettingsRepository.observeTheme() } returns flowOf(testTheme)
        val viewModel = buildViewModel()

        // When
        viewModel.submit(WebSettingsAction.OnCloseWebSettings)
        viewModel.state.test {

            // Then
            val actualState = awaitItem() as WebSettingsState.Data
            assertEquals(testTheme, actualState.theme)
            coVerify(exactly = 1) { handleCloseWebSettings() }
        }
    }
}
