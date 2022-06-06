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

package ch.protonmail.android.mailmailbox.presentation

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.MailFeatureDefault
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Inbox
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabelExpandedState
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Collapse
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Expand
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Select
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.Action.LabelAction
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.State.Enabled
import ch.protonmail.android.mailsettings.domain.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.FeatureFlagTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.User
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(userId)
    }
    private val appInformation = mockk<AppInformation>()

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(Inbox)
        every { this@mockk.set(any()) } returns Unit
    }

    private val mailFeatureDefault = mockk<MailFeatureDefault> {
        every { this@mockk[MailFeatureId.ShowSettings] } returns false
    }

    private val showSettings = MutableStateFlow(FeatureFlag(FeatureFlagTestData.showSettingsId.id, false))
    private val observeMailFeature = mockk<ObserveMailFeature> {
        every { this@mockk(userId, FeatureFlagTestData.showSettingsId) } returns showSettings
    }
    private val primaryUser = MutableStateFlow<User?>(null)
    private val observeUser = mockk<ObserveUser> {
        every { this@mockk(userId) } returns primaryUser
    }

    private val mailboxLabels = MutableStateFlow(MailLabels.Initial)
    private val observeMailboxLabels = mockk<ObserveMailLabels> {
        every { this@mockk.invoke(any<UserId>()) } returns mailboxLabels
    }

    private val updateLabelExpandedState = mockk<UpdateLabelExpandedState>(relaxUnitFun = true)

    private val folderColorSettings = MutableStateFlow(FolderColorSettings())
    private val observeFolderColors = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(any()) } returns folderColorSettings
    }

    private lateinit var sidebarViewModel: SidebarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sidebarViewModel = SidebarViewModel(
            accountManager = accountManager,
            appInformation,
            selectedMailLabelId,
            mailFeatureDefault,
            updateLabelExpandedState,
            observeMailFeature,
            observePrimaryUser,
            observeFolderColors,
            observeMailboxLabels,
        )
    }

    @Test
    fun `emits initial sidebar state when data is being loaded`() = runTest {
        // When
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.user)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = false,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits is settings enabled true when settings feature toggle is enabled`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            showSettings.emit(FeatureFlagTestData.enabledShowSettings)
            primaryUser.emit(UserTestData.user)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = true,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits is settings enabled false when settings feature toggle is disabled`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.user)
            showSettings.emit(FeatureFlagTestData.disabledShowSettings)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = false,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can change subscriptions when user is free`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.user)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = false,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can change subscriptions when user is admin`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.adminUser)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = false,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can't change subscription when user is organization member`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.orgMemberUser)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = Inbox,
                isSettingsEnabled = false,
                canChangeSubscription = false,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `onSidebarLabelAction Select Archive, set selectedMailLabelId`() = runTest {
        // When
        sidebarViewModel.submit(LabelAction(Select(Archive)))

        // Then
        verify { selectedMailLabelId.set(Archive) }
    }

    @Test
    fun `onSidebarLabelAction Collapse, call updateLabelExpandedState`() = runTest {
        // Given
        val mailLabelId = MailLabelId.Custom.Folder(LabelId("folder"))
        primaryUser.emit(UserTestData.user)

        // When
        sidebarViewModel.submit(LabelAction(Collapse(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserTestData.user.userId, mailLabelId, false) }
    }

    @Test
    fun `onSidebarLabelAction Expand, call updateLabelExpandedState`() = runTest {
        // Given
        val mailLabelId = MailLabelId.Custom.Folder(LabelId("folder"))
        primaryUser.emit(UserTestData.user)

        // When
        sidebarViewModel.submit(LabelAction(Expand(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserTestData.user.userId, mailLabelId, true) }
    }
}
