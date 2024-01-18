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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_FIRST_LAST_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData.contactFormSampleData
import ch.protonmail.android.mailcontact.presentation.ui.FormInputField
import ch.protonmail.android.mailcontact.presentation.ui.ImageContactAvatar
import ch.protonmail.android.mailcontact.presentation.ui.InitialsContactAvatar
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ProtonDimens.DefaultSpacing,
                            end = ProtonDimens.DefaultSpacing,
                            bottom = ProtonDimens.DefaultSpacing
                        ),
                    initialValue = state.contact.displayName,
                    hint = stringResource(R.string.display_name),
                    maxCharacters = CONTACT_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
                FormInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ProtonDimens.DefaultSpacing,
                            end = ProtonDimens.DefaultSpacing,
                            bottom = ProtonDimens.DefaultSpacing
                        ),
                    initialValue = state.contact.firstName,
                    hint = stringResource(R.string.first_name),
                    maxCharacters = CONTACT_FIRST_LAST_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
                FormInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ProtonDimens.DefaultSpacing,
                            end = ProtonDimens.DefaultSpacing,
                            bottom = ProtonDimens.DefaultSpacing
                        ),
                    initialValue = state.contact.lastName,
                    hint = stringResource(R.string.last_name),
                    maxCharacters = CONTACT_FIRST_LAST_NAME_MAX_LENGTH,
                    onTextChange = {
                        // Trigger action here
                    }
                )
            }
        }
        this.emailSection(state)
        this.phoneSection(state)
        this.addressSection(state)
        this.noteSection(state)
        this.otherSection(state)
    }
}

private fun LazyListScope.emailSection(state: ContactFormState.Data) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_at,
            title = stringResource(id = R.string.email_section)
        )
    }
    items(state.contact.emails) { email ->
        SingleTypedField(
            value = email.value,
            hint = stringResource(id = R.string.email_address),
            selectedType = email.selectedType.localizedValue,
            types = FieldType.EmailType.values().map { it.localizedValue }
        )
    }
    item {
        AddNewButton {
            // Trigger action here
        }
    }
}

private fun LazyListScope.phoneSection(state: ContactFormState.Data) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_phone,
            title = stringResource(id = R.string.phone_section)
        )
    }
    items(state.contact.phones) { phone ->
        SingleTypedField(
            value = phone.value,
            hint = stringResource(id = R.string.phone_number),
            selectedType = phone.selectedType.localizedValue,
            types = FieldType.PhoneType.values().map { it.localizedValue }
        )
    }
    item {
        AddNewButton {
            // Trigger action here
        }
    }
}

private fun LazyListScope.addressSection(state: ContactFormState.Data) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_map_pin,
            title = stringResource(id = R.string.address_section)
        )
    }
    items(state.contact.addresses) { address ->
        AddressField(address = address)
    }
    item {
        AddNewButton {
            // Trigger action here
        }
    }
}

private fun LazyListScope.noteSection(state: ContactFormState.Data) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_note,
            title = stringResource(id = R.string.note_section)
        )
    }
    items(state.contact.notes) { _ ->
        // Add note field here
    }
    item {
        AddNewButton {
            // Trigger action here
        }
    }
}

private fun LazyListScope.otherSection(state: ContactFormState.Data) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_text_align_left,
            title = stringResource(id = R.string.other_section)
        )
    }
    items(state.contact.others) { other ->
        when (other) {
            is InputField.DateTyped -> {
                // Add date field for anniversary here
            }
            is InputField.ImageTyped -> {
                // Add image picker / image display field for photos and logos here
            }
            is InputField.SingleTyped -> {
                SingleTypedField(
                    value = other.value,
                    hint = stringResource(id = R.string.additional_info),
                    selectedType = other.selectedType.localizedValue,
                    types = state.contact.otherTypes.map { it.localizedValue }
                )
            }
            else -> {
                // Ignore the other types
            }
        }
    }
    item {
        AddNewButton {
            // Trigger action here
        }
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    iconResId: Int,
    title: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.MediumSpacing,
                bottom = ProtonDimens.SmallSpacing
            )
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            modifier = Modifier.size(ProtonDimens.SmallIconSize),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
            text = title,
            style = ProtonTheme.typography.captionWeak
        )
    }
    Divider()
}

@Composable
private fun AddNewButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ProtonSecondaryButton(
        modifier = modifier.padding(
            start = ProtonDimens.DefaultSpacing,
            top = ProtonDimens.ExtraSmallSpacing,
            bottom = ProtonDimens.DefaultSpacing
        ),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.add_new),
            Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
            style = ProtonTheme.typography.captionNorm
        )
    }
}

@Composable
private fun SingleTypedField(
    value: String,
    hint: String,
    selectedType: TextUiModel,
    types: List<TextUiModel>
) {
    SingleInputFieldWithTrash(value = value, hint = hint)
    TypePickerField(selectedType = selectedType, types = types)
}

@Composable
private fun AddressField(address: InputField.Address) {
    SingleInputFieldWithTrash(value = address.streetAddress, hint = stringResource(R.string.address_street))
    SingleInputField(value = address.postalCode, hint = stringResource(R.string.address_postal_code))
    SingleInputField(value = address.city, hint = stringResource(R.string.address_city))
    SingleInputField(value = address.region, hint = stringResource(R.string.address_region))
    SingleInputField(value = address.country, hint = stringResource(R.string.address_country))
    TypePickerField(
        selectedType = address.selectedType.localizedValue,
        types = FieldType.AddressType.values().map { it.localizedValue }
    )
}

@Composable
private fun SingleInputField(value: String, hint: String) {
    Row {
        FormInputField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = ProtonDimens.DefaultSpacing,
                    end = MailDimens.ContactFormTypedFieldPaddingEnd,
                    top = ProtonDimens.DefaultSpacing
                ),
            initialValue = value,
            hint = hint,
            onTextChange = {
                // Trigger action here
            }
        )
    }
}

@Composable
private fun SingleInputFieldWithTrash(value: String, hint: String) {
    Row {
        FormInputField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.DefaultSpacing,
                    top = ProtonDimens.DefaultSpacing
                ),
            initialValue = value,
            hint = hint,
            onTextChange = {
                // Trigger action here
            }
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_trash),
            modifier = Modifier
                .padding(end = ProtonDimens.DefaultSpacing)
                .align(Alignment.CenterVertically)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onClickLabel = stringResource(R.string.remove_contact_property),
                    role = Role.Button,
                    onClick = {
                        // Trigger action here
                    }
                ),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

@Composable
private fun TypePickerField(selectedType: TextUiModel, types: List<TextUiModel>) {
    Row(
        modifier = Modifier
            .padding(
                top = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.DefaultSpacing,
                start = ProtonDimens.DefaultSpacing,
                end = MailDimens.ContactFormTypedFieldPaddingEnd
            )
            .background(
                color = ProtonTheme.colors.backgroundSecondary,
                shape = ProtonTheme.shapes.medium
            )
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.property_type),
                role = Role.Button,
                onClick = {
                    // Open dialog here
                }
            )
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = ProtonDimens.DefaultSpacing,
                    bottom = ProtonDimens.DefaultSpacing,
                    start = ProtonDimens.DefaultSpacing,
                    end = ProtonDimens.ExtraSmallSpacing
                ),
            text = selectedType.string(),
            style = ProtonTheme.typography.defaultNorm
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_chevron_down),
            modifier = Modifier
                .padding(end = ProtonDimens.DefaultSpacing)
                .size(ProtonDimens.SmallIconSize)
                .align(Alignment.CenterVertically),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
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
