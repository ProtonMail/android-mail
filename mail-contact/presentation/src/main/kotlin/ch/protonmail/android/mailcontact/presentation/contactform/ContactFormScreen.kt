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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import ch.protonmail.android.mailcommon.presentation.compose.PickerDialog
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_FIRST_LAST_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.CONTACT_NAME_MAX_LENGTH
import ch.protonmail.android.mailcontact.presentation.model.ContactFormAvatar
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section
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
import kotlin.math.min

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
                    // Trigger save action here
                }
            )
        },
        content = { paddingValues ->
            when (state) {
                is ContactFormState.Data -> {
                    ContactFormContent(
                        state = state,
                        modifier = Modifier.padding(paddingValues),
                        actions = ContactFormContent.Actions(
                            onAddItemClick = { viewModel.submit(ContactFormViewAction.OnAddItemClick(it)) },
                            onRemoveItemClick = { section, index ->
                                viewModel.submit(ContactFormViewAction.OnRemoveItemClick(section, index))
                            }
                        )
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
fun ContactFormContent(
    state: ContactFormState.Data,
    modifier: Modifier = Modifier,
    actions: ContactFormContent.Actions
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
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

                NameSection(state)
            }
        }
        this.emailSection(state, actions)
        this.telephoneSection(state, actions)
        this.addressSection(state, actions)
        this.noteSection(state, actions)
        this.otherSection(state, actions)
    }
}

private fun LazyListScope.emailSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_at,
            title = stringResource(id = R.string.email_section)
        )
    }
    itemsIndexed(state.contact.emails) { index, email ->
        key(email) {
            var emailValue by rememberSaveable { mutableStateOf(email.value) }
            InputFieldWithTrash(
                value = emailValue,
                hint = stringResource(id = R.string.email_address),
                onDeleteClick = { actions.onRemoveItemClick(Section.Emails, index) }
            ) {
                emailValue = it
            }
            TypePickerField(
                selectedType = email.selectedType.localizedValue,
                types = FieldType.EmailType.values().map { it.localizedValue }
            )
        }
    }
    item {
        AddNewButton(onClick = { actions.onAddItemClick(Section.Emails) })
    }
}

private fun LazyListScope.telephoneSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_phone,
            title = stringResource(id = R.string.phone_section)
        )
    }
    itemsIndexed(state.contact.telephones) { index, telephone ->
        key(telephone) {
            var telephoneValue by rememberSaveable { mutableStateOf(telephone.value) }
            InputFieldWithTrash(
                value = telephoneValue,
                hint = stringResource(id = R.string.phone_number),
                onDeleteClick = { actions.onRemoveItemClick(Section.Telephones, index) }
            ) {
                telephoneValue = it
            }
            TypePickerField(
                selectedType = telephone.selectedType.localizedValue,
                types = FieldType.TelephoneType.values().map { it.localizedValue }
            )
        }
    }
    item {
        AddNewButton(onClick = { actions.onAddItemClick(Section.Telephones) })
    }
}

private fun LazyListScope.addressSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_map_pin,
            title = stringResource(id = R.string.address_section)
        )
    }
    itemsIndexed(state.contact.addresses) { index, address ->
        key(address) {
            var streetAddress by rememberSaveable { mutableStateOf(address.streetAddress) }
            var postalCode by rememberSaveable { mutableStateOf(address.postalCode) }
            var city by rememberSaveable { mutableStateOf(address.city) }
            var region by rememberSaveable { mutableStateOf(address.region) }
            var country by rememberSaveable { mutableStateOf(address.country) }
            InputFieldWithTrash(
                value = streetAddress,
                hint = stringResource(R.string.address_street),
                onDeleteClick = { actions.onRemoveItemClick(Section.Addresses, index) }
            ) {
                streetAddress = it
            }
            InputField(value = postalCode, hint = stringResource(R.string.address_postal_code)) {
                postalCode = it
            }
            InputField(value = city, hint = stringResource(R.string.address_city)) {
                city = it
            }
            InputField(value = region, hint = stringResource(R.string.address_region)) {
                region = it
            }
            InputField(value = country, hint = stringResource(R.string.address_country)) {
                country = it
            }
            TypePickerField(
                selectedType = address.selectedType.localizedValue,
                types = FieldType.AddressType.values().map { it.localizedValue }
            )
        }
    }
    item {
        AddNewButton(onClick = { actions.onAddItemClick(Section.Addresses) })
    }
}

