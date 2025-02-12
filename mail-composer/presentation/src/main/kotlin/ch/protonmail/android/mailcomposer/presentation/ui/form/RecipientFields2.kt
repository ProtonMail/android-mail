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

package ch.protonmail.android.mailcomposer.presentation.ui.form

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.compose.FocusableFormScope
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.toImmutableChipList
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.chips.ComposerChipsListField
import ch.protonmail.android.mailcomposer.presentation.viewmodel.RecipientsViewModel
import ch.protonmail.android.uicomponents.chips.ChipsListField
import ch.protonmail.android.uicomponents.chips.ContactSuggestionState
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItem
import ch.protonmail.android.uicomponents.thenIf
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun FocusableFormScope<FocusedFieldType>.RecipientFields2(
    modifier: Modifier = Modifier,
    fieldFocusRequesters: Map<FocusedFieldType, FocusRequester>,
    onToggleSuggestions: (Boolean) -> Unit,
    viewModel: RecipientsViewModel
) {
    var recipientsOpen by remember { mutableStateOf(false) }
    val recipientsButtonRotation = remember { Animatable(0F) }
    val recipients by viewModel.recipientsStateManager.recipients.collectAsStateWithLifecycle()
    val recipientsTo = recipients.toRecipients.toImmutableChipList()
    val recipientsCcValue = recipients.ccRecipients.toImmutableChipList()
    val recipientsBccValue = recipients.bccRecipients.toImmutableChipList()

    val suggestions by viewModel.contactsSuggestions.collectAsStateWithLifecycle()
    val suggestionField by viewModel.contactSuggestionsFieldFlow.collectAsStateWithLifecycle()
    val isShowingToSuggestions = suggestionField == ContactSuggestionsField.TO && suggestions.isNotEmpty()
    val isShowingCcSuggestions = suggestionField == ContactSuggestionsField.CC && suggestions.isNotEmpty()
    val isShowingBccSuggestions = suggestionField == ContactSuggestionsField.BCC && suggestions.isNotEmpty()
    val hasCcBccContent = recipientsCcValue.isNotEmpty() || recipientsBccValue.isNotEmpty()
    val shouldShowCcBcc = recipientsOpen || hasCcBccContent

    var showContactsPermissionDialog by remember { mutableStateOf(false) }
    val contactsPermissionDenied = viewModel.contactsPermissionDenied.collectAsStateWithLifecycle(false)

    val readContactsPermission = rememberPermissionState(
        permission = Manifest.permission.READ_CONTACTS
    )

    if (!readContactsPermission.status.isGranted &&
        !contactsPermissionDenied.value &&
        showContactsPermissionDialog
    ) {
        ProtonAlertDialog(
            title = stringResource(id = R.string.device_contacts_permission_dialog_title),
            text = { ProtonAlertDialogText(R.string.device_contacts_permission_dialog_message) },
            dismissButton = {
                ProtonAlertDialogButton(R.string.device_contacts_permission_dialog_action_button_deny) {
                    showContactsPermissionDialog = false
                    viewModel.denyContactsPermission()
                }
            },
            confirmButton = {
                ProtonAlertDialogButton(R.string.device_contacts_permission_dialog_action_button) {
                    showContactsPermissionDialog = false
                    readContactsPermission.launchPermissionRequest()
                }
            },
            onDismissRequest = {
                showContactsPermissionDialog = false
                viewModel.denyContactsPermission()
            }
        )
    }

    LaunchedEffect(suggestions, suggestionField) {
        onToggleSuggestions(suggestionField == null || suggestions.isEmpty())
    }

    LaunchedEffect(readContactsPermission.status.isGranted) {
        if (!readContactsPermission.status.isGranted && !contactsPermissionDenied.value) {
            if (readContactsPermission.status.shouldShowRationale) {
                showContactsPermissionDialog = true
            } else if (!showContactsPermissionDialog) {
                readContactsPermission.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(suggestions, suggestionField) {
        onToggleSuggestions(suggestionField == null || suggestions.isEmpty())
    }

    LaunchedEffect(readContactsPermission.status.isGranted) {
        if (!readContactsPermission.status.isGranted) {
            if (readContactsPermission.status.shouldShowRationale) {
                showContactsPermissionDialog = true
            } else {
                readContactsPermission.launchPermissionRequest()
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        ComposerChipsListField(
            label = stringResource(id = R.string.to_prefix),
            chipsList = recipientsTo,
            modifier = Modifier
                .weight(1f)
                .testTag(ComposerTestTags.ToRecipient)
                .retainFieldFocusOnConfigurationChange(FocusedFieldType.TO),
            focusRequester = fieldFocusRequesters[FocusedFieldType.TO],
            actions = ChipsListField.Actions(
                onSuggestionTermTyped = {
                    viewModel.updateSearchTerm(it, ContactSuggestionsField.TO)
                },
                onSuggestionsDismissed = {
                    if (isShowingToSuggestions) viewModel.closeSuggestions()
                },
                onListChanged = {
                    viewModel.updateRecipients(it.toUiModel(), ContactSuggestionsField.TO)
                }
            ),
            contactSuggestionState = ContactSuggestionState(
                areSuggestionsExpanded = isShowingToSuggestions,
                contactSuggestionItems = suggestions.map { it.toSuggestionContactItem2() }
            ),
            chevronIconContent = {
                if (!hasCcBccContent) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .focusProperties { canFocus = false },
                        onClick = {
                            recipientsOpen = !recipientsOpen
                            viewModel.closeSuggestions()
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues2.Closed) {
                                    testTag(ComposerTestTags.ExpandCollapseArrow)
                                }
                                .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues2.Open) {
                                    testTag(ComposerTestTags.CollapseExpandArrow)
                                }
                                .rotate(recipientsButtonRotation.value)
                                .size(ProtonDimens.SmallIconSize),
                            imageVector = ImageVector.vectorResource(
                                id = me.proton.core.presentation.R.drawable.ic_proton_chevron_down_filled
                            ),
                            tint = ProtonTheme.colors.textWeak,
                            contentDescription = stringResource(id = R.string.composer_expand_recipients_button)
                        )
                    }
                }
            }
        )
    }

    AnimatedVisibility(
        visible = shouldShowCcBcc && !isShowingToSuggestions,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Column {
            MailDivider()
            ComposerChipsListField(
                label = stringResource(id = R.string.cc_prefix),
                chipsList = recipientsCcValue,
                modifier = Modifier
                    .testTag(ComposerTestTags.CcRecipient)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.CC),
                focusRequester = fieldFocusRequesters[FocusedFieldType.CC],
                actions = ChipsListField.Actions(
                    onSuggestionTermTyped = {
                        viewModel.updateSearchTerm(it, ContactSuggestionsField.CC)
                    },
                    onSuggestionsDismissed = {
                        if (isShowingCcSuggestions) viewModel.closeSuggestions()
                    },
                    onListChanged = {
                        viewModel.updateRecipients(it.toUiModel(), ContactSuggestionsField.CC)
                    }
                ),
                contactSuggestionState = ContactSuggestionState(
                    areSuggestionsExpanded = isShowingCcSuggestions,
                    contactSuggestionItems = suggestions.map { it.toSuggestionContactItem2() }
                )
            )

            if (!isShowingCcSuggestions) {
                MailDivider()
                ComposerChipsListField(
                    label = stringResource(id = R.string.bcc_prefix),
                    chipsList = recipientsBccValue,
                    modifier = Modifier
                        .testTag(ComposerTestTags.BccRecipient)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.BCC),
                    focusRequester = fieldFocusRequesters[FocusedFieldType.BCC],
                    actions = ChipsListField.Actions(
                        onSuggestionTermTyped = {
                            viewModel.updateSearchTerm(it, ContactSuggestionsField.BCC)
                        },
                        onSuggestionsDismissed = {
                            if (isShowingBccSuggestions) viewModel.closeSuggestions()
                        },
                        onListChanged = {
                            viewModel.updateRecipients(it.toUiModel(), ContactSuggestionsField.BCC)
                        }
                    ),
                    contactSuggestionState = ContactSuggestionState(
                        areSuggestionsExpanded = isShowingBccSuggestions,
                        contactSuggestionItems = suggestions.map { it.toSuggestionContactItem2() }
                    )
                )
            }
        }
    }

    LaunchedEffect(key1 = recipientsOpen) {
        recipientsButtonRotation.animateTo(
            if (recipientsOpen) RecipientsButtonRotationValues2.Open else RecipientsButtonRotationValues2.Closed
        )
    }
}

