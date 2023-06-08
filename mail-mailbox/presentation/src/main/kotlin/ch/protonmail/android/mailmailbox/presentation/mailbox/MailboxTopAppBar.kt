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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
fun MailboxTopAppBar(
    modifier: Modifier = Modifier,
    state: MailboxTopAppBarState,
    actions: MailboxTopAppBar.Actions
) {
    val uiModel = when (state) {
        is MailboxTopAppBarState.Loading -> UiModel.Empty
        is MailboxTopAppBarState.Data.DefaultMode -> UiModel(
            title = state.currentLabelName.string(),
            navigationIconRes = R.drawable.ic_proton_hamburger,
            navigationIconContentDescription = stringResource(
                id = R.string.mailbox_toolbar_menu_button_content_description
            ),
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
            navigationIconRes = R.drawable.ic_proton_arrow_left
        )
    }

    val onNavigationIconClick = when (state) {
        is MailboxTopAppBarState.Loading, is MailboxTopAppBarState.Data.DefaultMode -> actions.onOpenMenu
        is MailboxTopAppBarState.Data.SelectionMode -> actions.onExitSelectionMode
        is MailboxTopAppBarState.Data.SearchMode -> actions.onExitSearchMode
    }

    ProtonTopAppBar(
        modifier = modifier.testTag(MailboxTopAppBarTestTags.RootItem),
        title = {
            Text(
                modifier = Modifier
                    .testTag(MailboxTopAppBarTestTags.LocationLabel)
                    .clickable(onClick = actions.onTitleClick),
                text = uiModel.title
            )
        },
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
                IconButton(
                    modifier = Modifier.testTag(MailboxTopAppBarTestTags.SearchButton),
                    onClick = actions.onEnterSearchMode
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_magnifier),
                        contentDescription = stringResource(
                            id = R.string.mailbox_toolbar_search_button_content_description
                        )
                    )
                }
                IconButton(
                    modifier = Modifier.testTag(MailboxTopAppBarTestTags.ComposerButton),
                    onClick = actions.onOpenComposer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_pen_square),
                        contentDescription = stringResource(
                            id = R.string.mailbox_toolbar_compose_button_content_description
                        )
                    )
                }
            }
        }
    )
}

object MailboxTopAppBar {

    data class Actions(
        val onOpenMenu: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onExitSearchMode: () -> Unit,
        val onTitleClick: () -> Unit,
        val onEnterSearchMode: () -> Unit,
        val onSearch: (query: String) -> Unit,
        val onOpenComposer: () -> Unit
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
    val state = MailboxTopAppBarState.Loading(isComposerDisabled = false)

    MailboxTopAppBar(
        state = state,
        actions = MailboxTopAppBar.Actions(
            onOpenMenu = {},
            onExitSelectionMode = {},
            onExitSearchMode = {},
            onTitleClick = {},
            onEnterSearchMode = {},
            onSearch = {},
            onOpenComposer = {}
        )
    )
}

object MailboxTopAppBarTestTags {

    const val RootItem = "TopAppBarRootItem"
    const val LocationLabel = "LocationLabel"
    const val SearchButton = "SearchButton"
    const val ComposerButton = "ComposerButton"
}
