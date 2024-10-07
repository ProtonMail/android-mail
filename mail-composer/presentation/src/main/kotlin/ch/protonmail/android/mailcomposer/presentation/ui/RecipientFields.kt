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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ch.protonmail.android.mailcommon.presentation.compose.FocusableFormScope
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.uicomponents.chips.ChipsListField
import ch.protonmail.android.uicomponents.chips.ContactSuggestionItem
import ch.protonmail.android.uicomponents.chips.ContactSuggestionState
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.chips.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun FocusableFormScope<FocusedFieldType>.RecipientFields(
    modifier: Modifier = Modifier,
    fields: ComposerFields,
    fieldFocusRequesters: Map<FocusedFieldType, FocusRequester>,
    recipientsOpen: Boolean,
    emailValidator: (String) -> Boolean,
    contactSuggestions: Map<ContactSuggestionsField, List<ContactSuggestionUiModel>>,
    actions: ComposerFormActions,
    areContactSuggestionsExpanded: Map<ContactSuggestionsField, Boolean>
) {
    val emailNextKeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Email
    )
    val recipientsButtonRotation = remember { Animatable(0F) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ChipsListField(
            label = stringResource(id = R.string.to_prefix),
            value = fields.to.map { it.toChipItem() },
            chipValidator = emailValidator,
            modifier = Modifier
                .weight(1f)
                .padding(start = ProtonDimens.DefaultSpacing)
                .testTag(ComposerTestTags.ToRecipient)
                .retainFieldFocusOnConfigurationChange(FocusedFieldType.TO),
            keyboardOptions = emailNextKeyboardOptions,
            focusRequester = fieldFocusRequesters[FocusedFieldType.TO],
            actions = ChipsListField.Actions(
                onSuggestionTermTyped = {
                    actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.TO)
                },
                onSuggestionsDismissed = { actions.onContactSuggestionsDismissed(ContactSuggestionsField.TO) },
                onListChanged = {
                    actions.onToChanged(it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() })
                }
            ),
            contactSuggestionState = ContactSuggestionState(
                areSuggestionsExpanded = areContactSuggestionsExpanded[ContactSuggestionsField.TO] ?: false,
                contactSuggestionItems = contactSuggestions[ContactSuggestionsField.TO]?.map {
                    it.toSuggestionContactItem()
                } ?: emptyList()
            )
        )
        Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
        IconButton(
            modifier = Modifier.focusProperties { canFocus = false },
            onClick = { actions.onToggleRecipients(!recipientsOpen) }
        ) {
            Icon(
                modifier = Modifier
                    .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues.Closed) {
                        testTag(ComposerTestTags.ExpandCollapseArrow)
                    }
                    .thenIf(recipientsButtonRotation.value == RecipientsButtonRotationValues.Open) {
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
    AnimatedVisibility(
        visible = recipientsOpen,
        modifier = Modifier.animateContentSize()
    ) {
        Column {
            MailDivider()
            ChipsListField(
                label = stringResource(id = R.string.cc_prefix),
                value = fields.cc.map { it.toChipItem() },
                chipValidator = emailValidator,
                modifier = Modifier
                    .padding(start = ProtonDimens.DefaultSpacing)
                    .testTag(ComposerTestTags.CcRecipient)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.CC),
                keyboardOptions = emailNextKeyboardOptions,
                focusRequester = fieldFocusRequesters[FocusedFieldType.CC],
                actions = ChipsListField.Actions(
                    onSuggestionTermTyped = {
                        actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.CC)
                    },
                    onSuggestionsDismissed = {
                        actions.onContactSuggestionsDismissed(ContactSuggestionsField.CC)
                    },
                    onListChanged = {
                        actions.onCcChanged(it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() })
                    }
                ),
                contactSuggestionState = ContactSuggestionState(
                    areSuggestionsExpanded = areContactSuggestionsExpanded[ContactSuggestionsField.CC] ?: false,
                    contactSuggestionItems = contactSuggestions[ContactSuggestionsField.CC]?.map {
                        it.toSuggestionContactItem()
                    } ?: emptyList()
                )
            )
            MailDivider()
            ChipsListField(
                label = stringResource(id = R.string.bcc_prefix),
                value = fields.bcc.map { it.toChipItem() },
                chipValidator = emailValidator,
                modifier = Modifier
                    .padding(start = ProtonDimens.DefaultSpacing)
                    .testTag(ComposerTestTags.BccRecipient)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.BCC),
                keyboardOptions = emailNextKeyboardOptions,
                focusRequester = fieldFocusRequesters[FocusedFieldType.BCC],
                actions = ChipsListField.Actions(
                    onSuggestionTermTyped = {
                        actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.BCC)
                    },
                    onSuggestionsDismissed = {
                        actions.onContactSuggestionsDismissed(ContactSuggestionsField.BCC)
                    },
                    onListChanged = {
                        actions.onBccChanged(it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() })
                    }
                ),
                contactSuggestionState = ContactSuggestionState(
                    areSuggestionsExpanded = areContactSuggestionsExpanded[ContactSuggestionsField.BCC]
                        ?: false,
                    contactSuggestionItems = contactSuggestions[ContactSuggestionsField.BCC]?.map {
                        it.toSuggestionContactItem()
                    } ?: emptyList()
                )
            )
        }

        LaunchedEffect(key1 = recipientsOpen) {
            recipientsButtonRotation.animateTo(
                if (recipientsOpen) RecipientsButtonRotationValues.Open else RecipientsButtonRotationValues.Closed
            )
        }
    }
}

private object RecipientsButtonRotationValues {

    const val Open = 180f
    const val Closed = 0f
}

private fun ChipItem.toRecipientUiModel(): RecipientUiModel? = when (this) {
    is ChipItem.Counter -> null
    is ChipItem.Invalid -> RecipientUiModel.Invalid(value)
    is ChipItem.Valid -> RecipientUiModel.Valid(value)
}

private fun RecipientUiModel.toChipItem(): ChipItem = when (this) {
    is RecipientUiModel.Invalid -> ChipItem.Invalid(address)
    is RecipientUiModel.Valid -> ChipItem.Valid(address)
}

@Composable
private fun ContactSuggestionUiModel.toSuggestionContactItem(): ContactSuggestionItem = when (this) {
    is ContactSuggestionUiModel.Contact -> ContactSuggestionItem(
        this.name,
        this.email,
        listOf(this.email)
    )

    is ContactSuggestionUiModel.ContactGroup -> ContactSuggestionItem(
        this.name,
        TextUiModel.PluralisedText(
            value = R.plurals.composer_recipient_suggestion_contacts,
            count = this.emails.size
        ).string(),
        this.emails
    )
}
