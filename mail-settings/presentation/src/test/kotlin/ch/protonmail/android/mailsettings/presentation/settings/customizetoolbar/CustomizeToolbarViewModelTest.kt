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
                firstTabTitleRes = R.string.customize_toolbar_conversation
            )
            actual.assertActionsEqual(
                messageActionsSelected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ),
                messageActionsUnselected = listOf(
                    ToolbarAction.ReplyOrReplyAll,
                    ToolbarAction.Forward,
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.Print,
                    ToolbarAction.ReportPhishing
                ),
                inboxActionsSelected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs,
                    ToolbarAction.MoveToSpam
                ),
                inboxActionsUnelected = listOf(
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive
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
                        "unknown2"
                    ).map { ToolbarAction.enumOf(it) },
                    inboxActions = listOf(
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
                firstTabTitleRes = R.string.customize_toolbar_conversation
            )
            actual.assertActionsEqual(
                messageActionsSelected = listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.ReportPhishing
                ),
                messageActionsUnselected = listOf(
                    ToolbarAction.LabelAs,
                    ToolbarAction.ReplyOrReplyAll,
                    ToolbarAction.Forward,
                    ToolbarAction.StarOrUnstar,
                    ToolbarAction.MoveToArchive
                ),
                inboxActionsSelected = listOf(
                    ToolbarAction.LabelAs,
                    ToolbarAction.MoveToArchive,
                    ToolbarAction.StarOrUnstar
                ),
                inboxActionsUnelected = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.MoveToSpam
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
                firstTabTitleRes = R.string.customize_toolbar_conversation
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
                firstTabTitleRes = R.string.customize_toolbar_message
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
                inboxActions = listOf(
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

    private fun CustomizeToolbarState.assertTabsEqual(tabIndex: Int, @StringRes firstTabTitleRes: Int) {
        assertTrue(this is Data)
        assertEquals(tabIndex, this.selectedTabIdx)
        assertEquals(firstTabTitleRes, (tabs.first() as TextUiModel.TextRes).value)
    }

    private fun CustomizeToolbarState.assertActionsEqual(
        messageActionsSelected: List<ToolbarAction>,
        messageActionsUnselected: List<ToolbarAction>,
        inboxActionsSelected: List<ToolbarAction>,
        inboxActionsUnelected: List<ToolbarAction>
    ) {
        assertTrue(this is Data)
        val messageItemsSelected = pages.first().selectedActions
        val messageItemsUnselected = pages.first().remainingActions
        val inboxItemsSelected = pages[1].selectedActions
        val inboxItemsUnselected = pages[1].remainingActions

        assertContentEquals(
            expected = messageActionsSelected.map { it.value },
            actual = messageItemsSelected.map { it.id }
        )
        assertContentEquals(
            expected = messageActionsUnselected.map { it.value },
            actual = messageItemsUnselected.map { it.id }
        )
        assertContentEquals(
            expected = inboxActionsSelected.map { it.value },
            actual = inboxItemsSelected.map { it.id }
        )
        assertContentEquals(
            expected = inboxActionsUnelected.map { it.value },
            actual = inboxItemsUnselected.map { it.id }
        )
    }

    private fun defaultPreference(
        convMode: Boolean,
        messageActions: List<StringEnum<ToolbarAction>>? = null,
        conversationActions: List<StringEnum<ToolbarAction>>? = null,
        inboxActions: List<StringEnum<ToolbarAction>>? = null
    ) = ToolbarActionsPreference(
        messageToolbar = actions(messageActions, Defaults.MessageActions, Defaults.AllMessageActions),
        conversationToolbar = actions(conversationActions, Defaults.MessageActions, Defaults.AllMessageActions),
        listToolbar = actions(inboxActions, Defaults.InboxActions, Defaults.AllInboxActions),
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
