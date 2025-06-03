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
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBarTestTags.NavigationButton
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingMailButton
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import ch.protonmail.android.uicomponents.SearchView
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.util.kotlin.EMPTY_STRING

@Composable
fun MailboxTopAppBar(
    modifier: Modifier = Modifier,
    state: MailboxTopAppBarState,
    upgradeStorageState: UpgradeStorageState,
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
            shouldShowActions = true,
            notificationDotVisible = upgradeStorageState.notificationDotVisible
        )

        is MailboxTopAppBarState.Data.SelectionMode -> UiModel(
            title = pluralStringResource(
                id = R.plurals.mailbox_toolbar_selected_count,
                state.selectedCount,
                state.selectedCount
            ),
            navigationIconRes = R.drawable.ic_proton_arrow_left,
            navigationIconContentDescription =
            stringResource(id = R.string.mailbox_toolbar_exit_selection_mode_button_content_description),
            shouldShowActions = false
        )

        is MailboxTopAppBarState.Data.SearchMode -> UiModel.Empty.copy(
            navigationIconRes = R.drawable.ic_proton_arrow_left,
            navigationIconContentDescription = stringResource(
                id = R.string.mailbox_toolbar_exit_search_mode_content_description
            ),
            shouldShowActions = false,
            searchQuery = state.searchQuery
        )
    }

    val onNavigationIconClick = when (state) {
        is MailboxTopAppBarState.Loading, is MailboxTopAppBarState.Data.DefaultMode -> actions.onOpenMenu
        is MailboxTopAppBarState.Data.SelectionMode -> actions.onExitSelectionMode
        is MailboxTopAppBarState.Data.SearchMode -> actions.onExitSearchMode
    }

    Crossfade(targetState = state, label = "") { topBarState ->

        if (topBarState is MailboxTopAppBarState.Data.SearchMode) {
            TopAppBarInSearchMode(
                modifier = modifier,
                uiModel = uiModel,
                actions = actions
            )
        } else {
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
                    if (state !is MailboxTopAppBarState.Loading) {
                        NavigationIcon(
                            uiModel = uiModel,
                            onNavigationIconClick = onNavigationIconClick
                        )
                    }
                },
                actions = {
                    if (uiModel.shouldShowActions) {
                        UpsellingMailButton(onClick = actions.onNavigateToStandaloneUpselling)
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
    }
}

@Composable
private fun TopAppBarInSearchMode(
    modifier: Modifier = Modifier,
    uiModel: UiModel,
    actions: MailboxTopAppBar.Actions
) {

    ProtonTopAppBar(
        modifier = modifier.testTag(MailboxTopAppBarTestTags.RootItem),
        title = {
            SearchView(
                SearchView.Parameters(
                    initialSearchValue = uiModel.searchQuery,
                    searchPlaceholderText = R.string.mailbox_search_placeholder_text,
                    closeButtonContentDescription =
                    R.string.mailbox_toolbar_searchview_clear_search_query_content_description
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(MailboxTopAppBarTestTags.SearchView),
                actions = SearchView.Actions(
                    onClearSearchQuery = {},
                    onSearchQuerySubmit = actions.onSearch,
                    onSearchQueryChanged = {}
                )
            )
        },
        navigationIcon = {
            NavigationIcon(
                uiModel = uiModel,
                onNavigationIconClick = actions.onExitSearchMode
            )
        },
        actions = {}
    )
}

@Composable
private fun NavigationIcon(uiModel: UiModel, onNavigationIconClick: () -> Unit) {
    IconButton(
        modifier = Modifier.testTag(NavigationButton),
        onClick = onNavigationIconClick
    ) {
        BadgedBox(
            badge = {
                if (uiModel.notificationDotVisible) {
                    Badge(
                        containerColor = ProtonTheme.colors.notificationError,
                        modifier = Modifier
                            .size(MailDimens.NotificationDotSize)
                            .offset(BadgeOffsetValues.X.dp, BadgeOffsetValues.Y.dp)
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(id = uiModel.navigationIconRes),
                contentDescription = uiModel.navigationIconContentDescription
            )
        }
    }
}

object MailboxTopAppBar {

    data class Actions(
        val onOpenMenu: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onExitSearchMode: () -> Unit,
        val onTitleClick: () -> Unit,
        val onEnterSearchMode: () -> Unit,
        val onSearch: (query: String) -> Unit,
        val onOpenComposer: () -> Unit,
        val onNavigateToStandaloneUpselling: (type: UpsellingVisibility) -> Unit,
        val onOpenUpsellingPage: () -> Unit,
        val onCloseUpsellingPage: () -> Unit
    )
}

private data class UiModel(
    val title: String,
    @DrawableRes val navigationIconRes: Int,
    val navigationIconContentDescription: String,
    val shouldShowActions: Boolean,
    val notificationDotVisible: Boolean = false,
    val searchQuery: String = EMPTY_STRING
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
    val upgradeStorageState = UpgradeStorageState(notificationDotVisible = false)

    MailboxTopAppBar(
        state = state,
        upgradeStorageState = upgradeStorageState,
        actions = MailboxTopAppBar.Actions(
            onOpenMenu = {},
            onExitSelectionMode = {},
            onExitSearchMode = {},
            onTitleClick = {},
            onEnterSearchMode = {},
            onSearch = {},
            onOpenComposer = {},
            onNavigateToStandaloneUpselling = {},
            onOpenUpsellingPage = {},
            onCloseUpsellingPage = {}
        )
    )
}

private object BadgeOffsetValues {

    const val X = 4
    const val Y = -5
}

object MailboxTopAppBarTestTags {

    const val RootItem = "TopAppBarRootItem"
    const val NavigationButton = "NavigationButton"
    const val LocationLabel = "LocationLabel"
    const val SearchButton = "SearchButton"
    const val ComposerButton = "ComposerButton"
    const val SearchView = "SearchView"
}
