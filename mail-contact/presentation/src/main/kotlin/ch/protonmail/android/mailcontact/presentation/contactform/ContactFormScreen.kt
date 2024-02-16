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
import ch.protonmail.android.mailcontact.presentation.model.getAddressTypeByValue
import ch.protonmail.android.mailcontact.presentation.model.getEmailTypeByValue
import ch.protonmail.android.mailcontact.presentation.model.getOtherTypeByValue
import ch.protonmail.android.mailcontact.presentation.model.getTelephoneTypeByValue
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
                    viewModel.submit(ContactFormViewAction.OnSaveClick)
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
                            onUpdateDisplayName = { viewModel.submit(ContactFormViewAction.OnUpdateDisplayName(it)) },
                            onUpdateFirstName = { viewModel.submit(ContactFormViewAction.OnUpdateFirstName(it)) },
                            onUpdateLastName = { viewModel.submit(ContactFormViewAction.OnUpdateLastName(it)) },
                            onRemoveItemClick = { section, index ->
                                viewModel.submit(ContactFormViewAction.OnRemoveItemClick(section, index))
                            },
                            onUpdateItem = { section, index, newValue ->
                                viewModel.submit(ContactFormViewAction.OnUpdateItem(section, index, newValue))
                            }
                        )
                    )

                    ConsumableTextEffect(effect = state.closeWithSuccess) { message ->
                        actions.exitWithSuccessMessage(message)
                    }
                    ConsumableTextEffect(effect = state.showErrorSnackbar) { message ->
                        snackbarHostErrorState.showSnackbar(
                            message = message,
                            type = ProtonSnackbarType.ERROR
                        )
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

                NameSection(state, actions)
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
            val mutableEmail = remember { mutableStateOf(email) }
            InputFieldWithTrash(
                value = email.value,
                hint = stringResource(id = R.string.email_address),
                onDeleteClick = { actions.onRemoveItemClick(Section.Emails, index) }
            ) {
                mutableEmail.value = mutableEmail.value.copy(value = it)
                actions.onUpdateItem(Section.Emails, index, mutableEmail.value)
            }
            TypePickerField(
                initialSelectedType = email.selectedType
            ) { selectedValue ->
                mutableEmail.value = mutableEmail.value.copy(
                    selectedType = getEmailTypeByValue(selectedValue)
                )
                actions.onUpdateItem(
                    Section.Emails,
                    index,
                    mutableEmail.value
                )
            }
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
            val mutableTelephone = remember { mutableStateOf(telephone) }
            InputFieldWithTrash(
                value = telephone.value,
                hint = stringResource(id = R.string.phone_number),
                onDeleteClick = { actions.onRemoveItemClick(Section.Telephones, index) }
            ) {
                mutableTelephone.value = mutableTelephone.value.copy(value = it)
                actions.onUpdateItem(Section.Telephones, index, mutableTelephone.value)
            }
            TypePickerField(
                initialSelectedType = telephone.selectedType
            ) { selectedValue ->
                mutableTelephone.value = mutableTelephone.value.copy(
                    selectedType = getTelephoneTypeByValue(selectedValue)
                )
                actions.onUpdateItem(
                    Section.Telephones,
                    index,
                    mutableTelephone.value
                )
            }
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
            val mutableAddress = remember { mutableStateOf(address) }
            InputFieldWithTrash(
                value = address.streetAddress,
                hint = stringResource(R.string.address_street),
                onDeleteClick = { actions.onRemoveItemClick(Section.Addresses, index) }
            ) {
                mutableAddress.value = mutableAddress.value.copy(streetAddress = it)
                actions.onUpdateItem(Section.Addresses, index, mutableAddress.value)
            }
            InputField(value = address.postalCode, hint = stringResource(R.string.address_postal_code)) {
                mutableAddress.value = mutableAddress.value.copy(postalCode = it)
                actions.onUpdateItem(Section.Addresses, index, mutableAddress.value)
            }
            InputField(value = address.city, hint = stringResource(R.string.address_city)) {
                mutableAddress.value = mutableAddress.value.copy(city = it)
                actions.onUpdateItem(Section.Addresses, index, mutableAddress.value)
            }
            InputField(value = address.region, hint = stringResource(R.string.address_region)) {
                mutableAddress.value = mutableAddress.value.copy(region = it)
                actions.onUpdateItem(Section.Addresses, index, mutableAddress.value)
            }
            InputField(value = address.country, hint = stringResource(R.string.address_country)) {
                mutableAddress.value = mutableAddress.value.copy(country = it)
                actions.onUpdateItem(Section.Addresses, index, mutableAddress.value)
            }
            TypePickerField(
                initialSelectedType = address.selectedType
            ) { selectedValue ->
                mutableAddress.value = mutableAddress.value.copy(
                    selectedType = getAddressTypeByValue(selectedValue)
                )
                actions.onUpdateItem(
                    Section.Addresses,
                    index,
                    mutableAddress.value
                )
            }
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
            val mutableNote = remember { mutableStateOf(note) }
            InputFieldWithTrash(
                value = note.value,
                hint = stringResource(id = R.string.note_section),
                onDeleteClick = { actions.onRemoveItemClick(Section.Notes, index) }
            ) {
                mutableNote.value = mutableNote.value.copy(value = it)
                actions.onUpdateItem(Section.Notes, index, mutableNote.value)
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
                    val mutableOther = remember { mutableStateOf(other) }
                    InputFieldWithTrash(
                        value = other.value,
                        hint = stringResource(id = R.string.additional_info),
                        onDeleteClick = { actions.onRemoveItemClick(Section.Others, index) }
                    ) {
                        mutableOther.value = mutableOther.value.copy(value = it)
                        actions.onUpdateItem(Section.Others, index, mutableOther.value)
                    }
                    TypePickerField(
                        initialSelectedType = other.selectedType
                    ) { selectedValue ->
                        mutableOther.value = mutableOther.value.copy(
                            selectedType = getOtherTypeByValue(selectedValue)
                        )
                        actions.onUpdateItem(
                            Section.Others,
                            index,
                            mutableOther.value
                        )
                    }
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
private fun NameSection(state: ContactFormState.Data, actions: ContactFormContent.Actions) {
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
            actions.onUpdateDisplayName(it)
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
            actions.onUpdateFirstName(it)
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
            actions.onUpdateLastName(it)
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
            initialValue = value,
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
            initialValue = value,
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
private fun TypePickerField(initialSelectedType: FieldType, onValueSelected: (TextUiModel) -> Unit) {
    val openDialog = remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(initialSelectedType.localizedValue) }

    val localizedValues = when (initialSelectedType) {
        is FieldType.EmailType -> FieldType.EmailType.values()
        is FieldType.TelephoneType -> FieldType.TelephoneType.values()
        is FieldType.AddressType -> FieldType.AddressType.values()
        is FieldType.OtherType -> FieldType.OtherType.values()
    }.map { it.localizedValue }

    when {
        openDialog.value -> {
            PickerDialog(
                title = stringResource(R.string.property_label),
                selectedValue = selectedType,
                values = localizedValues,
                onDismissRequest = { openDialog.value = false },
                onValueSelected = { selectedValue ->
                    openDialog.value = false
                    selectedType = selectedValue
                    onValueSelected(selectedValue)
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
            val displaySaveLoader = state is ContactFormState.Data && state.displaySaveLoader
            if (displaySaveLoader) {
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
        val exitWithErrorMessage: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onCloseClick = {},
                exitWithSuccessMessage = {},
                exitWithErrorMessage = {}
            )
        }
    }
}

object ContactFormContent {

    data class Actions(
        val onAddItemClick: (Section) -> Unit,
        val onUpdateDisplayName: (String) -> Unit,
        val onUpdateFirstName: (String) -> Unit,
        val onUpdateLastName: (String) -> Unit,
        val onRemoveItemClick: (Section, Int) -> Unit,
        val onUpdateItem: (Section, Int, InputField) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onAddItemClick = {},
                onUpdateDisplayName = {},
                onUpdateFirstName = {},
                onUpdateLastName = {},
                onRemoveItemClick = { _, _ -> },
                onUpdateItem = { _, _, _ -> }
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CreateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data(contact = contactFormSampleData()),
        actions = ContactFormContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun UpdateContactFormScreenPreview() {
    ContactFormContent(
        state = ContactFormState.Data(contact = contactFormSampleData()),
        actions = ContactFormContent.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactFormTopBarPreview() {
    ContactFormTopBar(
        state = ContactFormState.Data(contact = contactFormSampleData()),
        onCloseContactFormClick = {},
        onSaveContactClick = {}
    )
}
