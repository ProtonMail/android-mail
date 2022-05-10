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

package ch.protonmail.android.mailmailbox.presentation

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.MailFeatureDefault
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.State.Enabled
import ch.protonmail.android.testdata.FeatureFlagTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.user.domain.entity.User
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val appInformation = mockk<AppInformation>()
    private val selectedSidebarLocation = mockk<SelectedSidebarLocation> {
        every { location } returns MutableStateFlow<SidebarLocation>(Inbox)
    }

    private val showSettings = MutableSharedFlow<FeatureFlag>()
    private val mailFeatureDefault = mockk<MailFeatureDefault> {
        every { this@mockk[MailFeatureId.ShowSettings] } returns false
    }
    private val observeMailFeature = mockk<ObserveMailFeature> {
        every { this@mockk.invoke(FeatureFlagTestData.showSettingsId) } returns showSettings
    }
    private val primaryUser = MutableSharedFlow<User?>()
    private val observePrimaryUser = mockk<ObservePrimaryUser> {
        every { this@mockk.invoke() } returns primaryUser
    }

    private lateinit var sidebarViewModel: SidebarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sidebarViewModel = SidebarViewModel(
            appInformation,
            selectedSidebarLocation,
            mailFeatureDefault,
            observeMailFeature,
            observePrimaryUser
        )
    }

    @Test
    fun `emits initial sidebar state when data is being loaded`() = runTest {
        // When
        sidebarViewModel.state.test {
            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = false, canChangeSubscription = true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits is settings enabled true when settings feature toggle is enabled`() = runTest {
        sidebarViewModel.state.test {
            // Given
            showSettings.emit(FeatureFlagTestData.enabledShowSettings)
            primaryUser.emit(UserTestData.user)

            // Then
            awaitItem() // First item is the default value (false).
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = true, canChangeSubscription = true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits is settings enabled false when settings feature toggle is disabled`() = runTest {
        sidebarViewModel.state.test {
            // Given
            showSettings.emit(FeatureFlagTestData.disabledShowSettings)

            // Then
            // Await one item only since it will be the same as the default value (false).
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = false, canChangeSubscription = true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can change subscriptions when user is free`() = runTest {
        sidebarViewModel.state.test {
            // Given
            showSettings.emit(FeatureFlagTestData.disabledShowSettings)
            primaryUser.emit(UserTestData.user)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = false, canChangeSubscription = true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can change subscriptions when user is admin`() = runTest {
        sidebarViewModel.state.test {
            // Given
            showSettings.emit(FeatureFlagTestData.disabledShowSettings)
            primaryUser.emit(UserTestData.adminUser)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = false, canChangeSubscription = true)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can't change subscription when user is organization member`() = runTest {
        sidebarViewModel.state.test {
            // Given
            showSettings.emit(FeatureFlagTestData.disabledShowSettings)
            primaryUser.emit(UserTestData.orgMemberUser)

            // Then
            awaitItem() // First emitted item is the initial value
            val actual = awaitItem() as Enabled
            val expected = Enabled(Inbox, isSettingsEnabled = false, canChangeSubscription = false)
            assertEquals(expected, actual)
        }
    }
}
