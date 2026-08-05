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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomBarActions
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BottomBarStateFactoryTest {

    private val userId = UserIdSample.Primary
    private val labelId = SystemLabelId.Inbox.labelId
    private val itemIds = listOf(MailboxItemId("1"), MailboxItemId("2"))

    private val getBottomBarActions = mockk<GetBottomBarActions>()
    private val actionUiModelMapper = ActionUiModelMapper()

    private val factory = BottomBarStateFactory(getBottomBarActions, actionUiModelMapper)

    @Test
    fun `should map the resolved actions to a shown state`() = runTest {
        coEvery {
            getBottomBarActions(userId, labelId, itemIds, ViewMode.NoConversationGrouping)
        } returns listOf(Action.Archive, Action.Trash).right()

        val result = factory.create(userId, labelId, itemIds, ViewMode.NoConversationGrouping)

        val expectedActions = listOf(Action.Archive, Action.Trash).map { actionUiModelMapper.toUiModel(it) }
        assertEquals(
            BottomBarState.Data.Shown(target = BottomBarTarget.Mailbox, actions = expectedActions.toImmutableList()),
            result
        )
    }

    @Test
    fun `should hide rather than show an empty toolbar when there are no actions`() = runTest {
        coEvery {
            getBottomBarActions(userId, labelId, itemIds, ViewMode.ConversationGrouping)
        } returns emptyList<Action>().right()

        val result = factory.create(userId, labelId, itemIds, ViewMode.ConversationGrouping)

        assertEquals(
            BottomBarState.Data.Hidden(target = BottomBarTarget.Mailbox, actions = persistentListOf()),
            result
        )
    }

    @Test
    fun `should surface a failure to load the actions`() = runTest {
        coEvery {
            getBottomBarActions(userId, labelId, itemIds, ViewMode.NoConversationGrouping)
        } returns DataError.Local.NoDataCached.left()

        val result = factory.create(userId, labelId, itemIds, ViewMode.NoConversationGrouping)

        assertEquals(BottomBarState.Error.FailedLoadingActions, result)
    }
}
