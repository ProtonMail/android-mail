package ch.protonmail.android.uicomponents.chips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.uicomponents.chips.ChipsListState2.Companion.ChipsCreationRegex
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItem2
import ch.protonmail.android.uicomponents.composer.suggestions.ContactSuggestionItemElement
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun ChipsListField2(
    label: String,
    value: List<ChipItem>,
    modifier: Modifier = Modifier,
    chipValidator: (String) -> Boolean = { true },
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = true,
    actions: ChipsListField2.Actions,
    contactSuggestionState: ContactSuggestionState2,
    chevronIconContent: @Composable () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by remember { mutableStateOf(ChipsListState2(chipValidator, actions.onListChanged)) }
    var textFieldValue by remember { mutableStateOf(initialTextFieldValue) }

    val chipsListActions = remember {
        ChipsListTextField2.Actions(
            onTextChanged = { value ->
                state.type(value.text)
                textFieldValue = if (ChipsCreationRegex.containsMatchIn(value.text)) initialTextFieldValue else value
                actions.onSuggestionTermTyped(textFieldValue.text)
            },
            onFocusChanged = { focusChange ->
                state.setFocusState(focusChange.isFocused)
                if (!focusChange.hasFocus) {
                    state.createChip()
                    textFieldValue = initialTextFieldValue
                    actions.onSuggestionsDismissed()
                }
            },
            onItemDeleted = {
                it?.let { state.onDelete(it) } ?: state.onDelete()
            },
            onTriggerChipCreation = {
                state.createChip()
                textFieldValue = initialTextFieldValue
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

            ChipsListTextField2(
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
                textFieldValue = textFieldValue,
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
                ContactSuggestionItemElement(textFieldValue.text, selectionOption, onClick = {
                    actions.onSuggestionsDismissed()
                    state.typeWord(it)
                    textFieldValue = initialTextFieldValue
                })
            }
        }
    }
}

private val initialTextFieldValue = TextFieldValue("")

@Stable
data class ContactSuggestionState2(
    val areSuggestionsExpanded: Boolean,
    val contactSuggestionItems: List<ContactSuggestionItem2>
)

object ChipsListField2 {
    data class Actions(
        val onSuggestionTermTyped: (String) -> Unit,
        val onSuggestionsDismissed: () -> Unit,
        val onListChanged: (List<ChipItem>) -> Unit
    )
}
