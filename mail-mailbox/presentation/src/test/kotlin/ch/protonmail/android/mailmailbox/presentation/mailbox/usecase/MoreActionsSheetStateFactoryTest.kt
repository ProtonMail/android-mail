/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomSheetActions
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MoreActionsSheetStateFactoryTest {

    private val userId = UserIdSample.Primary
    private val labelId = SystemLabelId.Inbox.labelId
    private val itemIds = listOf(MailboxItemId("1"), MailboxItemId("2"), MailboxItemId("3"))

    private val getBottomSheetActions = mockk<GetBottomSheetActions>()
    private val actionUiModelMapper = ActionUiModelMapper()

    private val factory = MoreActionsSheetStateFactory(getBottomSheetActions, actionUiModelMapper)

    @Test
    fun `should map the resolved actions to ui models`() = runTest {
        val actions = AllBottomBarActions(
            hiddenActions = listOf(Action.Spam),
            visibleActions = listOf(Action.Archive, Action.Trash)
        )
        coEvery {
            getBottomSheetActions(userId, labelId, itemIds, ViewMode.NoConversationGrouping)
        } returns actions.right()

        val result = factory.create(userId, labelId, itemIds, ViewMode.NoConversationGrouping)

        val actionData = result.getOrNull()!!
        assertEquals(
            listOf(actionUiModelMapper.toUiModel(Action.Spam)),
            actionData.hiddenActionUiModels
        )
        assertEquals(
            listOf(actionUiModelMapper.toUiModel(Action.Archive), actionUiModelMapper.toUiModel(Action.Trash)),
            actionData.visibleActionUiModels
        )
        assertEquals(actionUiModelMapper.toUiModel(Action.CustomizeToolbar), actionData.customizeToolbarActionUiModel)
    }

    @Test
    fun `should report the number of items acted on as the selected count`() = runTest {
        coEvery {
            getBottomSheetActions(userId, labelId, itemIds, ViewMode.ConversationGrouping)
        } returns AllBottomBarActions(hiddenActions = emptyList(), visibleActions = emptyList()).right()

        val result = factory.create(userId, labelId, itemIds, ViewMode.ConversationGrouping)

        assertEquals(itemIds.size, result.getOrNull()!!.selectedCount)
    }

    @Test
    fun `should propagate the error when the actions cannot be resolved`() = runTest {
        coEvery {
            getBottomSheetActions(userId, labelId, itemIds, ViewMode.NoConversationGrouping)
        } returns DataError.Local.NoDataCached.left()

        val result = factory.create(userId, labelId, itemIds, ViewMode.NoConversationGrouping)

        assertTrue(result.isLeft())
        assertEquals(DataError.Local.NoDataCached, result.leftOrNull())
    }
}
