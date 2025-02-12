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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import ch.protonmail.android.uicomponents.chips.ContactSuggestionState
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItem
import ch.protonmail.android.uicomponents.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import timber.log.Timber

@Composable
@Deprecated("Part of Composer V1, to be replaced with RecipientFields2")
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
    val recipientsButtonRotation = remember { Animatable(0F) }
    val isShowingToSuggestions = areContactSuggestionsExpanded[ContactSuggestionsField.TO] == true
    val isShowingCcSuggestions = areContactSuggestionsExpanded[ContactSuggestionsField.CC] == true
    val isShowingBccSuggestions = areContactSuggestionsExpanded[ContactSuggestionsField.BCC] == true
    val hasCcBccContent = fields.cc.isNotEmpty() || fields.bcc.isNotEmpty()
    val shouldShowCcBcc = recipientsOpen || hasCcBccContent

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        ChipsListField(
            label = stringResource(id = R.string.to_prefix),
            value = fields.to.map { it.toChipItem() },
            chipValidator = emailValidator,
            modifier = Modifier
                .weight(1f)
                .testTag(ComposerTestTags.ToRecipient)
                .retainFieldFocusOnConfigurationChange(FocusedFieldType.TO),
            focusRequester = fieldFocusRequesters[FocusedFieldType.TO],
            actions = ChipsListField.Actions(
                onSuggestionTermTyped = {
                    actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.TO)
                },
                onSuggestionsDismissed = {
                    if (isShowingToSuggestions) actions.onContactSuggestionsDismissed(ContactSuggestionsField.TO)
                },
                onListChanged = {
                    actions.onToChanged(it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() })
                }
            ),
            contactSuggestionState = ContactSuggestionState(
                areSuggestionsExpanded = areContactSuggestionsExpanded[ContactSuggestionsField.TO] ?: false,
                contactSuggestionItems = contactSuggestions[ContactSuggestionsField.TO]?.map {
                    it.toSuggestionContactItem()
                } ?: emptyList()
            ),
            chevronIconContent = {
                if (!hasCcBccContent) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .focusProperties { canFocus = false },
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
            }
        )
    }

    if (shouldShowCcBcc && !isShowingToSuggestions) {
        Column {
            MailDivider()
            ChipsListField(
                label = stringResource(id = R.string.cc_prefix),
                value = fields.cc.map { it.toChipItem() },
                chipValidator = emailValidator,
                modifier = Modifier
                    .testTag(ComposerTestTags.CcRecipient)
                    .retainFieldFocusOnConfigurationChange(FocusedFieldType.CC),
                focusRequester = fieldFocusRequesters[FocusedFieldType.CC],
                actions = ChipsListField.Actions(
                    onSuggestionTermTyped = {
                        actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.CC)
                    },
                    onSuggestionsDismissed = {
                        if (isShowingCcSuggestions) actions.onContactSuggestionsDismissed(ContactSuggestionsField.CC)
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

            if (!isShowingCcSuggestions) {
                MailDivider()
                ChipsListField(
                    label = stringResource(id = R.string.bcc_prefix),
                    value = fields.bcc.map { it.toChipItem() },
                    chipValidator = emailValidator,
                    modifier = Modifier
                        .testTag(ComposerTestTags.BccRecipient)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.BCC),
                    focusRequester = fieldFocusRequesters[FocusedFieldType.BCC],
                    actions = ChipsListField.Actions(
                        onSuggestionTermTyped = {
                            actions.onContactSuggestionTermChanged(it, ContactSuggestionsField.BCC)
                        },
                        onSuggestionsDismissed = {
                            if (isShowingBccSuggestions)
                                actions.onContactSuggestionsDismissed(ContactSuggestionsField.BCC)
                        },
                        onListChanged = {
                            actions.onBccChanged(it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() })
                        }
                    ),
                    contactSuggestionState = ContactSuggestionState(
                        areSuggestionsExpanded = areContactSuggestionsExpanded[ContactSuggestionsField.BCC] ?: false,
                        contactSuggestionItems = contactSuggestions[ContactSuggestionsField.BCC]?.map {
                            it.toSuggestionContactItem()
                        } ?: emptyList()
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
