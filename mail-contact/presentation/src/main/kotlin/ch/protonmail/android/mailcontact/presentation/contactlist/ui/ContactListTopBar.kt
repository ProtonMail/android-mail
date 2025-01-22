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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactCreate
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactImport
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun ContactListTopBar(
    modifier: Modifier = Modifier,
    actions: ContactListTopBar.Actions,
    isAddButtonVisible: Boolean
) {
    ProtonTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(text = stringResource(id = R.string.contact_list_title))
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            IconButton(onClick = actions.onSearchClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_magnifier),
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.search_contacts_description)
                )
            }

            // hasOneCreateOption can be deleted once we get rid of ContactFeatureFlags
            val hasOneCreateOption = ContactCreate.value || ContactImport.value
            if (isAddButtonVisible && hasOneCreateOption) {
                IconButton(onClick = actions.onAddClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_plus),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.add_contact_content_description)
                    )
                }
            }
        }
    )
}

internal object ContactListTopBar {

    data class Actions(
        val onBackClick: () -> Unit,
        val onAddClick: () -> Unit,
        val onSearchClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onAddClick = {},
                onSearchClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactListTopBarPreview() {
    ContactListTopBar(
        actions = ContactListTopBar.Actions.Empty,
        isAddButtonVisible = true
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactListTopBarPreview() {
    ContactListTopBar(
        actions = ContactListTopBar.Actions.Empty,
        isAddButtonVisible = false
    )
}
