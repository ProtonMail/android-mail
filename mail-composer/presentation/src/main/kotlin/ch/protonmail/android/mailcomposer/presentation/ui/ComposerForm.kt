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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.FocusableForm
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.uicomponents.chips.ChipItem
import ch.protonmail.android.uicomponents.chips.ChipsListField
import ch.protonmail.android.uicomponents.chips.ContactSuggestionState
import ch.protonmail.android.uicomponents.chips.SuggestionItem
import ch.protonmail.android.uicomponents.chips.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import timber.log.Timber

@Composable
internal fun ComposerForm(
    emailValidator: (String) -> Boolean,
    recipientsOpen: Boolean,
    initialFocus: FocusedFieldType,
    fields: ComposerFields,
    replaceDraftBody: Effect<TextUiModel>,
    shouldForceBodyTextFocus: Effect<Unit>,
    actions: ComposerFormActions,
    contactSuggestions: Map<ContactSuggestionsField, List<ContactSuggestionUiModel>>,
    areContactSuggestionsExpanded: Map<ContactSuggestionsField, Boolean>
) {
    val maxWidthModifier = Modifier.fillMaxWidth()
    val emailNextKeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Email
    )
    val recipientsButtonRotation = remember { Animatable(0F) }

    FocusableForm(
        fieldList = listOf(
            FocusedFieldType.TO,
            FocusedFieldType.CC,
            FocusedFieldType.BCC,
            FocusedFieldType.SUBJECT,
            FocusedFieldType.BODY
        ),
        initialFocus = initialFocus,
        onFocusedField = {
            Timber.d("Focus changed: onFocusedField: $it")
            actions.onFocusChanged(it)
        }
    ) { fieldFocusRequesters ->
        Column(
            modifier = maxWidthModifier
        ) {
            PrefixedEmailSelector(
                prefixStringResource = R.string.from_prefix,
                modifier = maxWidthModifier.testTag(ComposerTestTags.FromSender),
                selectedEmail = fields.sender.email,
                actions.onChangeSender
            )
            MailDivider()
            Row(
                modifier = maxWidthModifier,
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
                        suggestionItems = contactSuggestions[ContactSuggestionsField.TO]?.map {
                            it.toSuggestionContactItem()
                        } ?: emptyList()
                    )
                )
                Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
                IconButton(
                    modifier = Modifier,
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
                            .rotate(recipientsButtonRotation.value),
                        imageVector = Icons.Filled.KeyboardArrowUp,
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
                            suggestionItems = contactSuggestions[ContactSuggestionsField.CC]?.map {
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
                            suggestionItems = contactSuggestions[ContactSuggestionsField.BCC]?.map {
                                it.toSuggestionContactItem()
                            } ?: emptyList()
                        )
                    )
                }
            }
            MailDivider()
            SubjectTextField(
                initialValue = fields.subject,
                onSubjectChange = actions.onSubjectChanged,
                modifier = maxWidthModifier
                    .testTag(ComposerTestTags.Subject)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.SUBJECT)
            )
            MailDivider()
            BodyTextField(
                initialValue = fields.body,
                shouldRequestFocus = shouldForceBodyTextFocus,
                replaceDraftBody = replaceDraftBody,
                onBodyChange = actions.onBodyChanged,
                modifier = maxWidthModifier
                    .testTag(ComposerTestTags.MessageBody)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.BODY)
            )
            if (fields.quotedBody != null) {
                BodyHtmlQuote(
                    value = fields.quotedBody.styled.value,
                    modifier = maxWidthModifier.testTag(ComposerTestTags.MessageHtmlQuotedBody)
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

private fun ChipItem.toRecipientUiModel(): RecipientUiModel? = when (this) {
    is ChipItem.Counter -> null
    is ChipItem.Invalid -> RecipientUiModel.Invalid(value)
    is ChipItem.Valid -> RecipientUiModel.Valid(value)
}

private fun RecipientUiModel.toChipItem(): ChipItem = when (this) {
    is RecipientUiModel.Invalid -> ChipItem.Invalid(address)
    is RecipientUiModel.Valid -> ChipItem.Valid(address)
}

private fun ContactSuggestionUiModel.toSuggestionContactItem(): SuggestionItem = SuggestionItem(
    this.name,
    this.email
)
