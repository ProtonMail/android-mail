package ch.protonmail.android.uicomponents.chips

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.util.kotlin.takeIfNotBlank

@Stable
sealed class ChipItem(open val value: String) {

    data class Valid(override val value: String) : ChipItem(value)
    data class Invalid(override val value: String) : ChipItem(value)
    data class Counter(override val value: String) : ChipItem(value)
}

/*
    Composable that displays a TextField where the user can type and
    a chips list will be created from the text typed. A chip can be added
    when the user presses space or when the focus is lost from the field (by tapping in
    a different field or tapping the keyboard in a key that moves the focus away).
 */
@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
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
    animateChipsCreation: Boolean = false
) {
    val state by remember { mutableStateOf(ChipsListState(chipValidator, onListChanged)) }

    state.updateItems(value)

    val focusManager = LocalFocusManager.current
    val localDensity = LocalDensity.current
    var textMaxWidth by remember { mutableStateOf(Dp.Unspecified) }
    FlowRow(
        modifier = modifier
            .defaultMinSize(minWidth = 50.dp)
            .onSizeChanged { size ->
                if (textMaxWidth == Dp.Unspecified) {
                    textMaxWidth = with(localDensity) { size.width.toDp() }
                }
            },
        verticalAlignment = Alignment.CenterVertically
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
                },
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
            onValueChange = { newText -> state.type(newText) },
            cursorBrush = SolidColor(cursorColor),
            textStyle = textStyle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusedChipsList(
    chipItems: List<ChipItem>,
    animateChipsCreation: Boolean = false,
    textMaxWidth: Dp,
    onDeleteItem: (Int) -> Unit
) {
    chipItems.forEachIndexed { index, chipItem ->
        val scale by remember { mutableStateOf(Animatable(0F)) }
        val alpha by remember { mutableStateOf(Animatable(0F)) }
        InputChip(
            modifier = Modifier
                .testTag("${ChipsTestTags.InputChip}$index")
                .semantics { isValidField = chipItem !is ChipItem.Invalid }
                .padding(horizontal = 4.dp)
                .thenIf(animateChipsCreation) {
                    scale(scale.value)
                    alpha(alpha.value)
                },
            selected = false,
            onClick = { onDeleteItem(index) },
            label = {
                Text(
                    modifier = Modifier
                        .testTag(ChipsTestTags.InputChipText)
                        .widthIn(max = textMaxWidth - 64.dp),
                    text = chipItem.value,
                    color = when (chipItem) {
                        is ChipItem.Invalid -> Color.Red
                        else -> Color.Unspecified
                    }
                )
            },
            shape = chipShape,
            trailingIcon = {
                Icon(
                    Icons.Default.Clear,
                    modifier = Modifier
                        .testTag(ChipsTestTags.InputChipIcon)
                        .size(16.dp),
                    contentDescription = ""
                )
            }
        )
        LaunchedEffect(key1 = index) {
            if (animateChipsCreation) {
                scale.animateTo(1F)
                alpha.animateTo(1F)
            }
        }
    }
}

@Composable
@Suppress("MagicNumber")
private fun UnFocusedChipsList(
    itemChip: ChipItem,
    counterChip: ChipItem? = null,
    onChipClick: () -> Unit = {}
) {
    Row {
        SuggestionChip(
            modifier = Modifier
                .testTag(ChipsTestTags.BaseSuggestionChip)
                .semantics { isValidField = itemChip !is ChipItem.Invalid }
                .weight(1f, fill = false)
                .padding(horizontal = 4.dp),
            onClick = onChipClick,
            label = {
                Text(
                    modifier = Modifier.testTag(ChipsTestTags.InputChipText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = itemChip.value,
                    color = when (itemChip) {
                        is ChipItem.Invalid -> Color.Red
                        else -> Color.Unspecified
                    }
                )
            },
            shape = chipShape
        )
        if (counterChip != null) {
            SuggestionChip(
                modifier = Modifier
                    .testTag(ChipsTestTags.AdditionalSuggestionChip)
                    .semantics { isValidField = itemChip !is ChipItem.Invalid }
                    .padding(horizontal = 4.dp),
                onClick = onChipClick,
                label = {
                    Text(
                        modifier = Modifier.testTag(ChipsTestTags.InputChipText),
                        maxLines = 1,
                        text = counterChip.value,
                        color = Color.Unspecified
                    )
                },
                shape = chipShape
            )
        }
    }
}

@Stable
internal class ChipsListState(
    private val isValid: (String) -> Boolean,
    private val onListChanged: (List<ChipItem>) -> Unit
) {

    private val items: SnapshotStateList<ChipItem> = mutableStateListOf()
    private val typedText: MutableState<String> = mutableStateOf("")
    private val focusedState: MutableState<Boolean> = mutableStateOf(false)

    fun updateItems(newItems: List<ChipItem>) {
        if (newItems != items.toList()) {
            items.clear()
            items.addAll(newItems)
        }
    }

    fun getItems(): ChipItemsList = when {
        items.isEmpty() -> ChipItemsList.Empty
        items.size == 1 && !focusedState.value -> ChipItemsList.Unfocused.Single(items.first())
        items.size > 1 && !focusedState.value -> {
            ChipItemsList.Unfocused.Multiple(items.first(), ChipItem.Counter("+${items.size - 1}"))
        }

        else -> ChipItemsList.Focused(items)
    }

    fun getTypedText(): String = typedText.value

    fun type(newValue: String) {
        when {
            typedText.value + newValue.trim() == EmptyString -> clearTypedText()
            newValue.endsWith(WordSeparator) -> {

                val words = typedText.value.split(
                    WordSeparator,
                    NewLineDelimiter,
                    CarriageReturnNewLineDelimiter,
                    CommaDelimiter,
                    SemiColonDelimiter,
                    TabDelimiter
                ).mapNotNull { it.takeIfNotBlank() }

                if (words.isNotEmpty()) {
                    words.forEach { add(it) }
                    // added here so we only check for duplicates after pasting all the words
                    onListChanged(items)
                    clearTypedText()
                } else typedText.value = newValue
            }

            else -> typedText.value = newValue
        }
    }

    fun typeWord(word: String) {
        type(word)
        type(WordSeparator)
    }

    fun isFocused(): Boolean = focusedState.value

    private fun add(item: String) {
        val chipContent = if (isValid(item)) {
            ChipItem.Valid(item)
        } else {
            ChipItem.Invalid(item)
        }

        items.add(chipContent)

    }

    fun onDelete() {
        if (typedText.value.isEmpty()) {
            items.removeLastOrNull()
        }
        onListChanged(items)
    }

    fun onDelete(index: Int) {
        items.removeAt(index)
        onListChanged(items)
    }

    fun setFocusState(focused: Boolean) {
        focusedState.value = focused
    }

    private fun clearTypedText() {
        typedText.value = EmptyString
    }

    private companion object {

        private const val WordSeparator = " "
        private const val EmptyString = ""
        private const val NewLineDelimiter = "\n"
        private const val CarriageReturnNewLineDelimiter = "\r\n"
        private const val SemiColonDelimiter = ";"
        private const val CommaDelimiter = ","
        private const val TabDelimiter = "\t"
    }
}

private val chipShape = RoundedCornerShape(16.dp)

@Stable
internal sealed class ChipItemsList {

    object Empty : ChipItemsList()

    data class Focused(val items: List<ChipItem>) : ChipItemsList()

    @Stable
    sealed class Unfocused : ChipItemsList() {

        data class Single(val item: ChipItem) : Unfocused()
        data class Multiple(val item: ChipItem, val counter: ChipItem) : Unfocused()
    }
}
