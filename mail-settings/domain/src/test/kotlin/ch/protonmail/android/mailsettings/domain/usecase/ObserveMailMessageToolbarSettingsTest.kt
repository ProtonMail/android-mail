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

package ch.protonmail.android.mailsettings.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.mailsettings.domain.entity.ActionsToolbarSetting
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MobileSettings
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMailMessageToolbarSettingsTest {

    private val userId = UserIdSample.Primary

    private val repo = mockk<MailSettingsRepository>()

    private val useCase by lazy {
        ObserveMailMessageToolbarSettings(repo)
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `empty actions are returned when mail settings is empty`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns null
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(null, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `conversation actions are returned when mail settings is not empty and conversation mode is on`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = null,
                messageToolbar = null,
                conversationToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.Print,
                        ToolbarAction.MoveTo,
                        ToolbarAction.MoveToSpam
                    ).map { ToolbarAction.enumOf(it.value) }
                )
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(
                listOf(
                    Action.Print,
                    Action.Move,
                    Action.Spam
                ),
                awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun `conversation actions do not include reply, reply all and forward`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.ConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = null,
                messageToolbar = null,
                conversationToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.ReplyOrReplyAll,
                        ToolbarAction.Forward,
                        ToolbarAction.MoveToSpam
                    ).map { ToolbarAction.enumOf(it.value) }
                )
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(
                listOf(
                    Action.Spam
                ),
                awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun `message actions are returned when mail settings is not empty and conversation mode is off`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.NoConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = null,
                messageToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.ReportPhishing,
                        ToolbarAction.StarOrUnstar,
                        ToolbarAction.MoveToArchive
                    ).map { ToolbarAction.enumOf(it.value) }
                ),
                conversationToolbar = null
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(
                listOf(
                    Action.ReportPhishing,
                    Action.Star,
                    Action.Archive
                ),
                awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun `null actions are returned when mail settings is not empty and no actions are supported`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.NoConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = null,
                messageToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.enumOf("unsupported1"),
                        ToolbarAction.enumOf("unsupported2"),
                        ToolbarAction.enumOf("unsupported3")
                    )
                ),
                conversationToolbar = null
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(
                null,
                awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun `filters out unsupported actions`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.NoConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = null,
                messageToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.enumOf("unsupported1"),
                        ToolbarAction.enumOf("toggle_star"),
                        ToolbarAction.enumOf("unsupported3")
                    )
                ),
                conversationToolbar = null
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = false).test {
            // Then
            assertEquals(
                listOf(
                    Action.Star
                ),
                awaitItem()
            )
            awaitComplete()
        }
    }

    @Test
    fun `mailbox actions are returned when isMailbox is true`() = runTest {
        val settings = mockk<MailSettings> {
            every { this@mockk.viewMode } returns ViewMode.enumOf(ViewMode.NoConversationGrouping.value)
            every { this@mockk.mobileSettings } returns MobileSettings(
                listToolbar = ActionsToolbarSetting(
                    isCustom = true,
                    actions = listOf(
                        ToolbarAction.LabelAs,
                        ToolbarAction.Forward,
                        ToolbarAction.ViewMessageInLightMode
                    ).map { ToolbarAction.enumOf(it.value) }
                ),
                messageToolbar = null,
                conversationToolbar = null
            )
        }

        // Given
        every { repo.getMailSettingsFlow(userId) } returns flowOf(DataResult.Success(ResponseSource.Remote, settings))

        // When
        useCase.invoke(userId, isMailBox = true).test {
            // Then
            assertEquals(
                listOf(
                    Action.Label,
                    Action.Forward,
                    Action.ViewInLightMode
                ),
                awaitItem()
            )
            awaitComplete()
        }
    }
}
