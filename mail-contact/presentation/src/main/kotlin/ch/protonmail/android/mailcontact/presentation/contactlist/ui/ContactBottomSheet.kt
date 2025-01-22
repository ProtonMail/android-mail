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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactCreate
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactImport
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingIcon
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
internal fun ContactBottomSheetContent(
    modifier: Modifier = Modifier,
    isContactGroupsUpsellingVisible: Boolean,
    actions: ContactBottomSheet.Actions
) {
    Column(
        modifier = modifier
            .padding(top = ProtonDimens.SmallSpacing)
            .verticalScroll(rememberScrollState())
    ) {
        if (ContactCreate.value) {
            ContactBottomSheetItem(
                modifier = Modifier,
                titleResId = R.string.new_contact,
                iconResId = R.drawable.ic_proton_user_plus,
                onClick = actions.onNewContactClick
            )
        }
        ContactBottomSheetItem(
            modifier = Modifier,
            titleResId = R.string.new_group,
            iconResId = R.drawable.ic_proton_users_plus,
            isUpsellingVisible = isContactGroupsUpsellingVisible,
            onClick = actions.onNewContactGroupClick
        )
        if (ContactImport.value) {
            ContactBottomSheetItem(
                modifier = Modifier,
                titleResId = R.string.import_contact,
                iconResId = R.drawable.ic_proton_mobile_plus,
                onClick = actions.onImportContactClick
            )
        }
    }
}

@Composable
private fun ContactBottomSheetItem(
    modifier: Modifier = Modifier,
    titleResId: Int,
    iconResId: Int,
    isUpsellingVisible: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(id = titleResId),
                role = Role.Button,
                onClick = onClick
            )
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconNorm
        )
        Text(
            modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing),
            text = stringResource(id = titleResId),
            style = ProtonTheme.typography.defaultNorm
        )
        if (isUpsellingVisible) {
            UpsellingIcon(modifier = Modifier.padding(start = ProtonDimens.SmallSpacing))
        }
    }
}

object ContactBottomSheet {

    data class Actions(
        val onNewContactClick: () -> Unit,
        val onNewContactGroupClick: () -> Unit,
        val onImportContactClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onNewContactClick = {},
                onNewContactGroupClick = {},
                onImportContactClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactBottomSheetScreenPreview() {
    ContactBottomSheetContent(
        isContactGroupsUpsellingVisible = true,
        actions = ContactBottomSheet.Actions.Empty
    )
}
