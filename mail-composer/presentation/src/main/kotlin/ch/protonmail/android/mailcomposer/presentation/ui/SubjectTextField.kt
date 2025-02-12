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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import ch.protonmail.android.mailcomposer.presentation.R
import kotlinx.coroutines.flow.collectLatest
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
@Deprecated("Part of Composer V1, to be replaced with SubjectTextField2")
internal fun SubjectTextField(
    initialValue: String,
    onSubjectChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldState = rememberTextFieldState(initialValue)

    val keyboardOptions = remember {
        KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    }

    var userUpdated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text }
            .collectLatest {
                if (it.toString() != initialValue) userUpdated = true

                // This skips the onSubjectChange call when the SubjectTextField enters the composition
                // without the user making any change to prevent an invalid draft creation.
                if (userUpdated) onSubjectChange(it.toString())
            }
    }

    BasicTextField(
        modifier = modifier,
        state = textFieldState,
        textStyle = ProtonTheme.typography.defaultNorm,
        cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = keyboardOptions,
        decorator = @Composable { innerTextField ->
            if (textFieldState.text.isEmpty()) {
                PlaceholderText()
            }
            innerTextField()
        }
    )
}

@Composable
private fun PlaceholderText() {
    Text(
        modifier = Modifier.testTag(ComposerTestTags.SubjectPlaceholder),
        text = stringResource(R.string.subject_placeholder),
        color = ProtonTheme.colors.textHint,
        style = ProtonTheme.typography.defaultNorm
    )
}