private fun LazyListScope.noteSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_note,
            title = stringResource(id = R.string.note_section)
        )
    }
    itemsIndexed(state.contact.notes) { index, note ->
        key(note) {
            var noteValue by rememberSaveable { mutableStateOf(note.value) }
            InputFieldWithTrash(
                value = noteValue,
                hint = stringResource(id = R.string.note_section),
                onDeleteClick = { actions.onRemoveItemClick(Section.Notes, index) }
            ) {
                noteValue = it
            }
        }
    }
    item {
        AddNewButton(onClick = { actions.onAddItemClick(Section.Notes) })
    }
}

private fun LazyListScope.otherSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
    item {
        SectionHeader(
            iconResId = R.drawable.ic_proton_text_align_left,
            title = stringResource(id = R.string.other_section)
        )
    }
    itemsIndexed(state.contact.others) { index, other ->
        key(other) {
            when (other) {
                is InputField.DateTyped -> {
                    // Add date field for anniversary here
                }
                is InputField.ImageTyped -> {
                    // Add image picker / image display field for photos and logos here
                }
                is InputField.SingleTyped -> {
                    var otherValue by rememberSaveable { mutableStateOf(other.value) }
                    InputFieldWithTrash(
                        value = otherValue,
                        hint = stringResource(id = R.string.additional_info),
                        onDeleteClick = { actions.onRemoveItemClick(Section.Others, index) }
                    ) {
                        otherValue = it
                    }
                    TypePickerField(
                        selectedType = other.selectedType.localizedValue,
                        types = FieldType.OtherType.values().map { it.localizedValue }
                    )
                }
                else -> {
                    // Ignore the other types
                }
            }
        }
    }
    item {
        AddNewButton(onClick = { actions.onAddItemClick(Section.Others) })
    }
}

@Composable
private fun NameSection(state: ContactFormState.Data) {
    var displayName by rememberSaveable { mutableStateOf(state.contact.displayName) }
    var firstName by rememberSaveable { mutableStateOf(state.contact.firstName) }
    var lastName by rememberSaveable { mutableStateOf(state.contact.lastName) }
    FormInputField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.DefaultSpacing
            ),
        value = displayName,
        hint = stringResource(R.string.display_name),
        onTextChange = {
            displayName = it.substring(0, min(it.length, CONTACT_NAME_MAX_LENGTH))
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
        value = firstName,
        hint = stringResource(R.string.first_name),
        onTextChange = {
            firstName = it.substring(0, min(it.length, CONTACT_FIRST_LAST_NAME_MAX_LENGTH))
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
        value = lastName,
        hint = stringResource(R.string.last_name),
        onTextChange = {
            lastName = it.substring(0, min(it.length, CONTACT_FIRST_LAST_NAME_MAX_LENGTH))
        }
    )
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
    MailDivider()
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
private fun InputField(
    value: String,
    hint: String,
    onTextChange: (String) -> Unit
) {
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
            value = value,
            hint = hint,
            onTextChange = onTextChange
        )
    }
}

@Composable
private fun InputFieldWithTrash(
    value: String,
    hint: String,
    onDeleteClick: () -> Unit,
    onTextChange: (String) -> Unit
) {
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
            value = value,
            hint = hint,
            onTextChange = onTextChange
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_trash),
            tint = ProtonTheme.colors.iconNorm,
            modifier = Modifier
                .padding(end = ProtonDimens.DefaultSpacing)
                .align(Alignment.CenterVertically)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onClickLabel = stringResource(R.string.remove_contact_property),
                    role = Role.Button,
                    onClick = onDeleteClick
                ),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

@Composable
private fun TypePickerField(selectedType: TextUiModel, types: List<TextUiModel>) {
    val openDialog = remember { mutableStateOf(false) }
    when {
        openDialog.value -> {
            PickerDialog(
                title = stringResource(R.string.property_label),
                selectedValue = selectedType,
                values = types,
                onDismissRequest = { openDialog.value = false },
                onValueSelected = { _ ->
                    openDialog.value = false
                    // Trigger action here
                }
            )
        }
    }

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
                onClick = { openDialog.value = true }
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
            tint = ProtonTheme.colors.iconNorm,
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

object ContactFormContent {

    data class Actions(
        val onAddItemClick: (Section) -> Unit,
        val onRemoveItemClick: (Section, Int) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onAddItemClick = {},
                onRemoveItemClick = { _, _ -> }
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data.Create(contact = contactFormSampleData),
        actions = ContactFormContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun UpdateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data.Update(contact = contactFormSampleData),
        actions = ContactFormContent.Actions.Empty
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
