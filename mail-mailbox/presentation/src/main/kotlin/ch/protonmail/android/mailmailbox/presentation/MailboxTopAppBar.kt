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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
fun MailboxTopAppBar(
    modifier: Modifier = Modifier,
    state: MailboxTopAppBarState,
    onOpenMenu: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onExitSearchMode: () -> Unit,
    onTitleClick: () -> Unit,
    onEnterSearchMode: () -> Unit,
    onSearch: (query: String) -> Unit,
    onOpenCompose: () -> Unit
) {

    val uiModel = when (state) {
        MailboxTopAppBarState.Loading -> UiModel.Empty
        is MailboxTopAppBarState.Data.DefaultMode -> UiModel(
            title = state.currentLabelName,
            navigationIconRes = R.drawable.ic_proton_hamburger,
            navigationIconContentDescription = stringResource(id = R.string.mailbox_toolbar_menu_button_content_description),
            shouldShowActions = true
        )
        is MailboxTopAppBarState.Data.SelectionMode -> UiModel(
            title = stringResource(id = R.string.mailbox_toolbar_selected_count, state.selectedCount),
            navigationIconRes = R.drawable.ic_proton_arrow_left,
            navigationIconContentDescription =
            stringResource(id = R.string.mailbox_toolbar_exit_selection_mode_button_content_description),
            shouldShowActions = false
        )
        is MailboxTopAppBarState.Data.SearchMode -> UiModel.Empty.copy(
            navigationIconRes = R.drawable.ic_proton_arrow_left,
        )
    }

    val onNavigationIconClick = when (state) {
        MailboxTopAppBarState.Loading, is MailboxTopAppBarState.Data.DefaultMode -> onOpenMenu
        is MailboxTopAppBarState.Data.SelectionMode -> onExitSelectionMode
        is MailboxTopAppBarState.Data.SearchMode -> onExitSearchMode
    }

    ProtonTopAppBar(
        modifier = modifier,
        title = { Text(modifier = Modifier.clickable(onClick = onTitleClick), text = uiModel.title) },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(id = uiModel.navigationIconRes),
                    contentDescription = uiModel.navigationIconContentDescription
                )
            }
        },
        actions = {

            if (uiModel.shouldShowActions) {
                IconButton(onClick = onEnterSearchMode) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_magnifier),
                        contentDescription = stringResource(id = R.string.mailbox_toolbar_search_button_content_description)
                    )
                }
                IconButton(onClick = onOpenCompose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_pen_square),
                        contentDescription = stringResource(id = R.string.mailbox_toolbar_compose_button_content_description)
                    )
                }
            }
        }
    )
}

private data class UiModel(
    val title: String,
    @DrawableRes val navigationIconRes: Int,
    val navigationIconContentDescription: String,
    val shouldShowActions: Boolean
) {
    
    companion object {
        
        val Empty = UiModel(
            title = EMPTY_STRING,
            navigationIconRes = R.drawable.ic_proton_hamburger,
            navigationIconContentDescription = EMPTY_STRING,
            shouldShowActions = false
        )
    }
}

@Composable
@Preview
fun LoadingMailboxTopAppBarPreview() {
    val state = MailboxTopAppBarState.Loading
    
    MailboxTopAppBar(
        state = state,
        onOpenMenu = {},
        onExitSelectionMode = {},
        onExitSearchMode = {},
        onTitleClick = {},
        onEnterSearchMode = {},
        onSearch = {},
        onOpenCompose = {}
    )
}
