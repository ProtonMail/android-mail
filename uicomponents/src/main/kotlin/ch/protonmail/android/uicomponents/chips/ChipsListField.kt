package ch.protonmail.android.uicomponents.chips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm

/*
    Composable that displays a ChipsListTextField with a label in a row (useful for forms)
 */
@Composable
fun ChipsListField(
    label: String,
    value: List<ChipItem>,
    onListChanged: (List<ChipItem>) -> Unit,
    modifier: Modifier = Modifier,
    chipValidator: (String) -> Boolean = { true },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = label,
            modifier = Modifier
                .testTag(ChipsTestTags.FieldPrefix)
                .align(Alignment.Top)
                .padding(top = 16.dp, bottom = 16.dp, start = 0.dp),
            color = ProtonTheme.colors.textWeak,
            style = ProtonTheme.typography.defaultSmallNorm
        )
        ChipsListTextField(
            modifier = Modifier
                .weight(1f)
                .thenIf(focusOnClick) {
                    clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { focusRequester?.requestFocus() }
                    )
                },
            chipValidator = chipValidator,
            onListChanged = onListChanged,
            value = value,
            keyboardOptions = keyboardOptions,
            focusRequester = focusRequester
        )
    }
}

fun Modifier.thenIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier())
    } else {
        this
    }
}
