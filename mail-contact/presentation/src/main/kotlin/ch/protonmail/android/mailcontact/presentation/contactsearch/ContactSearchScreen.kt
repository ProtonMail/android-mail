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

package ch.protonmail.android.mailcontact.presentation.contactsearch

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactSearchUiModel
import ch.protonmail.android.uicomponents.SearchView
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.LabelId

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ContactSearchScreen(actions: ContactSearchScreen.Actions, viewModel: ContactSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = rememberAsState(flow = viewModel.state, initial = ContactSearchViewModel.initialState).value

    Scaffold(
        topBar = {
            ContactSearchTopBar(
                Modifier,
                actions = actions,
                state = state,
                onSearchValueChange = {
                    viewModel.submit(ContactSearchViewAction.OnSearchValueChanged(it))
                },
                onSearchValueClear = {
                    viewModel.submit(ContactSearchViewAction.OnSearchValueCleared)
                }
            )
        },
        content = { _ ->
            ContactSearchContent(
                state = state,
                actions = ContactSearchContent.Actions(
                    onContactClick = {
                        actions.onContactSelected(it)
                    },
                    onContactGroupClick = {
                        actions.onContactGroupSelected(it)
                    }
                )
            )
        }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        actions.onClose
    }
}

@Composable
fun ContactSearchContent(
    modifier: Modifier = Modifier,
    state: ContactSearchState,
    actions: ContactSearchContent.Actions
) {

    if (state.uiModels?.isEmpty() == true) {
        NoResultsContent()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        state.uiModels?.let {
            items(state.uiModels) {
                ContactSearchItem(
                    contactSearchUiModel = it,
                    actions = actions
                )
            }
        }
    }
}

@Composable
fun NoResultsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(id = R.string.contact_search_no_results),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.defaultSmallWeak
        )
    }
}

@Composable
fun ContactSearchItem(
    modifier: Modifier = Modifier,
    actions: ContactSearchContent.Actions,
    contactSearchUiModel: ContactSearchUiModel
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = {
                    when (contactSearchUiModel) {
                        is ContactSearchUiModel.Contact -> actions.onContactClick(contactSearchUiModel.id)
                        is ContactSearchUiModel.ContactGroup -> actions.onContactGroupClick(contactSearchUiModel.id)
                    }
                }
            )
            .padding(start = ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactSearchAvatar(contactSearchUiModel)

        Column(
            modifier = Modifier
                .padding(
                    start = ProtonDimens.ListItemTextStartPadding,
                    top = ProtonDimens.ListItemTextStartPadding,
                    bottom = ProtonDimens.ListItemTextStartPadding,
                    end = ProtonDimens.DefaultSpacing
                )
                .weight(1f)
        ) {
            Text(
                text = when (contactSearchUiModel) {
                    is ContactSearchUiModel.Contact -> contactSearchUiModel.name
                    is ContactSearchUiModel.ContactGroup -> contactSearchUiModel.name
                },
                style = ProtonTheme.typography.defaultNorm
            )
            when (contactSearchUiModel) {
                is ContactSearchUiModel.Contact -> Text(
                    text = contactSearchUiModel.email,
                    style = ProtonTheme.typography.defaultSmallWeak
                )

                is ContactSearchUiModel.ContactGroup -> Text(
                    text = pluralStringResource(
                        R.plurals.contact_search_contact_group_counter,
                        contactSearchUiModel.emailCount,
                        contactSearchUiModel.emailCount
                    ),
                    style = ProtonTheme.typography.defaultSmallWeak
                )
            }

        }
    }
}

@Composable
private fun ContactSearchAvatar(contactSearchUiModel: ContactSearchUiModel) {
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = MailDimens.AvatarMinSize,
                minHeight = MailDimens.AvatarMinSize
            )
            .background(
                color = when (contactSearchUiModel) {
                    is ContactSearchUiModel.Contact -> ProtonTheme.colors.interactionWeakNorm
                    is ContactSearchUiModel.ContactGroup -> contactSearchUiModel.color
                },
                shape = ProtonTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        when (contactSearchUiModel) {
            is ContactSearchUiModel.Contact -> {
                Text(
                    textAlign = TextAlign.Center,
                    text = contactSearchUiModel.initials
                )
            }

            is ContactSearchUiModel.ContactGroup -> {
                Icon(
                    modifier = Modifier.size(ProtonDimens.SmallIconSize),
                    painter = painterResource(id = R.drawable.ic_proton_users_filled),
                    tint = Color.White,
                    contentDescription = NO_CONTENT_DESCRIPTION
                )

            }
        }
    }
}

@Composable
fun ContactSearchTopBar(
    modifier: Modifier,
    actions: ContactSearchScreen.Actions,
    state: ContactSearchState,
    onSearchValueChange: (String) -> Unit,
    onSearchValueClear: () -> Unit
) {

    ProtonTopAppBar(
        modifier = modifier,
        title = {
            SearchView(
                SearchView.Parameters(
                    initialSearchValue = state.searchValue,
                    searchPlaceholderText = R.string.contact_search_placeholder,
                    closeButtonContentDescription = R.string.contact_search_content_description
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                actions = SearchView.Actions(
                    onClearSearchQuery = { onSearchValueClear() },
                    onSearchQuerySubmit = {},
                    onSearchQueryChanged = { onSearchValueChange(it) }
                )
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier,
                onClick = actions.onClose
            ) {
                androidx.compose.material.Icon(
                    painter = painterResource(id = R.drawable.ic_proton_arrow_left),
                    contentDescription = stringResource(id = R.string.contact_search_arrow_back_content_description)
                )
            }
        }
    )
}

object ContactSearchScreen {

    data class Actions(
        val onContactSelected: (ContactId) -> Unit,
        val onContactGroupSelected: (LabelId) -> Unit,
        val onClose: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onContactSelected = { },
                onContactGroupSelected = { },
                onClose = {}
            )
        }
    }
}

object ContactSearchContent {

    data class Actions(
        val onContactClick: (ContactId) -> Unit,
        val onContactGroupClick: (LabelId) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onContactClick = {},
                onContactGroupClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ManageMembersContentPreview() {
    ContactSearchContent(
        state = ContactSearchState(
            uiModels = emptyList()
        ),
        actions = ContactSearchContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyManageMembersContentPreview() {
    ContactSearchContent(
        state = ContactSearchState(
            uiModels = emptyList()
        ),
        actions = ContactSearchContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ManageMembersTopBarPreview() {
    ContactSearchTopBar(
        Modifier,
        actions = ContactSearchScreen.Actions.Empty,
        state = ContactSearchState(),
        onSearchValueChange = {},
        onSearchValueClear = {}
    )
}
