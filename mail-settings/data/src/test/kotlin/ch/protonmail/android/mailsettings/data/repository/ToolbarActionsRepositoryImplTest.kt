/*
 * Copyright (c) 2025 Proton Technologies AG
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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailsettings.data.local.ToolbarActionSettingsDataSource
import ch.protonmail.android.mailsettings.domain.model.ToolbarType
import ch.protonmail.android.mailsettings.domain.repository.ToolbarActionsRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uniffi.mail_uniffi.MobileAction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ToolbarActionsRepositoryImplTest(
    private val toolbarType: ToolbarType,
    private val mobileActions: List<MobileAction>,
    private val domainActions: List<Action>,
    @Suppress("Unused") private val testName: String
) {

    private val dataSource = mockk<ToolbarActionSettingsDataSource>()
    private lateinit var repo: ToolbarActionsRepository

    @BeforeTest
    fun setup() {
        repo = ToolbarActionsRepositoryImpl(dataSource)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `should call the appropriate data source method when actions are requested`() = runTest {
        // Given
        setupMockForGetActions(toolbarType, mobileActions)

        // When
        val result = repo.getToolbarActions(userId, toolbarType)

        // Then
        assertEquals(domainActions.right(), result)
        verifyGetActionsCall(toolbarType)
        confirmVerified(dataSource)
    }

    @Test
    fun `should call the appropriate data source method when all actions are requested`() = runTest {
        // Given
        setupMockForGetAllActions(toolbarType, mobileActions)

        // When
        val result = repo.getAllActions(toolbarType)

        // Then
        assertEquals(domainActions, result)
        verifyGetAllActionsCall(toolbarType)
        confirmVerified(dataSource)
    }

    @Test
    fun `should call the appropriate data source method when a save is requested`() = runTest {
        // Given
        setupMockForSaveActions(toolbarType, mobileActions)

        // When
        val result = repo.saveActions(userId, toolbarType, domainActions)

        // Then
        assertEquals(Unit.right(), result)
        verifySaveActionsCall(toolbarType, mobileActions)
        confirmVerified(dataSource)
    }

    private fun setupMockForGetActions(toolbarType: ToolbarType, mobileActions: List<MobileAction>) {
        when (toolbarType) {
            ToolbarType.List -> coEvery { dataSource.getListActions(userId) } returns mobileActions.right()
            ToolbarType.Conversation -> coEvery {
                dataSource.getConversationActions(userId)
            } returns mobileActions.right()

            ToolbarType.Message -> coEvery { dataSource.getMessageActions(userId) } returns mobileActions.right()
        }
    }

    private fun verifyGetActionsCall(toolbarType: ToolbarType) {
        when (toolbarType) {
            ToolbarType.List -> coVerify(exactly = 1) { dataSource.getListActions(userId) }
            ToolbarType.Conversation -> coVerify(exactly = 1) { dataSource.getConversationActions(userId) }
            ToolbarType.Message -> coVerify(exactly = 1) { dataSource.getMessageActions(userId) }
        }
    }

    private fun setupMockForGetAllActions(toolbarType: ToolbarType, mobileActions: List<MobileAction>) {
        when (toolbarType) {
            ToolbarType.List -> coEvery { dataSource.getAllListActions() } returns mobileActions
            ToolbarType.Conversation -> coEvery { dataSource.getAllConversationActions() } returns mobileActions
            ToolbarType.Message -> coEvery { dataSource.getAllMessageActions() } returns mobileActions
        }
    }

    private fun verifyGetAllActionsCall(toolbarType: ToolbarType) {
        when (toolbarType) {
            ToolbarType.List -> coVerify(exactly = 1) { dataSource.getAllListActions() }
            ToolbarType.Conversation -> coVerify(exactly = 1) { dataSource.getAllConversationActions() }
            ToolbarType.Message -> coVerify(exactly = 1) { dataSource.getAllMessageActions() }
        }
    }

    private fun setupMockForSaveActions(toolbarType: ToolbarType, mobileActions: List<MobileAction>) {
        when (toolbarType) {
            ToolbarType.List -> coEvery { dataSource.updateListActions(userId, mobileActions) } returns Unit.right()
            ToolbarType.Conversation -> coEvery {
                dataSource.updateConversationActions(userId, mobileActions)
            } returns Unit.right()

            ToolbarType.Message -> coEvery {
                dataSource.updateMessageActions(userId, mobileActions)
            } returns Unit.right()
        }
    }

    private fun verifySaveActionsCall(toolbarType: ToolbarType, mobileActions: List<MobileAction>) {
        when (toolbarType) {
            ToolbarType.List -> coVerify(exactly = 1) { dataSource.updateListActions(userId, mobileActions) }
            ToolbarType.Conversation -> coVerify(exactly = 1) {
                dataSource.updateConversationActions(userId, mobileActions)
            }

            ToolbarType.Message -> coVerify(exactly = 1) { dataSource.updateMessageActions(userId, mobileActions) }
        }
    }

    companion object {

        private val userId = UserId("user-id")

        @JvmStatic
        @Parameterized.Parameters(name = "{3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    ToolbarType.List,
                    listOf(MobileAction.TRASH, MobileAction.LABEL),
                    listOf(Action.Trash, Action.Label),
                    "List toolbar"
                ),
                arrayOf(
                    ToolbarType.Conversation,
                    listOf(MobileAction.ARCHIVE, MobileAction.SNOOZE),
                    listOf(Action.Archive, Action.Snooze),
                    "Conversation toolbar"
                ),
                arrayOf(
                    ToolbarType.Message,
                    listOf(MobileAction.REPORT_PHISHING, MobileAction.MOVE),
                    listOf(Action.ReportPhishing, Action.Move),
                    "Message toolbar"
                )
            )
        }
    }
}
