/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.privacy

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateAutoShowEmbeddedImagesSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateBackgroundSyncSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdatePreventScreenshotsSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateShowRemoteContentSetting
import ch.protonmail.android.mailsettings.presentation.settings.privacy.reducer.PrivacySettingsReducer
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class PrivacySettingsViewModelTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val observePrivacySettings = mockk<ObservePrivacySettings>()
    private val updateShowRemoteContentSettings = mockk<UpdateShowRemoteContentSetting>()
    private val updateAutoShowEmbeddedImagesSetting = mockk<UpdateAutoShowEmbeddedImagesSetting>()
    private val updatePreventScreenshotsSetting = mockk<UpdatePreventScreenshotsSetting>()
    private val updateLinkConfirmationSetting = mockk<UpdateLinkConfirmationSetting>()
    private val updateBackgroundSyncSetting = mockk<UpdateBackgroundSyncSetting>()
    private val privacySettingsReducer = spyk<PrivacySettingsReducer>()
    private val viewModel: PrivacySettingsViewModel by lazy {
        PrivacySettingsViewModel(
            observePrimaryUserId,
            observePrivacySettings,
            updateShowRemoteContentSettings,
            updateAutoShowEmbeddedImagesSetting,
            updateLinkConfirmationSetting,
            updatePreventScreenshotsSetting,
            updateBackgroundSyncSetting,
            privacySettingsReducer
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return loading state when first launched`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf()

        // Then
        viewModel.state.test {
            val loadingState = awaitItem()
            assertEquals(PrivacySettingsState.Loading, loadingState)
        }
    }

    @Test
    fun `should return an error when no primary user id is found`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf(null)

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(PrivacySettingsState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            privacySettingsReducer.newStateFrom(
                PrivacySettingsState.Loading,
                PrivacySettingsEvent.Error.LoadingError
            )
        }
        confirmVerified(privacySettingsReducer)
    }

    @Test
    fun `should return an error when settings cannot be fetched`() = runTest {
        // Given
        val userId = UserId("123")
        every { observePrimaryUserId.invoke() } returns flowOf(userId)
        every { observePrivacySettings.invoke(userId) } returns flowOf(DataError.Local.NoDataCached.left())

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(PrivacySettingsState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            privacySettingsReducer.newStateFrom(
                PrivacySettingsState.Loading,
                PrivacySettingsEvent.Error.LoadingError
            )
        }
        confirmVerified(privacySettingsReducer)
    }

    @Test
    fun `should load content data when userId and settings are fetched correctly`() = runTest {
        // Given
        expectValidSettingsLoaded()

        val expectedState = PrivacySettingsState.WithData(
            basePrivacySettings,
            Effect.empty()
        )

        // Then
        viewModel.state.test {
            assertEquals(expectedState, awaitItem())
        }
        verify(exactly = 1) {
            privacySettingsReducer.newStateFrom(
                PrivacySettingsState.Loading,
                PrivacySettingsEvent.Data.ContentLoaded(basePrivacySettings)
            )
        }
        confirmVerified(privacySettingsReducer)
    }

    @Test
    fun `should return an update error when updating remote content setting fails`() = runTest {
        // Given
        expectValidSettingsLoaded()
        coEvery { updateShowRemoteContentSettings(any()) } returns DataError.Local.NoDataCached.left()

        // When + Then
        verifyUpdateError { viewModel.onAutoShowRemoteContentToggled(true) }
    }

    @Test
    fun `should return an update error when updating embedded image setting fails`() = runTest {
        // Given
        expectValidSettingsLoaded()
        coEvery { updateAutoShowEmbeddedImagesSetting(any()) } returns DataError.Local.NoDataCached.left()

        // When + Then
        verifyUpdateError { viewModel.onAutoShowEmbeddedImagesToggled(true) }
    }

    @Test
    fun `should return an update error when updating link confirmation setting fails`() = runTest {
        // Given
        expectValidSettingsLoaded()
        coEvery { updateLinkConfirmationSetting(any()) } returns DataError.Local.NoDataCached.left()

        // When + Then
        verifyUpdateError { viewModel.onConfirmLinkToggled(true) }
    }

    @Test
    fun `should call the correct use case and update state when remote content setting update is done`() = runTest {
        // Given
        expectValidSettingsLoaded()
        val expectedFinalState = PrivacySettingsState.WithData(
            basePrivacySettings.copy(autoShowRemoteContent = false),
            Effect.empty()
        )
        coEvery { updateShowRemoteContentSettings(any()) } returns Unit.right()

        // When
        viewModel.state.test {
            skipItems(1)

            // When
            viewModel.onAutoShowRemoteContentToggled(false)

            // Then
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            updateShowRemoteContentSettings(false)
        }
        confirmVerified(updateShowRemoteContentSettings)
        verify {
            updateAutoShowEmbeddedImagesSetting wasNot called
            updateLinkConfirmationSetting wasNot called
            updatePreventScreenshotsSetting wasNot called
            updateBackgroundSyncSetting wasNot called
        }
    }

    @Test
    fun `should call the correct use case and update state when embedded images setting update is done`() = runTest {
        // Given
        expectValidSettingsLoaded()
        val expectedFinalState = PrivacySettingsState.WithData(
            basePrivacySettings.copy(autoShowEmbeddedImages = false),
            Effect.empty()
        )
        coEvery { updateAutoShowEmbeddedImagesSetting(any()) } returns Unit.right()

        // When
        viewModel.state.test {
            skipItems(1)

            // When
            viewModel.onAutoShowEmbeddedImagesToggled(false)

            // Then
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            updateAutoShowEmbeddedImagesSetting(false)
        }
        confirmVerified(updateAutoShowEmbeddedImagesSetting)
        verify {
            updateShowRemoteContentSettings wasNot called
            updateLinkConfirmationSetting wasNot called
            updatePreventScreenshotsSetting wasNot called
            updateBackgroundSyncSetting wasNot called
        }
    }

    @Test
    fun `should call the correct use case and update state when link confirmation setting update is done`() = runTest {
        // Given
        expectValidSettingsLoaded()
        val expectedFinalState = PrivacySettingsState.WithData(
            basePrivacySettings.copy(requestLinkConfirmation = false),
            Effect.empty()
        )
        coEvery { updateLinkConfirmationSetting(any()) } returns Unit.right()

        // When
        viewModel.state.test {
            skipItems(1)

            // When
            viewModel.onConfirmLinkToggled(false)

            // Then
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            updateLinkConfirmationSetting(false)
        }
        confirmVerified(updateLinkConfirmationSetting)
        verify {
            updateShowRemoteContentSettings wasNot called
            updateAutoShowEmbeddedImagesSetting wasNot called
            updatePreventScreenshotsSetting wasNot called
            updateBackgroundSyncSetting wasNot called
        }
    }

    @Test
    fun `should call the correct use case and update state when disable screenshots update is done`() = runTest {
        // Given
        expectValidSettingsLoaded()
        val expectedFinalState = PrivacySettingsState.WithData(
            basePrivacySettings.copy(preventTakingScreenshots = true),
            Effect.empty()
        )
        coEvery { updatePreventScreenshotsSetting(any()) } returns Unit.right()

        // When
        viewModel.state.test {
            skipItems(1)

            // When
            viewModel.onPreventScreenshotsToggled(true)

            // Then
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            updatePreventScreenshotsSetting(true)
        }
        confirmVerified(updatePreventScreenshotsSetting)
        verify {
            updateShowRemoteContentSettings wasNot called
            updateAutoShowEmbeddedImagesSetting wasNot called
            updateLinkConfirmationSetting wasNot called
            updateBackgroundSyncSetting wasNot called
        }
    }

    @Test
    fun `should call the correct use case and update state when allow background sync update is done`() = runTest {
        // Given
        expectValidSettingsLoaded()
        val expectedFinalState = PrivacySettingsState.WithData(
            basePrivacySettings.copy(allowBackgroundSync = true),
            Effect.empty()
        )
        coEvery { updateBackgroundSyncSetting(true) } returns Unit.right()

        // When
        viewModel.state.test {
            skipItems(1)

            // When
            viewModel.onAllowBackgroundSyncToggled(true)

            // Then
            assertEquals(expectedFinalState, awaitItem())
        }

        // Then
        coVerify(exactly = 1) {
            updateBackgroundSyncSetting(true)
        }
        confirmVerified(updateBackgroundSyncSetting)
        verify {
            updatePreventScreenshotsSetting wasNot called
            updateShowRemoteContentSettings wasNot called
            updateAutoShowEmbeddedImagesSetting wasNot called
            updateLinkConfirmationSetting wasNot called
        }
    }

    private suspend fun verifyUpdateError(action: PrivacySettingsViewModel.() -> Unit) {
        val expectedFinalState = PrivacySettingsState.WithData(basePrivacySettings, Effect.of(Unit))
        viewModel.state.test {
            skipItems(1) // Skip loading state
            action(viewModel)
            assertEquals(expectedFinalState, awaitItem())
        }
        // Then
        verifySequence {
            privacySettingsReducer.newStateFrom(
                PrivacySettingsState.Loading,
                PrivacySettingsEvent.Data.ContentLoaded(basePrivacySettings)
            )
            privacySettingsReducer.newStateFrom(
                PrivacySettingsState.WithData(basePrivacySettings, Effect.empty()),
                PrivacySettingsEvent.Error.UpdateError
            )
        }
        confirmVerified(privacySettingsReducer)
    }

    private fun expectValidSettingsLoaded() {
        every { observePrimaryUserId.invoke() } returns flowOf(userId)
        every { observePrivacySettings.invoke(userId) } returns flowOf(basePrivacySettings.right())
    }

    private companion object {

        val userId = UserIdTestData.userId
        val basePrivacySettings = PrivacySettings(
            autoShowRemoteContent = true,
            autoShowEmbeddedImages = true,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = false
        )
    }
}
