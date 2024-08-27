package ch.protonmail.android.uicomponents.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.uicomponents.verticalScrollbar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.util.kotlin.takeIfNotBlank

/*
    Composable that displays a TextField where the user can type and
    a chips list will be created from the text typed. A chip can be added
    when the user presses space or when the focus is lost from the field (by tapping in
    a different field or tapping the keyboard in a key that moves the focus away).

    When suggestion options are provided, it also displays DropdownMenu and auto-types
    them into TextField on click.
 */
@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun ChipsListTextField(
    value: List<ChipItem>,
    modifier: Modifier = Modifier,
    chipValidator: (String) -> Boolean = { true },
    onListChanged: (List<ChipItem>) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester? = null,
    cursorColor: Color = ProtonTheme.colors.brandDarken20,
    textStyle: TextStyle = ProtonTheme.typography.defaultSmallNorm,
    animateChipsCreation: Boolean = false,
    actions: ChipsListTextField.Actions,
    contactSuggestionState: ContactSuggestionState
) {
    val state by remember { mutableStateOf(ChipsListState(chipValidator, onListChanged)) }

    state.updateItems(value)

    val focusManager = LocalFocusManager.current
    val localDensity = LocalDensity.current
    val localConfiguration = LocalConfiguration.current
    var textMaxWidth by remember { mutableStateOf(Dp.Unspecified) }
    FlowRow(
        modifier = modifier
            .defaultMinSize(minWidth = 50.dp)
            .onSizeChanged { size ->
                if (textMaxWidth == Dp.Unspecified) {
                    textMaxWidth = with(localDensity) { size.width.toDp() }
                }
            },
        verticalArrangement = Arrangement.Center
    ) {

        when (val items = state.getItems()) {
            ChipItemsList.Empty -> Unit
            is ChipItemsList.Focused -> FocusedChipsList(
                items.items,
                animateChipsCreation,
                textMaxWidth
            ) { state.onDelete(it) }

            is ChipItemsList.Unfocused.Multiple -> UnFocusedChipsList(
                items.item,
                items.counter
            ) { focusRequester?.requestFocus() }

            is ChipItemsList.Unfocused.Single -> UnFocusedChipsList(items.item) { focusRequester?.requestFocus() }
        }

        ExposedDropdownMenuBox(
            expanded = contactSuggestionState.areSuggestionsExpanded,
            onExpandedChange = {}
        ) {
            BasicTextField(
                modifier = Modifier
                    .testTag(ChipsTestTags.BasicTextField)
                    .thenIf(focusRequester != null) {
                        focusRequester(focusRequester!!)
                    }
                    .thenIf(!state.isFocused()) {
                        height(0.dp)
                    }
                    .padding(16.dp)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Backspace) {
                            state.onDelete()
                            true
                        } else {
                            false
                        }
                    }
                    .onFocusChanged { focusChange ->
                        state.typeWord(state.getTypedText())
                        state.setFocusState(focusChange.isFocused)
                        if (!focusChange.hasFocus) actions.onSuggestionsDismissed()
                    }
                    .menuAnchor(),
                value = state.getTypedText(),
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(
                    onNext = {
                        state.typeWord(state.getTypedText())
                        focusManager.moveFocus(FocusDirection.Next)
                    },
                    onDone = {
                        state.typeWord(state.getTypedText())
                        focusManager.clearFocus()
                    },
                    onPrevious = {
                        state.typeWord(state.getTypedText())
                        focusManager.moveFocus(FocusDirection.Previous)
                    }
                ),
                onValueChange = { newText ->
                    state.type(newText)
                    actions.onSuggestionTermTyped(newText)
                },
                cursorBrush = SolidColor(cursorColor),
                textStyle = textStyle
            )

            val suggestionScrollState = rememberScrollState(initial = 0)

            val dropDownMenuBackground = if (isSystemInDarkTheme()) {
                ProtonTheme.colors.backgroundSecondary
            } else {
                ProtonTheme.colors.backgroundNorm
            }

            LaunchedEffect(localConfiguration.orientation) {
                if (contactSuggestionState.areSuggestionsExpanded) {
                    actions.onSuggestionsDismissed()
                }
            }

            if (contactSuggestionState.contactSuggestionItems.isNotEmpty()) {
                LaunchedEffect(contactSuggestionState) {
                    // auto-scroll to first item on each contact suggestions change
                    //
                    // we do it also when suggestions visibility changes,
                    // because we want to scroll even if the results are the same
                    suggestionScrollState.animateScrollTo(0)
                }

                ExposedDropdownMenu(
                    modifier = Modifier
                        .background(dropDownMenuBackground)
                        .exposedDropdownSize(false)
                        .fillMaxWidth(DROP_DOWN_WIDTH_PERCENT)
                        .fillMaxHeight(DROP_DOWN_HEIGHT_PERCENT)
                        .verticalScrollbar(suggestionScrollState),
                    expanded = contactSuggestionState.areSuggestionsExpanded,
                    onDismissRequest = {
                        actions.onSuggestionsDismissed()
                    },
                    scrollState = suggestionScrollState
                ) {
                    contactSuggestionState.contactSuggestionItems.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = ProtonDimens.SmallSpacing)) {
                                    Text(
                                        text = selectionOption.header,
                                        maxLines = 1,
                                        color = ProtonTheme.colors.textNorm,
                                        style = ProtonTheme.typography.defaultNorm,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
                                    Text(
                                        text = selectionOption.subheader,
                                        maxLines = 1,
                                        color = ProtonTheme.colors.textWeak,
                                        style = ProtonTheme.typography.defaultSmallWeak,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                selectionOption.emails.joinToString(separator = " ").takeIfNotBlank()?.let {
                                    state.typeWord(it)
                                }
                                actions.onSuggestionsDismissed()
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

    }
}

private const val DROP_DOWN_HEIGHT_PERCENT = 0.8f
private const val DROP_DOWN_WIDTH_PERCENT = 0.9f

object ChipsListTextField {
    data class Actions(
        val onSuggestionTermTyped: (String) -> Unit,
        val onSuggestionsDismissed: () -> Unit
    )
}
