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

package ch.protonmail.android.mailcontact.presentation.contactlist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactlist.ContactListState
import me.proton.core.compose.theme.ProtonDimens

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactGroupsScreenContent(
    modifier: Modifier = Modifier,
    state: ContactListState.Loaded.Data,
    actions: ContactListScreen.Actions,
    onNewGroupClick: () -> Unit
) {
    if (state.contactGroups.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = ProtonDimens.ListItemTextStartPadding)
        ) {
            items(state.contactGroups) { contactGroupItemUiModel ->
                ContactListGroupItem(
                    modifier = Modifier.animateItemPlacement(),
                    contact = contactGroupItemUiModel,
                    actions = actions
                )
            }
        }
    } else {
        ContactEmptyDataScreen(
            iconResId = R.drawable.ic_proton_users_plus,
            title = stringResource(R.string.no_contact_groups),
            description = stringResource(R.string.no_contact_groups_description),
            buttonText = stringResource(R.string.add_contact_group),
            showAddButton = true,
            onAddClick = onNewGroupClick
        )
    }
}
