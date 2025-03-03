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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar

import androidx.annotation.StringRes
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.SettingsToolbarType
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.ActionSelection
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.Defaults
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.ToolbarActions
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarPreferenceRepository.Error
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ObserveToolbarActionsSettings
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ReorderSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ResetSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.SaveSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.SelectSettingsActions
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.CustomizeToolbarState.Data
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.mapper.ToolbarActionMapper
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.model.CustomizeToolbarOperation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomizeToolbarViewModelTest {

    private val prefsFlow = MutableSharedFlow<Either<Error.UserNotLoggedIn, ToolbarActionsPreference>>(replay = 1)

    private val resetToDefault: ResetSettingsActions = mockk()
    private val select: SelectSettingsActions = mockk()
    private val reorder: ReorderSettingsActions = mockk()
    private val save: SaveSettingsActions = mockk()
    private val observe: ObserveToolbarActionsSettings = mockk {
        every { this@mockk.invoke() } returns prefsFlow
    }
    private val mapper: ToolbarActionMapper = ToolbarActionMapper()

    private lateinit var viewModel: CustomizeToolbarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = CustomizeToolbarViewModel(
            resetToDefault, select, reorder, save, observe, mapper
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `state is logged out if observe returns an error`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(Error.UserNotLoggedIn.left())

            // Then
            assertEquals(CustomizeToolbarState.NotLoggedIn, awaitItem())

            viewModel.submit(CustomizeToolbarOperation.TabSelected(1))
        }
    }

    @Test
    fun `state has correctly partitioned default actions`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(defaultPreference(convMode = true).right())

            // Then
            val actual = awaitItem()
            actual.assertTabsEqual(
                tabIndex = 0,
                firstTabTitleRes = R.string.customize_toolbar_conversation,
                mailboxDisclaimerRes = R.string.customize_toolbar_disclaimer_mailbox_conversations
            )
            actual.assertActionsEqual(
                messageActionsSelected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ),
                messageActionsUnselected = listOf(
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.MoveToSpam,
                    ToolbarAction.Print,
                    ToolbarAction.ReportPhishing
                ),
                mailboxActionsSelected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ),
                mailboxActionsUnselected = listOf(
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.MoveToSpam
                ),
                ActionEnabledStates(
                    canAddMessage = true,
                    canRemoveMessage = true,
                    canAddMailbox = true,
                    canRemoveMailbox = true
                )
            )
        }
    }

    @Test
    fun `state has correctly partitioned actions`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(
                defaultPreference(
                    convMode = true,
                    conversationActions = listOf(
                        "unknown1",
                        "move",
                        "print",
                        "report_phishing",
                        "unknown2",
                        "unknown3",
                        "unknown4"
                    ).map { ToolbarAction.enumOf(it) },
                    mailboxActions = listOf(
                        "unknown1",
                        "label",
                        "unknown2",
                        "archive",
                        "toggle_star"
                    ).map { ToolbarAction.enumOf(it) }
                ).right()
            )

            // Then
            val actual = awaitItem()
            actual.assertTabsEqual(
                tabIndex = 0,
                firstTabTitleRes = R.string.customize_toolbar_conversation,
                mailboxDisclaimerRes = R.string.customize_toolbar_disclaimer_mailbox_conversations
            )
            actual.assertActionsEqual(
                messageActionsSelected = listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.ReportPhishing
                ),
                messageActionsUnselected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.LabelAs,
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.MoveToSpam
                ),
                mailboxActionsSelected = listOf(
                    ToolbarAction.LabelAs,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.StarOrUnstar
                ),
                mailboxActionsUnselected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.MoveToSpam
                ),
                ActionEnabledStates(
                    canAddMessage = true,
                    canRemoveMessage = true,
                    canAddMailbox = true,
                    canRemoveMailbox = true
                )
            )
        }
    }

    @Test
    fun `more actions cannot be added once 5 recognized are already selected`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(
                defaultPreference(
                    convMode = true,
                    conversationActions = listOf(
                        "unknown1",
                        "move",
                        "spam",
                        "print",
                        "report_phishing",
                        "label",
                        "toggle_star",
                        "unknown2"
                    ).map { ToolbarAction.enumOf(it) },
                    mailboxActions = listOf(
                        "unknown1",
                        "label",
                        "unknown2",
                        "archive",
                        "reply",
                        "print",
                        "toggle_star"
                    ).map { ToolbarAction.enumOf(it) }
                ).right()
            )

            // Then
            val actual = awaitItem()
            actual.assertTabsEqual(
                tabIndex = 0,
                firstTabTitleRes = R.string.customize_toolbar_conversation,
                mailboxDisclaimerRes = R.string.customize_toolbar_disclaimer_mailbox_conversations
            )
            actual.assertActionsEqual(
                messageActionsSelected = listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.MoveToSpam,
                    ToolbarAction.Print,
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.LabelAs,
                    ToolbarAction.StarOrUnstar
                ),
                messageActionsUnselected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveToArchive
                ),
                mailboxActionsSelected = listOf(
                    ToolbarAction.LabelAs,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.ReplyOrReplyAll,
                    ToolbarAction.Print,
                    ToolbarAction.StarOrUnstar
                ),
                mailboxActionsUnselected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.MoveToSpam
                ),
                ActionEnabledStates(
                    canAddMessage = false,
                    canRemoveMessage = true,
                    canAddMailbox = false,
                    canRemoveMailbox = true
                )
            )
        }
    }

    @Test
    fun `state correctly switches tabs`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(defaultPreference(convMode = true).right())
            awaitItem()

            viewModel.submit(CustomizeToolbarOperation.TabSelected(1))

            // Then
            val actual = awaitItem()
            actual.assertTabsEqual(
                tabIndex = 1,
                firstTabTitleRes = R.string.customize_toolbar_conversation,
                mailboxDisclaimerRes = R.string.customize_toolbar_disclaimer_mailbox_conversations
            )
        }
    }

    @Test
    fun `state correctly determines the first tab index`() = runTest {
        viewModel.state.test {
            // Given
            initialStateEmitted()

            // When
            prefsFlow.emit(defaultPreference(convMode = false).right())

            // Then
            val actual = awaitItem()
            actual.assertTabsEqual(
                tabIndex = 0,
                firstTabTitleRes = R.string.customize_toolbar_message,
                mailboxDisclaimerRes = R.string.customize_toolbar_disclaimer_mailbox
            )
        }
    }

    @Test
    fun `correct state is taken when saving`() = runTest {
        // Given
        coEvery { save.invoke(any()) } just runs
        viewModel.state.test {
            initialStateEmitted()

            // When
            val prefs = defaultPreference(
                convMode = true,
                conversationActions = listOf(
                    "unknown1",
                    "move",
                    "print",
                    "report_phishing",
                    "unknown2"
                ).map { ToolbarAction.enumOf(it) },
                mailboxActions = listOf(
                    "unknown1",
                    "label",
                    "unknown2",
                    "archive",
                    "toggle_star"
                ).map { ToolbarAction.enumOf(it) }
            )
            prefsFlow.emit(prefs.right())
            awaitItem()

            viewModel.submit(CustomizeToolbarOperation.SaveClicked)

            // Then
            coVerify(exactly = 1) { save.invoke(prefs) }
        }
    }

    @Test
    fun `correct tab is used when selecting actions`() = runTest {
        // Given
        every { select.invoke(any(), any(), any()) } just runs
        every { reorder.invoke(any(), any(), any()) } just runs
        viewModel.state.test {
            initialStateEmitted()

            // When
            prefsFlow.emit(defaultPreference(convMode = true).right())
            awaitItem()

            viewModel.submit(CustomizeToolbarOperation.TabSelected(1))
            awaitItem()

            viewModel.submit(CustomizeToolbarOperation.ActionSelected("reply"))
            viewModel.submit(CustomizeToolbarOperation.ActionRemoved("archive"))
            viewModel.submit(CustomizeToolbarOperation.ActionMoved(1, 3))

            // Then
            verify(exactly = 1) { select.invoke("reply", selected = true, tab = SettingsToolbarType.Inbox) }
            verify(exactly = 1) { select.invoke("archive", selected = false, tab = SettingsToolbarType.Inbox) }
            verify(exactly = 1) { reorder.invoke(1, 3, tab = SettingsToolbarType.Inbox) }
        }
    }

    private fun CustomizeToolbarState.assertTabsEqual(
        tabIndex: Int,
        @StringRes firstTabTitleRes: Int,
        @StringRes mailboxDisclaimerRes: Int
    ) {
        assertTrue(this is Data)
        assertEquals(tabIndex, this.selectedTabIdx)
        assertEquals(firstTabTitleRes, (tabs.first() as TextUiModel.TextRes).value)
        assertEquals(mailboxDisclaimerRes, (pages[1].disclaimer as TextUiModel.TextRes).value)
    }

    private data class ActionEnabledStates(
        val canAddMailbox: Boolean,
        val canRemoveMailbox: Boolean,
        val canAddMessage: Boolean,
        val canRemoveMessage: Boolean
    )

    private fun CustomizeToolbarState.assertActionsEqual(
        messageActionsSelected: List<ToolbarAction>,
        messageActionsUnselected: List<ToolbarAction>,
        mailboxActionsSelected: List<ToolbarAction>,
        mailboxActionsUnselected: List<ToolbarAction>,
        enabledStates: ActionEnabledStates
    ) {
        assertTrue(this is Data)
        val messageItemsSelected = pages.first().selectedActions
        val messageItemsUnselected = pages.first().remainingActions
        val mailboxItemsSelected = pages[1].selectedActions
        val mailboxItemsUnselected = pages[1].remainingActions

        assertContentEquals(
            expected = messageActionsSelected.map { it.value },
            actual = messageItemsSelected.map { it.id }
        )
        assertContentEquals(
            expected = messageActionsUnselected.map { it.value },
            actual = messageItemsUnselected.map { it.id }
        )
        assertContentEquals(
            expected = mailboxActionsSelected.map { it.value },
            actual = mailboxItemsSelected.map { it.id }
        )
        assertContentEquals(
            expected = mailboxActionsUnselected.map { it.value },
            actual = mailboxItemsUnselected.map { it.id }
        )
        messageItemsSelected.forEach { assertEquals(enabledStates.canRemoveMessage, it.enabled) }
        messageItemsUnselected.forEach { assertEquals(enabledStates.canAddMessage, it.enabled) }
        mailboxItemsSelected.forEach { assertEquals(enabledStates.canRemoveMailbox, it.enabled) }
        mailboxItemsUnselected.forEach { assertEquals(enabledStates.canAddMailbox, it.enabled) }
    }

    private fun defaultPreference(
        convMode: Boolean,
        messageActions: List<StringEnum<ToolbarAction>>? = null,
        conversationActions: List<StringEnum<ToolbarAction>>? = null,
        mailboxActions: List<StringEnum<ToolbarAction>>? = null
    ) = ToolbarActionsPreference(
        messageToolbar = actions(messageActions, Defaults.MessageConversationActions, Defaults.AllMessageActions),
        conversationToolbar = actions(
            conversationActions,
            Defaults.MessageConversationActions, Defaults.AllConversationActions
        ),
        listToolbar = actions(mailboxActions, Defaults.MailboxActions, Defaults.AllMailboxActions),
        isConversationMode = convMode
    )

    private fun actions(
        custom: List<StringEnum<ToolbarAction>>?,
        defaults: List<ToolbarAction>,
        all: List<ToolbarAction>
    ) = ToolbarActions(
        current = ActionSelection(
            selected = custom ?: defaults.map { ToolbarAction.enumOf(it.value) },
            all = all
        ),
        default = defaults
    )

    private suspend fun ReceiveTurbine<CustomizeToolbarState>.initialStateEmitted() {
        awaitItem() as CustomizeToolbarState.Loading
    }
}
