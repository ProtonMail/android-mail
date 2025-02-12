package ch.protonmail.android.uicomponents.chips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.uicomponents.chips.ChipsListState.Companion.ChipsCreationRegex
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItemElement
import ch.protonmail.android.uicomponents.thenIf
import kotlinx.coroutines.flow.collectLatest
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun ChipsListField(
    label: String,
    value: List<ChipItem>,
    modifier: Modifier = Modifier,
    chipValidator: (String) -> Boolean = { true },
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = true,
    actions: ChipsListField.Actions,
    contactSuggestionState: ContactSuggestionState,
    chevronIconContent: @Composable () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by remember { mutableStateOf(ChipsListState(chipValidator, actions.onListChanged)) }
    val textFieldState = rememberTextFieldState()

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }
            .collectLatest {
                state.type(textFieldState.text.toString())
                if (ChipsCreationRegex.containsMatchIn(textFieldState.text)) textFieldState.edit { delete(0, length) }
                actions.onSuggestionTermTyped(textFieldState.text.toString())
            }
    }

    val chipsListActions = remember {
        ChipsListTextField.Actions(
            onFocusChanged = { focusChange ->
                state.setFocusState(focusChange.isFocused)
                if (!focusChange.hasFocus) {
                    state.createChip()
                    textFieldState.edit { delete(0, length) }
                    actions.onSuggestionsDismissed()
                }
            },
            onItemDeleted = {
                it?.let { state.onDelete(it) } ?: state.onDelete()
            },
            onTriggerChipCreation = {
                state.createChip()
                textFieldState.edit { delete(0, length) }
                actions.onSuggestionsDismissed()
            }
        )
    }

    state.updateItems(value)

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
                state = state,
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
                    state.typeWord(it)
                    textFieldState.edit { delete(0, length) }
                })
            }
        }
    }
}

@Stable
data class ContactSuggestionState(
    val areSuggestionsExpanded: Boolean,
    val contactSuggestionItems: List<ContactSuggestionItem>
)

object ChipsListField {
    data class Actions(
        val onSuggestionTermTyped: (String) -> Unit,
        val onSuggestionsDismissed: () -> Unit,
        val onListChanged: (List<ChipItem>) -> Unit
    )
}
