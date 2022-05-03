/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation

import androidx.compose.foundation.clickable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
internal fun MailboxTopAppBar(
    modifier: Modifier = Modifier,
    state: MailboxTopAppBarState,
    onOpenMenu: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onCloseSearchMode: () -> Unit,
    onTitleClick: () -> Unit,
    onOpenSearchMode: () -> Unit,
    onSearch: (query: String) -> Unit,
    onOpenCompose: () -> Unit
) {

    val (uiModel, onNavigationIconClick) = when (state) {
        MailboxTopAppBarState.Loading -> UiModel.Empty to {}
        is MailboxTopAppBarState.Data.DefaultMode -> UiModel(
            title = state.currentLabelName,
            navigationIcon = Icons.Filled.Menu,
            navigationIconContentDescription = stringResource(id = R.string.x_toolbar_menu_button_content_description),
            shouldShowActions = true
        ) to onOpenMenu
        is MailboxTopAppBarState.Data.SelectionMode -> UiModel(
            title = stringResource(id = R.string.mailbox_toolbar_selected_count, state.selectedCount),
            navigationIcon = Icons.Filled.ArrowBack,
            navigationIconContentDescription =
            stringResource(id = R.string.mailbox_toolbar_close_selection_mode_button_content_description),
            shouldShowActions = false
        ) to onCloseSelectionMode
        is MailboxTopAppBarState.Data.SearchMode -> UiModel.Empty.copy(
            navigationIcon = Icons.Filled.ArrowBack
        ) to onCloseSearchMode
    }
    
    ProtonTopAppBar(
        modifier = modifier,
        title = { Text(modifier = Modifier.clickable(onClick = onTitleClick), text = uiModel.title) },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = uiModel.navigationIcon,
                    contentDescription = uiModel.navigationIconContentDescription
                )
            }
        },
        actions = {
            if (uiModel.shouldShowActions) {
                Text(text = "TODO")
            }
        }
    )
}

@Composable
private fun DefaultMailboxTopAppBar(
    modifier: Modifier = Modifier,
    labelName: String,
    onOpenMenu: () -> Unit
) {
    ProtonTopAppBar(
        modifier = modifier,
        title = { Text(text = labelName) },
        navigationIcon = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.x_toolbar_menu_button_content_description)
                )
            }
        }
    )
}

private data class UiModel(
    val title: String,
    val navigationIcon: ImageVector,
    val navigationIconContentDescription: String,
    val shouldShowActions: Boolean
) {
    
    companion object {
        
        val Empty = UiModel(
            title = EMPTY_STRING,
            navigationIcon = Icons.Filled.Menu,
            navigationIconContentDescription = EMPTY_STRING,
            shouldShowActions = false
        )
    }
}

@Composable
@Preview
private fun LoadingMailboxTopAppBarPreview() {
    val state = MailboxTopAppBarState.Loading
    
    MailboxTopAppBar(
        state = state,
        onOpenMenu = {},
        onCloseSelectionMode = {},
        onCloseSearchMode = {},
        onTitleClick = {},
        onOpenSearchMode = {},
        onSearch = {},
        onOpenCompose = {}
    )
}
