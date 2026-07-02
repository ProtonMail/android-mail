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
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLoadedMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.SelectMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabelExpandedState
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsidebar.presentation.usecase.ObservePrimaryCategoryUnreadCount
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.Action.LabelAction
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Enabled
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Collapse
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Expand
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction.Select
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarViewModelTest {

    private val appInformation = mockk<AppInformation>()

    private val observeLoadedMailLabelId = mockk<ObserveLoadedMailLabelId> {
        every { this@mockk.invoke() } returns MutableStateFlow(
            MailLabelId.System(SystemLabelId.Inbox.labelId)
        )
    }

    private val selectMailLabelId = mockk<SelectMailLabelId> {
        every { this@mockk.invoke(any()) } returns Unit
    }

    private val primaryUserId = MutableStateFlow<UserId?>(null)
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk() } returns primaryUserId
    }

    private val mailboxLabels = MutableStateFlow(MailLabels.Initial)
    private val observeMailboxLabels = mockk<ObserveMailLabels> {
        every { this@mockk(any<UserId>()) } returns mailboxLabels
    }

    private val updateLabelExpandedState = mockk<UpdateLabelExpandedState>(relaxUnitFun = true)

    private val observeUnreadCounters = mockk<ObserveUnreadCounters> {
        coEvery { this@mockk.invoke(any()) } returns flowOf(emptyList<UnreadCounter>())
    }

    private val observePrimaryCategoryUnreadCount = mockk<ObservePrimaryCategoryUnreadCount> {
        every { this@mockk.invoke(any()) } returns flowOf(null)
    }

    private lateinit var sidebarViewModel: SidebarViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sidebarViewModel = SidebarViewModel(
            appInformation = appInformation,
            updateLabelExpandedState = updateLabelExpandedState,
            observePrimaryUserId = observePrimaryUserId,
            observeMailLabels = observeMailboxLabels,
            observeUnreadCounters = observeUnreadCounters,
            observePrimaryCategoryUnreadCount = observePrimaryCategoryUnreadCount,
            observeLoadedMailLabelId = observeLoadedMailLabelId,
            selectMailLabelId = selectMailLabelId
        )
    }

    @Test
    fun `emits initial sidebar state when data is being loaded`() = runTest {
        // When
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUserId.emit(UserIdTestData.Primary)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = MailLabelId.System(SystemLabelId.Inbox.labelId),
                canChangeSubscription = false,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits initial sidebar state after user id recovers from being null`() = runTest {
        // Given
        primaryUserId.emit(null)

        // When
        sidebarViewModel.state.test {
            // Initial state is Disabled.
            assertEquals(Disabled, awaitItem())

            // Given
            primaryUserId.emit(UserIdTestData.Primary)

            // Then
            val actual = awaitItem() as Enabled
            val expected = Enabled(
                selectedMailLabelId = MailLabelId.System(SystemLabelId.Inbox.labelId),
                canChangeSubscription = false,
                mailLabels = MailLabelsUiModel.Loading
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `onSidebarLabelAction Select Archive, set selectedMailLabelId`() = runTest {
        // When
        sidebarViewModel.submit(LabelAction(Select(MailLabelTestData.archiveSystemLabel.id)))

        // Then
        verify { selectMailLabelId(MailLabelTestData.archiveSystemLabel.id) }
    }

    @Test
    fun `onSidebarLabelAction Collapse, call updateLabelExpandedState`() = runTest {
        // Given
        val mailLabelId = MailLabelId.Custom.Folder(LabelId("folder"))
        primaryUserId.emit(UserIdTestData.Primary)

        // When
        sidebarViewModel.submit(LabelAction(Collapse(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserIdTestData.Primary, mailLabelId, false) }
    }

    @Test
    fun `onSidebarLabelAction Expand, call updateLabelExpandedState`() = runTest {
        // Given
        val mailLabelId = MailLabelId.Custom.Folder(LabelId("folder"))
        primaryUserId.emit(UserIdTestData.Primary)

        // When
        sidebarViewModel.submit(LabelAction(Expand(mailLabelId)))

        // Then
        coVerify { updateLabelExpandedState.invoke(UserIdTestData.Primary, mailLabelId, true) }
    }

    @Test
    fun `when a primary category unread count is available, inbox row shows it`() = runTest {
        // Given
        every { observePrimaryCategoryUnreadCount(any()) } returns flowOf(7)
        coEvery { observeUnreadCounters(any()) } returns flowOf(
            listOf(UnreadCounter(MailLabelTestData.inboxSystemLabel.id.labelId, 3))
        )
        mailboxLabels.value = MailLabels(
            system = listOf(MailLabelTestData.inboxSystemLabel),
            folders = emptyList(),
            labels = emptyList()
        )

        // When + Then
        sidebarViewModel.state.test {
            assertEquals(Disabled, awaitItem())
            primaryUserId.emit(UserIdTestData.Primary)

            // onStart briefly seeds the regular inbox count before the Primary count arrives;
            // assert the settled value.
            val settled = expectMostRecentItem() as Enabled
            assertEquals(CappedNumberUiModel.Exact(7), settled.inboxCount())
        }
    }

    @Test
    fun `when no primary category unread count, inbox row shows the regular inbox unread count`() = runTest {
        // Given
        every { observePrimaryCategoryUnreadCount(any()) } returns flowOf(null)
        coEvery { observeUnreadCounters(any()) } returns flowOf(
            listOf(UnreadCounter(MailLabelTestData.inboxSystemLabel.id.labelId, 3))
        )
        mailboxLabels.value = MailLabels(
            system = listOf(MailLabelTestData.inboxSystemLabel),
            folders = emptyList(),
            labels = emptyList()
        )

        // When + Then
        sidebarViewModel.state.test {
            assertEquals(Disabled, awaitItem())
            primaryUserId.emit(UserIdTestData.Primary)

            val enabled = awaitItem() as Enabled
            assertEquals(CappedNumberUiModel.Exact(3), enabled.inboxCount())
        }
    }

    private fun Enabled.inboxCount(): CappedNumberUiModel =
        mailLabels.systemLabels.first { it.id == MailLabelTestData.inboxSystemLabel.id }.count
}
