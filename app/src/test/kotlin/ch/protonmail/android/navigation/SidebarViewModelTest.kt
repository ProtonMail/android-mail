/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation

import app.cash.turbine.test
import ch.protonmail.android.MailFeatureFlags
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.presentation.SelectedSidebarLocation
import ch.protonmail.android.sidebar.SidebarViewModel
import ch.protonmail.android.sidebar.SidebarViewModel.State.Enabled
import ch.protonmail.android.testdata.FeatureFlagTestData
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { location } returns MutableStateFlow<SidebarLocation>(Inbox)
    }
    private val showSettingsFlagFlow = MutableSharedFlow<FeatureFlag>()
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every {
            this@mockk.observe(UserIdTestData.userId, FeatureFlagTestData.showSettingsId)
        } returns showSettingsFlagFlow
    }
    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns flowOf(UserIdTestData.userId)
    }

    private lateinit var sidebarViewModel: SidebarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        sidebarViewModel = SidebarViewModel(
            selectedSidebarLocation,
            featureFlagManager,
            accountManager
        )
    }

    @Test
    fun emitsInitialSidebarStateWhenDataIsBeingLoaded() = runTest {
        // When
        sidebarViewModel.state.test {
            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, MailFeatureFlags.ShowSettings.defaultLocalValue)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun emitsIsSettingsEnabledTrueWhenSettingsFeatureToggleIsEnabled() = runTest {
        // Given
        sidebarViewModel.state.test {
            // When
            showSettingsFlagFlow.emit(FeatureFlagTestData.enabledShowSettings)

            // Then
            // Await one item only since it will be the same as the default value
            // (true for debug builds) and this will not cause further emissions
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun emitsIsSettingsEnabledFalseWhenSettingsFeatureToggleIsDisabled() = runTest {
        sidebarViewModel.state.test {
            // First item is the default value, which is true as
            // `MailFeatureFlags.localDefaultValue` is true by default for debug builds
            awaitItem()

            // When
            showSettingsFlagFlow.emit(FeatureFlagTestData.disabledShowSettings)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, false)
            assertEquals(expected, actual)
        }
    }
}
