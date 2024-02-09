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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
import ch.protonmail.android.mailcontact.presentation.ui.IconContactAvatar
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.label.domain.entity.LabelId

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactGroupDetailsScreen(
    actions: ContactGroupDetailsScreen.Actions,
    viewModel: ContactGroupDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = ContactGroupDetailsViewModel.initialState).value

    Scaffold(
        topBar = {
            ContactGroupDetailsTopBar(
                state = state,
                actions = actions
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactGroupDetailsState.Data -> {
                    ContactGroupDetailsContent(
                        state,
                        actions
                    )
                }
                is ContactGroupDetailsState.Loading -> {
                    ProtonCenteredProgress(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    )

                    ConsumableTextEffect(effect = state.errorLoading) { message ->
                        actions.exitWithErrorMessage(message)
                    }
                }
            }
        },
        snackbarHost = {
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                hostState = snackbarHostErrorState
            )
        }
    )

    ConsumableLaunchedEffect(effect = state.close) {
        dismissKeyboard(context, view, keyboardController)
        actions.onBackClick()
    }
}

@Composable
fun ContactGroupDetailsContent(
    state: ContactGroupDetailsState.Data,
    actions: ContactGroupDetailsScreen.Actions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Column(modifier.fillMaxWidth()) {
                IconContactAvatar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = ProtonDimens.DefaultSpacing),
                    iconResId = R.drawable.ic_proton_users,
                    backgroundColor = state.contactGroup.color
                )
                Text(
                    modifier = Modifier
                        .padding(top = ProtonDimens.MediumSpacing)
                        .align(Alignment.CenterHorizontally),
                    style = ProtonTheme.typography.headlineNorm,
                    text = state.contactGroup.name
                )
                Text(
                    modifier = Modifier
                        .padding(top = ProtonDimens.ExtraSmallSpacing)
                        .align(Alignment.CenterHorizontally),
                    style = ProtonTheme.typography.captionWeak,
                    text = pluralStringResource(
                        R.plurals.contact_group_member_count,
                        state.contactGroup.memberCount,
                        state.contactGroup.memberCount
                    )
                )
            }
        }
    }
}

@Composable
fun ContactGroupDetailsTopBar(state: ContactGroupDetailsState, actions: ContactGroupDetailsScreen.Actions) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            if (state is ContactGroupDetailsState.Data) {
                IconButton(onClick = { actions.onEditClick(state.contactGroup.id) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_pen),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.edit_contact_group_content_description)
                    )
                }
            }
        }
    )
}

object ContactGroupDetailsScreen {

    const val ContactGroupDetailsLabelIdKey = "contact_group_details_label_id"

    data class Actions(
        val onBackClick: () -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val onEditClick: (LabelId) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                exitWithErrorMessage = {},
                onEditClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactGroupDetailsContentPreview() {
    ContactGroupDetailsContent(
        state = ContactGroupDetailsState.Data(
            contactGroup = contactGroupDetailsSampleData
        ),
        actions = ContactGroupDetailsScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactGroupDetailsContentPreview() {
    ContactGroupDetailsContent(
        state = ContactGroupDetailsState.Data(
            contactGroup = contactGroupDetailsSampleData.copy(
                memberCount = 0,
                members = emptyList()
            )
        ),
        actions = ContactGroupDetailsScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactDetailsTopBarPreview() {
    ContactGroupDetailsTopBar(
        state = ContactGroupDetailsState.Data(
            contactGroup = contactGroupDetailsSampleData
        ),
        actions = ContactGroupDetailsScreen.Actions.Empty
    )
}
