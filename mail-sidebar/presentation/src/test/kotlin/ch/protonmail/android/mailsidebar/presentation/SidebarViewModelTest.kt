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

package ch.protonmail.android.mailsidebar.presentation

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
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
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.Action.LabelAction
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Enabled
import ch.protonmail.android.mailsidebar.presentation.usecase.ObserveSidebarUpsellingVisibility
import ch.protonmail.android.mailsidebar.presentation.usecase.TrackSidebarUpsellingClick
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.payment.domain.PaymentManager
import me.proton.core.user.domain.entity.User
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val appInformation = mockk<AppInformation>()

    private val selectedMailLabelId = mockk<SelectedMailLabelId> {
        every { this@mockk.flow } returns MutableStateFlow<MailLabelId>(Inbox)
        every { this@mockk.set(any()) } returns Unit
    }

    private val primaryUser = MutableStateFlow<User?>(null)
    private val observePrimaryUser = mockk<ObservePrimaryUser> {
        every { this@mockk() } returns primaryUser
    }

    private val mailboxLabels = MutableStateFlow(MailLabels.Initial)
    private val observeMailboxLabels = mockk<ObserveMailLabels> {
        every { this@mockk(any<UserId>(), any()) } returns mailboxLabels
    }

    private val updateLabelExpandedState = mockk<UpdateLabelExpandedState>(relaxUnitFun = true)

    private val folderColorSettings = MutableStateFlow(FolderColorSettings())
    private val observeFolderColors = mockk<ObserveFolderColorSettings> {
        every { this@mockk(any()) } returns folderColorSettings
    }

    private val observeUnreadCounters = mockk<ObserveUnreadCounters> {
        coEvery { this@mockk.invoke(any()) } returns flowOf(emptyList<UnreadCounter>())
    }
    private val paymentManager = mockk<PaymentManager> {
        coEvery { this@mockk.isSubscriptionAvailable(userId = any()) } returns true
    }
    private val observeSidebarUpsellingVisibility = mockk<ObserveSidebarUpsellingVisibility> {
        coEvery { this@mockk.invoke() } returns flowOf(false)
    }
    private val trackUpsellingClick = mockk<TrackSidebarUpsellingClick>()

    private lateinit var sidebarViewModel: SidebarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sidebarViewModel = SidebarViewModel(
            appInformation = appInformation,
            selectedMailLabelId = selectedMailLabelId,
            updateLabelExpandedState = updateLabelExpandedState,
            paymentManager = paymentManager,
            observePrimaryUser = observePrimaryUser,
            observeFolderColors = observeFolderColors,
            observeMailLabels = observeMailboxLabels,
            observeUnreadCounters = observeUnreadCounters,
            observeSidebarUpsellingVisibility = observeSidebarUpsellingVisibility,
            trackUpsellingClick = trackUpsellingClick
        )
    }

    @Test
    fun `emits initial sidebar state when data is being loaded`() = runTest {
        // When
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUser.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                showUpsell = false,
                selectedMailLabelId = Inbox,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can change subscriptions when payment manager subscription available is true`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            coEvery { paymentManager.isSubscriptionAvailable(UserIdTestData.userId) } returns true
            primaryUser.emit(UserTestData.Primary)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                showUpsell = false,
                selectedMailLabelId = Inbox,
                canChangeSubscription = true,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `state is can't change subscription when payment manager subscription available is false`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            coEvery { paymentManager.isSubscriptionAvailable(UserIdTestData.adminUserId) } returns false
            primaryUser.emit(UserTestData.orgMemberUser)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                showUpsell = false,
                selectedMailLabelId = Inbox,
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
        primaryUser.emit(UserTestData.Primary)

        // When
        sidebarViewModel.submit(LabelAction(Collapse(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserTestData.Primary.userId, mailLabelId, false) }
    }

    @Test
    fun `onSidebarLabelAction Expand, call updateLabelExpandedState`() = runTest {
        // Given
        val mailLabelId = MailLabelId.Custom.Folder(LabelId("folder"))
        primaryUser.emit(UserTestData.Primary)

        // When
        sidebarViewModel.submit(LabelAction(Expand(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserTestData.Primary.userId, mailLabelId, true) }
    }

    @Test
    fun `emit upsell when upselling enabled`() = runTest {
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            coEvery { paymentManager.isSubscriptionAvailable(UserIdTestData.adminUserId) } returns false
            coEvery { observeSidebarUpsellingVisibility.invoke() } returns flowOf(true)
            primaryUser.emit(UserTestData.orgMemberUser)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                showUpsell = true,
                selectedMailLabelId = Inbox,
                canChangeSubscription = false,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }
}
