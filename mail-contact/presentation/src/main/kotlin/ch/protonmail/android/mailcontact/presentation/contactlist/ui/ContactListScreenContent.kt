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

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcontact.presentation.contactlist.ContactListState
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.contactGroupSampleData
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.contactSampleData
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.headerSampleData

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactListScreenContent(
    modifier: Modifier = Modifier,
    state: ContactListState.Loaded.Data,
    actions: ContactListScreen.Actions
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.contacts) { contactListItemUiModel ->
            when (contactListItemUiModel) {
                is ContactListItemUiModel.Header -> {
                    ContactListHeaderItem(
                        modifier = Modifier.animateItemPlacement(),
                        header = contactListItemUiModel
                    )
                }

                is ContactListItemUiModel.Contact -> {
                    ContactListItem(
                        modifier = Modifier.animateItemPlacement(),
                        contact = contactListItemUiModel,
                        actions = actions
                    )
                }
            }
        }
    }
}


@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactListScreenPreview() {
    ContactListScreenContent(
        state = ContactListState.Loaded.Data(
            contacts = listOf(
                headerSampleData,
                contactSampleData,
                contactSampleData,
                contactSampleData
            ),
            contactGroups = listOf(
                contactGroupSampleData,
                contactGroupSampleData,
                contactGroupSampleData
            )
        ),
        actions = ContactListScreen.Actions.Empty
    )
}
