package ch.protonmail.android.uicomponents.chips

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp

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
    focusRequester: FocusRequester? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.Top)
                .padding(top = 16.dp, bottom = 16.dp)
        )
        ChipsListTextField(
            chipValidator = chipValidator,
            onListChanged = onListChanged,
            value = value,
            keyboardOptions = keyboardOptions,
            focusRequester = focusRequester
        )
    }
}
