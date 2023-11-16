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

package ch.protonmail.android.mailsettings.presentation.settings.notifications.viewmodel

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.domain.usecase.notifications.SetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.ExtendedNotificationsSettingUiModel
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.notifications.reducer.PushNotificationsSettingsReducer
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class PushNotificationsSettingsViewModelTest {

    private val getNotificationExtendedPreference = mockk<GetExtendedNotificationsSetting>()
    private val setNotificationsExtendedSetting = mockk<SetExtendedNotificationsSetting>()
    private val reducer = spyk(PushNotificationsSettingsReducer())
    private val viewModel by lazy {
        PushNotificationsSettingsViewModel(
            getNotificationExtendedPreference,
            setNotificationsExtendedSetting,
            reducer
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
    fun `should return an error when no preference can be fetched`() = runTest {
        // Given
        coEvery { getNotificationExtendedPreference() } returns DataError.Local.NoDataCached.left()

        // Then
        viewModel.state.test {
            val errorState = awaitItem()
            assertEquals(PushNotificationsSettingsState.LoadingError, errorState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                PushNotificationsSettingsState.Loading,
                PushNotificationSettingsEvent.Error.LoadingError
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should load data when the preference is fetched with success`() = runTest {
        // Given
        coEvery { getNotificationExtendedPreference() } returns BaseExpectedPreference.right()

        // Then
        viewModel.state.test {
            val actualState = awaitItem()
            assertEquals(BaseLoadedState, actualState)
        }
        verify(exactly = 1) {
            reducer.newStateFrom(
                PushNotificationsSettingsState.Loading,
                PushNotificationSettingsEvent.Data.Loaded(BaseExpectedPreference)
            )
        }
        confirmVerified(reducer)
    }

    @Test
    fun `should update the state when the notifications extended toggled value is changed`() = runTest {
        // Given
        val newValue = false
        coEvery { getNotificationExtendedPreference() } returns BaseExpectedPreference.right()
        coEvery { setNotificationsExtendedSetting(newValue) } returns Unit.right()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(PushNotificationSettingsViewAction.ToggleExtendedNotifications(newValue))
            assertEquals(
                BaseLoadedState.copy(
                    BaseExtendedNotificationsState.copy(
                        ExtendedNotificationsSettingUiModel(newValue)
                    )
                ),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                PushNotificationsSettingsState.Loading,
                PushNotificationSettingsEvent.Data.Loaded(BaseExpectedPreference)
            )
            reducer.newStateFrom(
                BaseLoadedState,
                PushNotificationSettingsViewAction.ToggleExtendedNotifications(newValue)
            )
        }
    }

    @Test
    fun `should report the error when the notifications extended toggled value fails to be changed`() = runTest {
        // Given
        val newValue = false
        coEvery { getNotificationExtendedPreference() } returns BaseExpectedPreference.right()
        coEvery {
            setNotificationsExtendedSetting(newValue)
        } returns SetExtendedNotificationsSetting.Error.UpdateFailed.left()

        // When + Then
        viewModel.state.test {
            skipItems(1)

            viewModel.submit(PushNotificationSettingsViewAction.ToggleExtendedNotifications(newValue))
            assertEquals(
                BaseLoadedState.copy(updateErrorState = BaseUpdateErrorState),
                awaitItem()
            )
        }
        coVerifySequence {
            reducer.newStateFrom(
                PushNotificationsSettingsState.Loading,
                PushNotificationSettingsEvent.Data.Loaded(BaseExpectedPreference)
            )
            reducer.newStateFrom(
                BaseLoadedState,
                PushNotificationSettingsEvent.Error.UpdateError
            )
        }
    }

    private companion object {

        val BaseExpectedPreference = ExtendedNotificationPreference(true)

        val BaseExtendedNotificationsState = PushNotificationsSettingsState.ExtendedNotificationState(
            ExtendedNotificationsSettingUiModel(true)
        )

        val BaseLoadedState = PushNotificationsSettingsState.DataLoaded(
            BaseExtendedNotificationsState,
            PushNotificationsSettingsState.UpdateErrorState(Effect.empty())
        )

        val BaseUpdateErrorState = PushNotificationsSettingsState.UpdateErrorState(Effect.of(Unit))
    }
}
