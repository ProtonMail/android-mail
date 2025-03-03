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

package ch.protonmail.android.mailsettings.data.repository

import app.cash.turbine.test
import arrow.core.left
import ch.protonmail.android.mailsettings.domain.model.SettingsToolbarType
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.ActionSelection
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.Defaults
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference.ToolbarActions
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarPreferenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ActionsToolbarSetting
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MobileSettings
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class InMemoryToolbarPreferenceRepositoryImplTest {

    private val mailSettingsRepository = mockk<MailSettingsRepository>()

    private val accountManager = mockk<AccountManager> {
        every { this@mockk.getPrimaryUserId() } returns flowOf(UserId("test-id"))
    }

    private val repo: InMemoryToolbarPreferenceRepositoryImpl =
        InMemoryToolbarPreferenceRepositoryImpl(mailSettingsRepository, accountManager)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `returns error if user is not logged in`() = runTest {
        // Given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(null)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val item = awaitItem()
            assertEquals(InMemoryToolbarPreferenceRepository.Error.UserNotLoggedIn.left(), item)
            coVerify(exactly = 0) { mailSettingsRepository.getMailSettingsFlow(any()) }
            awaitComplete()
        }
    }

    @Test
    fun `returns default actions if user has no preference set`() = runTest {
        // Given
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns null
            every { this@mockk.mobileSettings } returns null
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(convMode = false)
            val item = awaitItem().getOrNull()
            assertEquals(expected, item)
        }
    }

    @Test
    fun `returns correctly mapped actions if user has a preference set`() = runTest {
        // Given
        val mobileSettings = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam
                )
                    .map { ToolbarAction.enumOf(it.value) }
            ),
            messageToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.ReplyOrReplyAll,
                    ToolbarAction.MarkAsReadOrUnread
                )
                    .map { ToolbarAction.enumOf(it.value) }
            ),
            conversationToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.MoveToSpam,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo
                )
                    .map { ToolbarAction.enumOf(it.value) }
            )
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns null
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(
                convMode = false,
                mailboxActions = listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam
                ).stringEnums(),
                messageActions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.ReplyOrReplyAll,
                    ToolbarAction.MarkAsReadOrUnread
                ).stringEnums(),
                conversationActions = listOf(
                    ToolbarAction.MoveToSpam,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo
                ).stringEnums()
            )
            val item = awaitItem().getOrNull()
            assertEquals(expected, item)
        }
    }

    @Test
    fun `returns correctly partitioned actions when user has a preference`() = runTest {
        // Given
        val remoteActions = listOf(
            "unknown1",
            "move",
            "print",
            "spam",
            "unknown2"
        )
        val mobileSettings = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = remoteActions.map { ToolbarAction.enumOf(it) }
            ),
            messageToolbar = null,
            conversationToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                mailboxActions = listOf(ToolbarAction.enumOf("unknown1")) + listOf(
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam
                ).stringEnums() + listOf(ToolbarAction.enumOf("unknown2"))
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `returns default actions when user preference is empty`() = runTest {
        // Given
        val mobileSettings = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = emptyList()
            ),
            messageToolbar = null,
            conversationToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                mailboxActions = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ).stringEnums()
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `returns empty actions when user preference is empty and is custom is true`() = runTest {
        // Given
        val mobileSettings = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = true,
                actions = emptyList()
            ),
            messageToolbar = null,
            conversationToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                mailboxActions = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ).stringEnums()
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `returns default actions when user preference has unsupported actions`() = runTest {
        // Given
        val mobileSettings = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = true,
                actions = listOf(
                    "unknown1",
                    "unknown2"
                ).map { ToolbarAction.enumOf(it) }
            ),
            messageToolbar = null,
            conversationToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                mailboxActions = listOf(
                    ToolbarAction.MarkAsReadOrUnread,
                    ToolbarAction.MoveToTrash,
                    ToolbarAction.MoveTo,
                    ToolbarAction.LabelAs
                ).stringEnums()
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `removes an action from the selection when deselected`() = runTest {
        // Given
        val mobileSettings = MobileSettings(
            listToolbar = null,
            messageToolbar = null,
            conversationToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam
                ).stringEnums()
            )
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            awaitItem()
            repo.toggleSelection(ToolbarAction.Print.value, tab = SettingsToolbarType.Message, toggled = false)

            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                conversationActions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.MoveTo,
                    ToolbarAction.MoveToSpam
                ).stringEnums()
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `reorders an action when requested`() = runTest {
        // Given
        val remoteActions = listOf(
            "report_phishing",
            "unknown1",
            "move",
            "print",
            "spam",
            "unknown2"
        )
        val mobileSettings = MobileSettings(
            listToolbar = null,
            conversationToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = remoteActions.map { ToolbarAction.enumOf(it) }
            ),
            messageToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettings
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            awaitItem()
            repo.reorder(fromIndex = 0, toIndex = 2, tab = SettingsToolbarType.Message)
            /**
             *    "report_phishing",  | from idx 0
             *    "unknown1",
             *    "move"
             *    "print",
             *    "spam",
             *    "unknown2",
             ->>
             *    "move",
             *    "unknown1",
             *    "print",
             *    "report_phishing", // now at index 2 (not counting unknown)
             *    "spam",
             *    "unknown2",
             */

            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                conversationActions = listOf(
                    ToolbarAction.enumOf(ToolbarAction.MoveTo.value),
                    ToolbarAction.enumOf("unknown1"),
                    ToolbarAction.enumOf(ToolbarAction.Print.value),
                    ToolbarAction.enumOf(ToolbarAction.ReportPhishing.value),
                    ToolbarAction.enumOf(ToolbarAction.MoveToSpam.value),
                    ToolbarAction.enumOf("unknown2")
                )
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `adds an action from the selection when selected`() = runTest {
        // Given
        val mobileSettingsMock = MobileSettings(
            listToolbar = null,
            messageToolbar = null,
            conversationToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam
                )
                    .map { ToolbarAction.enumOf(it.value) }
            )
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettingsMock
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            awaitItem()
            repo.toggleSelection(ToolbarAction.MoveToArchive.value, tab = SettingsToolbarType.Message, toggled = true)

            // Then
            val expected = expectedDefaultPreference(
                convMode = true,
                conversationActions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print,
                    ToolbarAction.MoveToSpam,
                    ToolbarAction.MoveToArchive
                ).stringEnums()
            )
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    @Test
    fun `resets to defaults when reset`() = runTest {
        // Given
        val mobileSettingsMock = MobileSettings(
            listToolbar = ActionsToolbarSetting(
                isCustom = false,
                actions = listOf(
                    ToolbarAction.ReportPhishing,
                    ToolbarAction.MoveTo,
                    ToolbarAction.Print
                )
                    .map { ToolbarAction.enumOf(it.value) }
            ),
            messageToolbar = null,
            conversationToolbar = null
        )
        val settingsMock = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns mobileSettingsMock
        }
        val resp = DataResult.Success(ResponseSource.Remote, settingsMock)
        coEvery { mailSettingsRepository.getMailSettingsFlow(any(), any()) } returns flowOf(resp)

        // When
        repo.inMemoryPreferences().test {
            awaitItem()
            repo.resetToDefault(tab = SettingsToolbarType.Inbox)

            // Then
            val expected = expectedDefaultPreference(convMode = true)
            assertEquals(expected, awaitItem().getOrNull())
        }
    }

    private fun List<ToolbarAction>.stringEnums() = map { ToolbarAction.enumOf(it.value) }

    private fun expectedDefaultPreference(
        convMode: Boolean,
        messageActions: List<StringEnum<ToolbarAction>>? = null,
        conversationActions: List<StringEnum<ToolbarAction>>? = null,
        mailboxActions: List<StringEnum<ToolbarAction>>? = null
    ) = ToolbarActionsPreference(
        messageToolbar = expectedActions(
            messageActions, Defaults.MessageConversationActions, Defaults.AllMessageActions
        ),
        conversationToolbar = expectedActions(
            conversationActions, Defaults.MessageConversationActions, Defaults.AllConversationActions
        ),
        listToolbar = expectedActions(mailboxActions, Defaults.MailboxActions, Defaults.AllMailboxActions),
        isConversationMode = convMode
    )

    private fun expectedActions(
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
}
