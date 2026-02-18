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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.compose.FocusableFormScope
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsActions
import ch.protonmail.android.mailcomposer.presentation.model.toImmutableChipList
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.chips.ComposerChipsListField
import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem
import ch.protonmail.android.mailcomposer.presentation.ui.suggestions.ContactSuggestionState
import ch.protonmail.android.mailcomposer.presentation.viewmodel.RecipientsViewModel
import ch.protonmail.android.uicomponents.thenIf
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun FocusableFormScope<FocusedFieldType>.RecipientFields(
    modifier: Modifier = Modifier,
    fieldFocusRequesters: Map<FocusedFieldType, FocusRequester>,
    onToggleSuggestions: (Boolean) -> Unit,
    formHeightPx: Float,
    viewModel: RecipientsViewModel
) {
    var recipientsOpen by remember { mutableStateOf(false) }
    val recipientsButtonRotation = remember { Animatable(0F) }
    val recipients by viewModel.recipientsStateManager.recipients.collectAsStateWithLifecycle()
    val recipientsTo = recipients.toRecipients.toImmutableChipList()
    val recipientsCcValue = recipients.ccRecipients.toImmutableChipList()
    val recipientsBccValue = recipients.bccRecipients.toImmutableChipList()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestions = state.suggestions
    val suggestionsField = state.suggestionsField

    val isShowingToSuggestions = suggestionsField == ContactSuggestionsField.TO && suggestions.isNotEmpty()
    val isShowingCcSuggestions = suggestionsField == ContactSuggestionsField.CC && suggestions.isNotEmpty()
    val isShowingBccSuggestions = suggestionsField == ContactSuggestionsField.BCC && suggestions.isNotEmpty()
    val hasCcBccContent = recipientsCcValue.isNotEmpty() || recipientsBccValue.isNotEmpty()
    val shouldShowCcBcc = recipientsOpen || hasCcBccContent

    val readContactsPermission = rememberPermissionState(
        permission = Manifest.permission.READ_CONTACTS
    ) { _ ->
        viewModel.submit(RecipientsActions.MarkContactsPermissionInteraction)
    }

    ConsumableLaunchedEffect(state.requestContactsPermission) {
        readContactsPermission.launchPermissionRequest()
    }

    LaunchedEffect(suggestions, suggestionsField) {
        onToggleSuggestions(suggestionsField == null || suggestions.isEmpty())
    }

    // Take height of to chip text field as it's always shown (ie. no changing to 0 on recompose)
    var toChipTextFieldHeightPx by remember { mutableFloatStateOf(0f) }
    // When showing to suggestions, only "to" field is visible
    val toSuggestionsListHeightPx = (formHeightPx - toChipTextFieldHeightPx).coerceAtLeast(0f)
    // When showing cc suggestions, "to" and "cc" fields are visible
    val ccSuggestionsListHeightPx = (formHeightPx - toChipTextFieldHeightPx * 2).coerceAtLeast(0f)
    // When showing cc suggestions, "to", "cc" and "bcc" fields are visible
    val bccSuggestionsListHeightPx = (formHeightPx - toChipTextFieldHeightPx * 3).coerceAtLeast(0f)

    val toSuggestionListHeightDp = with(LocalDensity.current) { toSuggestionsListHeightPx.toDp() }
    val ccSuggestionListHeightDp = with(LocalDensity.current) { ccSuggestionsListHeightPx.toDp() }
    val bccSuggestionListHeightDp = with(LocalDensity.current) { bccSuggestionsListHeightPx.toDp() }

    val baseFieldActions = ComposerChipsListField.Actions.Empty.copy(
        onPermissionRequest = { viewModel.submit(RecipientsActions.RequestContactsPermission) },
        onPermissionInteraction = { viewModel.submit(RecipientsActions.MarkContactsPermissionInteraction) }
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ComposerChipsListField(
            label = stringResource(id = R.string.to_prefix),
            chipsList = recipientsTo,
            modifier = Modifier
                .weight(1f)
                .testTag(ComposerTestTags.ToRecipient)
                .retainFieldFocusOnConfigurationChange(FocusedFieldType.TO)
                .onGloballyPositioned {
                    // Only take this measure when suggestions are not shown, as we want the height of the field only
                    if (!isShowingToSuggestions) {
                        toChipTextFieldHeightPx = it.boundsInWindow().height
                    }
                },
            focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.TO),
            nextFocusRequester = if (shouldShowCcBcc) {
                fieldFocusRequesters.getValue(FocusedFieldType.CC)
            } else { fieldFocusRequesters.getValue(FocusedFieldType.SUBJECT) },
            actions = baseFieldActions.copy(
                onSuggestionTermTyped = {
                    viewModel.submit(RecipientsActions.UpdateSearchTerm(it, ContactSuggestionsField.TO))
                },
                onSuggestionsDismissed = {
                    if (isShowingToSuggestions) viewModel.submit(RecipientsActions.CloseSuggestions)
                },
                onListChanged = {
                    viewModel.submit(RecipientsActions.UpdateRecipients(it.toUiModel(), ContactSuggestionsField.TO))
                }
            ),
            contactSuggestionState = ContactSuggestionState(
                areSuggestionsExpanded = isShowingToSuggestions,
                contactSuggestionItems = suggestions,
                suggestionListHeightDp = toSuggestionListHeightDp
            ),
            chevronIconContent = {
                if (!hasCcBccContent) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .focusProperties { canFocus = false },
                        onClick = {
                            recipientsOpen = !recipientsOpen
                            viewModel.submit(RecipientsActions.CloseSuggestions)
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues.Closed) {
                                    testTag(ComposerTestTags.ExpandCollapseArrow)
                                }
                                .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues.Open) {
                                    testTag(ComposerTestTags.CollapseExpandArrow)
                                }
                                .rotate(recipientsButtonRotation.value),
                            imageVector = Icons.Default.ArrowDropDown,
                            tint = ProtonTheme.colors.iconHint,
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
                focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.CC),
                nextFocusRequester = fieldFocusRequesters.getValue(FocusedFieldType.BCC),
                actions = baseFieldActions.copy(
                    onSuggestionTermTyped = {
                        viewModel.submit(RecipientsActions.UpdateSearchTerm(it, ContactSuggestionsField.CC))
                    },
                    onSuggestionsDismissed = {
                        if (isShowingCcSuggestions) viewModel.submit(RecipientsActions.CloseSuggestions)
                    },
                    onListChanged = {
                        viewModel.submit(
                            RecipientsActions.UpdateRecipients(it.toUiModel(), ContactSuggestionsField.CC)
                        )
                    }
                ),
                contactSuggestionState = ContactSuggestionState(
                    areSuggestionsExpanded = isShowingCcSuggestions,
                    contactSuggestionItems = suggestions,
                    suggestionListHeightDp = ccSuggestionListHeightDp
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
                    focusRequester = fieldFocusRequesters.getValue(FocusedFieldType.BCC),
                    nextFocusRequester = fieldFocusRequesters.getValue(FocusedFieldType.SUBJECT),
                    actions = baseFieldActions.copy(
                        onSuggestionTermTyped = {
                            viewModel.submit(
                                RecipientsActions.UpdateSearchTerm(it, ContactSuggestionsField.BCC)
                            )
                        },
                        onSuggestionsDismissed = {
                            if (isShowingBccSuggestions) viewModel.submit(RecipientsActions.CloseSuggestions)
                        },
                        onListChanged = {
                            viewModel.submit(
                                RecipientsActions.UpdateRecipients(it.toUiModel(), ContactSuggestionsField.BCC)
                            )
                        }
                    ),
                    contactSuggestionState = ContactSuggestionState(
                        areSuggestionsExpanded = isShowingBccSuggestions,
                        contactSuggestionItems = suggestions,
                        suggestionListHeightDp = bccSuggestionListHeightDp
                    )
                )
            }
        }
    }

    LaunchedEffect(key1 = recipientsOpen) {
        recipientsButtonRotation.animateTo(
            if (recipientsOpen) RecipientsButtonRotationValues.Open else RecipientsButtonRotationValues.Closed
        )
    }
}

private object RecipientsButtonRotationValues {

    const val Open = 180f
    const val Closed = 0f
}

private fun List<ChipItem>.toUiModel() = mapNotNull { it ->
    when (it) {
        is ChipItem.Counter -> null
        is ChipItem.Invalid -> RecipientUiModel.Invalid(it.value)
        is ChipItem.Valid -> RecipientUiModel.Valid(it.value)
        is ChipItem.Validating -> RecipientUiModel.Validating(it.value)
        is ChipItem.Group -> RecipientUiModel.Group(
            name = it.value,
            members = it.members,
            color = it.color
        )
    }
}
