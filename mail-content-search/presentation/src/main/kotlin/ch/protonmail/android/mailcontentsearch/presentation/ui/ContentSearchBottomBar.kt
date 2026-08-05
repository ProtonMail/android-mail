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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.FloatingBottomToolbar

@Composable
internal fun ContentSearchBottomToolbar(
    bottomBarState: BottomBarState,
    actions: ContentSearchActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FloatingBottomToolbar(
            state = bottomBarState,
            viewActionCallbacks = remember(actions) { actions.toBottomBarCallbacks() }
        )
    }
}

private fun ContentSearchActions.toBottomBarCallbacks(): BottomActionBar.Actions = BottomActionBar.Actions.Empty.copy(
    onMarkRead = onMarkRead,
    onMarkUnread = onMarkUnread,
    onStar = onStarSelection,
    onUnstar = onUnStarSelection,
    onTrash = onTrash,
    onArchive = onArchive,
    onSpam = onSpam,
    onDelete = onDelete,
    onMove = onRequestMoveTo,
    onLabel = onRequestLabelAs,
    onMore = onRequestMore
)
