package ch.protonmail.android.uicomponents.chips

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.protonmail.android.uicomponents.chips.item.ChipItemsList
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
internal fun ChipsListTextField2(
    state: ChipsListState2,
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    cursorColor: Color = ProtonTheme.colors.brandDarken20,
    textStyle: TextStyle = ProtonTheme.typography.defaultNorm,
    animateChipsCreation: Boolean = false,
    actions: ChipsListTextField2.Actions
) {
    val focusManager = LocalFocusManager.current
    val localDensity = LocalDensity.current
    var textMaxWidth by remember { mutableStateOf(Dp.Unspecified) }

    var rect by remember { mutableStateOf(Rect.Zero) }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardOptions = remember {
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrect = false)
    }

    // Similar to what we do for the Message body, we need to ensure
    // that the cursor is always on screen when the user types.
    fun bringRectIntoView(rect: Rect) = coroutineScope.launch { bringIntoViewRequester.bringIntoView(rect) }

    LaunchedEffect(textFieldValue.text) {
        bringRectIntoView(rect)
    }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (textMaxWidth == Dp.Unspecified) {
                    textMaxWidth = with(localDensity) { size.width.toDp() }
                }
            },
        verticalArrangement = Arrangement.Center
    ) {
        val items = state.getItems()

        when (items) {
            ChipItemsList.Empty -> Unit
            is ChipItemsList.Focused -> FocusedChipsList(
                items.items,
                animateChipsCreation,
                textMaxWidth
            ) { actions.onItemDeleted(it) }

            is ChipItemsList.Unfocused.Multiple -> UnFocusedChipsList(
                items.item,
                items.counter
            ) { focusRequester?.requestFocus() }

            is ChipItemsList.Unfocused.Single -> UnFocusedChipsList(items.item) { focusRequester?.requestFocus() }
        }

        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .align(Alignment.CenterVertically)
                .testTag(ChipsTestTags.BasicTextField)
                .bringIntoViewRequester(bringIntoViewRequester)
                .thenIf(focusRequester != null) {
                    focusRequester(focusRequester!!)
                }
                .thenIf(!state.isFocused() && items !is ChipItemsList.Empty) {
                    height(0.dp)
                }
                .padding(vertical = ProtonDimens.DefaultSpacing)
                .padding(start = ExtraSmallSpacing)
                .padding(end = ProtonDimens.DefaultSpacing)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Backspace) {
                        actions.onItemDeleted(null)
                        bringRectIntoView(rect)
                        true
                    } else {
                        false
                    }
                }
                .onFocusChanged { actions.onFocusChanged(it) },
            value = textFieldValue,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(
                onNext = {
                    // If there's some text, trigger the chip creation and do not move the focus.
                    if (textFieldValue.text.isNotEmpty()) {
                        actions.onTriggerChipCreation()
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                },
                onDone = {
                    focusManager.clearFocus()
                },
                onPrevious = {
                    focusManager.moveFocus(FocusDirection.Previous)
                }
            ),
            onValueChange = {
                // Triggering the chip creation manually causes the same value to be dispatched again.
                // This is needed for some custom keyboards such as SwiftKey.
                if (it.text == textFieldValue.text && it.selection == textFieldValue.selection) return@BasicTextField

                actions.onTextChanged(it)
            },
            onTextLayout = {
                rect = it.getCursorRect(textFieldValue.selection.end)
            },
            cursorBrush = SolidColor(cursorColor),
            textStyle = textStyle
        )
    }
}

object ChipsListTextField2 {
    data class Actions(
        val onTextChanged: (value: TextFieldValue) -> Unit,
        val onFocusChanged: (focusChange: FocusState) -> Unit,
        val onItemDeleted: (index: Int?) -> Unit,
        val onTriggerChipCreation: () -> Unit
    )
}
