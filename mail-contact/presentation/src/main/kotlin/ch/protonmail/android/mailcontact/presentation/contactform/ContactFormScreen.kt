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

package ch.protonmail.android.mailcontact.presentation.contactform

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_FIRST_LAST_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData.contactFormSampleData
import ch.protonmail.android.mailcontact.presentation.ui.FormInputField
import ch.protonmail.android.mailcontact.presentation.ui.ImageContactAvatar
import ch.protonmail.android.mailcontact.presentation.ui.InitialsContactAvatar
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactFormScreen(actions: ContactFormScreen.Actions, viewModel: ContactFormViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val state = rememberAsState(flow = viewModel.state, initial = viewModel.initialState).value

    Scaffold(
        topBar = {
            ContactFormTopBar(
                state = state,
                onCloseContactFormClick = {
                    viewModel.submit(ContactFormViewAction.OnCloseContactFormClick)
                },
                onSaveContactClick = {
                    TODO()
                }
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactFormState.Data -> {
                    ContactFormContent(
                        state = state,
                        modifier = Modifier.padding(paddingValues)
                    )

                    ConsumableTextEffect(effect = state.closeWithSuccess) { message ->
                        actions.exitWithSuccessMessage(message)
                    }
                }
                is ContactFormState.Loading -> {
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
        actions.onCloseClick()
    }
}

@Composable
fun ContactFormContent(state: ContactFormState.Data, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Column(modifier.fillMaxWidth()) {
                when (val avatar = state.contact.avatar) {
                    is ContactFormAvatar.Empty -> {
                        // Temporary: replace with camera icon avatar once we implement image picker.
                        InitialsContactAvatar(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(
                                    top = ProtonDimens.DefaultSpacing,
                                    bottom = ProtonDimens.MediumSpacing
                                ),
                            initials = ""
                        )
                    }
                    is ContactFormAvatar.Photo -> {
                        ImageContactAvatar(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(
                                    top = ProtonDimens.DefaultSpacing,
                                    bottom = ProtonDimens.MediumSpacing
                                ),
                            imageBitmap = avatar.bitmap.asImageBitmap()
                        )
                    }
                }
                FormInputField(
                    initialValue = state.contact.displayName,
                    hint = "Display name",
                    maxCharacters = CONTACT_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
                FormInputField(
                    initialValue = state.contact.firstName,
                    hint = "First name",
                    maxCharacters = CONTACT_FIRST_LAST_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
                FormInputField(
                    initialValue = state.contact.lastName,
                    hint = "Last name",
                    maxCharacters = CONTACT_FIRST_LAST_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
            }
        }
    }
}

@Composable
fun ContactFormTopBar(
    state: ContactFormState,
    onCloseContactFormClick: () -> Unit,
    onSaveContactClick: () -> Unit
) {
    ProtonTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { },
        navigationIcon = {
            IconButton(onClick = onCloseContactFormClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.presentation_close)
                )
            }
        },
        actions = {
            val displayCreateLoader = state is ContactFormState.Data.Create && state.displayCreateLoader
            if (displayCreateLoader) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = ProtonDimens.DefaultSpacing)
                        .size(MailDimens.ProgressDefaultSize),
                    strokeWidth = MailDimens.ProgressStrokeWidth
                )
            } else {
                ProtonTextButton(
                    onClick = onSaveContactClick,
                    enabled = state.isSaveEnabled
                ) {
                    val textColor =
                        if (state.isSaveEnabled) ProtonTheme.colors.textAccent
                        else ProtonTheme.colors.interactionDisabled
                    Text(
                        text = stringResource(id = R.string.contact_form_save),
                        color = textColor,
                        style = ProtonTheme.typography.defaultStrongNorm
                    )
                }
            }
        }
    )
}

object ContactFormScreen {

    const val ContactFormContactIdKey = "contact_form_contact_id"

    data class Actions(
        val onCloseClick: () -> Unit,
        val exitWithSuccessMessage: (String) -> Unit,
        val exitWithErrorMessage: (String) -> Unit,
        val onSaveClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onCloseClick = {},
                exitWithSuccessMessage = {},
                exitWithErrorMessage = {},
                onSaveClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data.Create(contact = contactFormSampleData)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun UpdateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data.Update(contact = contactFormSampleData)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactFormTopBarPreview() {
    ContactFormTopBar(
        state = ContactFormState.Data.Update(contact = contactFormSampleData),
        onCloseContactFormClick = {},
        onSaveContactClick = {}
    )
}
