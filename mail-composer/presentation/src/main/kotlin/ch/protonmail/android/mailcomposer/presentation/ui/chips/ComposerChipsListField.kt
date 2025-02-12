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

package ch.protonmail.android.mailcomposer.presentation.ui.chips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.delete
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerChipsListViewModel
import ch.protonmail.android.uicomponents.chips.ChipsListField
import ch.protonmail.android.uicomponents.chips.ChipsListTextField
import ch.protonmail.android.uicomponents.chips.ChipsTestTags
import ch.protonmail.android.uicomponents.chips.ContactSuggestionState
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItemElement
import ch.protonmail.android.uicomponents.thenIf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.presentation.utils.showToast

@Composable
fun ComposerChipsListField(
    label: String,
    chipsList: List<ChipItem>,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = true,
    actions: ChipsListField.Actions,
    contactSuggestionState: ContactSuggestionState,
    chevronIconContent: @Composable () -> Unit = {}
) {
    // Every chip field needs its specific VM instance, as it can't be shared.
    // We use the `label` String as key, as it's stable and won't change throughout the whole lifecycle.
    val composerChipsListViewModel = hiltViewModel<ComposerChipsListViewModel>(key = label)
    val context = LocalContext.current

    val state by composerChipsListViewModel.state.collectAsStateWithLifecycle()
    val listState = state.listState
    val textFieldState = composerChipsListViewModel.textFieldState

    LaunchedEffect(chipsList) {
        listState.updateItems(chipsList)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val chipsListActions = remember {
        ChipsListTextField.Actions(
            onFocusChanged = { focusChange ->
                listState.setFocusState(focusChange.isFocused)
                if (!focusChange.hasFocus) {
                    listState.createChip()
                    textFieldState.edit { delete(0, length) }
                    actions.onSuggestionsDismissed()
                }
            },
            onItemDeleted = {
                it?.let { listState.onDelete(it) } ?: listState.onDelete()
            },
            onTriggerChipCreation = {
                listState.createChip()
                textFieldState.edit { delete(0, length) }
                actions.onSuggestionsDismissed()
            }
        )
    }

    BackHandler(contactSuggestionState.areSuggestionsExpanded) {
        actions.onSuggestionsDismissed()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .testTag(ChipsTestTags.FieldPrefix)
                    .align(Alignment.Top)
                    .padding(vertical = ProtonDimens.DefaultSpacing)
                    .padding(start = ProtonDimens.DefaultSpacing),
                color = ProtonTheme.colors.textWeak,
                style = ProtonTheme.typography.defaultNorm
            )

            ChipsListTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .thenIf(focusOnClick) {
                        clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { focusRequester?.requestFocus() }
                        )
                    },
                textFieldState = textFieldState,
                state = listState,
                focusRequester = focusRequester,
                actions = chipsListActions
            )

            chevronIconContent()
        }

        if (contactSuggestionState.areSuggestionsExpanded &&
            contactSuggestionState.contactSuggestionItems.isNotEmpty()
        ) {
            Divider(modifier = Modifier.padding(bottom = ProtonDimens.DefaultSpacing))

            contactSuggestionState.contactSuggestionItems.forEach { selectionOption ->
                ContactSuggestionItemElement(textFieldState.text.toString(), selectionOption, onClick = {
                    actions.onSuggestionsDismissed()
                    listState.typeWord(it)
                    textFieldState.edit { delete(0, length) }
                })
            }
        }
    }

    ConsumableLaunchedEffect(state.listChanged) {
        actions.onListChanged(it)
    }

    ConsumableLaunchedEffect(state.suggestionsTermTyped) {
        actions.onSuggestionTermTyped(it)
    }

    ConsumableTextEffect(state.duplicateRemovalWarning) {
        context.showToast(it)
    }

    ConsumableTextEffect(state.invalidEntryWarning) {
        context.showToast(it)
    }
}