// Move the below it once ComposerV2 becomes the default flow.
private object RecipientsButtonRotationValues2 {

    const val Open = 180f
    const val Closed = 0f
}

private fun List<ChipItem>.toUiModel() = mapNotNull { it ->
    when (it) {
        is ChipItem.Counter -> null
        is ChipItem.Invalid -> RecipientUiModel.Invalid(it.value)
        is ChipItem.Valid -> RecipientUiModel.Valid(it.value)
    }
}

@Composable
private fun ContactSuggestionUiModel.toSuggestionContactItem2(): ContactSuggestionItem = when (this) {
    is ContactSuggestionUiModel.Contact -> ContactSuggestionItem.Contact(
        initials = this.initial,
        header = this.name,
        subheader = this.email,
        email = this.email
    )

    is ContactSuggestionUiModel.ContactGroup -> {
        val backgroundColor = runCatching { Color(android.graphics.Color.parseColor(this.color)) }.getOrElse {
            Timber.tag("getContactGroupColor").w("Failed to convert raw string color from $color")
            ProtonTheme.colors.backgroundSecondary
        }

        ContactSuggestionItem.Group(
            header = this.name,
            subheader = TextUiModel.PluralisedText(
                value = R.plurals.composer_recipient_suggestion_contacts,
                count = this.emails.size
            ).string(),
            emails = this.emails,
            backgroundColor = backgroundColor
        )
    }
}
