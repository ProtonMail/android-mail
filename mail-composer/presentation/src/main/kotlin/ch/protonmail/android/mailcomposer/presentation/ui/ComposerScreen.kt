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

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.ui.Composer.MessageBodyPortraitMinLines
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultNorm

@Composable
fun ComposerScreen(
    onCloseComposerClick: () -> Unit
) {
    val maxWidthModifier = Modifier.fillMaxWidth()
    val focusRequester = remember { FocusRequester() }
    var focusedField by rememberSaveable(inputs = emptyArray()) { mutableStateOf(FocusedFieldType.TO) }

    fun Modifier.retainFieldFocus(fieldType: FocusedFieldType): Modifier =
        if (focusedField == fieldType) {
            focusRequester(focusRequester)
        } else {
            this
        }.onFocusChanged {
            if (it.isFocused) focusedField = fieldType
        }

    Column {
        ComposerTopBar(onCloseComposerClick = onCloseComposerClick)
        Column(
            modifier = maxWidthModifier
                .verticalScroll(rememberScrollState(), reverseScrolling = true)
        ) {
            PrefixedEmailTextField(
                prefixStringResource = R.string.from_prefix,
                modifier = maxWidthModifier
            )
            MailDivider()
            PrefixedEmailTextField(
                prefixStringResource = R.string.to_prefix,
                modifier = maxWidthModifier
                    .retainFieldFocus(FocusedFieldType.TO)
            )
            MailDivider()
            SubjectTextField(
                maxWidthModifier
                    .retainFieldFocus(FocusedFieldType.SUBJECT)
            )
            MailDivider()
            BodyTextField(
                maxWidthModifier
                    .retainFieldFocus(FocusedFieldType.BODY)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ComposerTopBar(onCloseComposerClick: () -> Unit) {
    ProtonTopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onCloseComposerClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.close_composer_content_description)
                )
            }
        },
        actions = {
            IconButton(onClick = {}, enabled = false) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_paper_plane),
                    tint = ProtonTheme.colors.iconDisabled,
                    contentDescription = stringResource(R.string.send_message_content_description)
                )
            }
        }
    )
}

@Composable
private fun PrefixedEmailTextField(@StringRes prefixStringResource: Int, modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.defaultNorm,
        prefix = {
            Row {
                Text(
                    text = stringResource(prefixStringResource),
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.defaultNorm
                )
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
            }
        },
        colors = TextFieldDefaults.composerTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email
        )
    )
}

@Composable
private fun SubjectTextField(modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.defaultNorm,
        colors = TextFieldDefaults.composerTextFieldColors(),
        maxLines = 3,
        placeholder = {
            Text(
                text = stringResource(R.string.subject_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun BodyTextField(modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val screenOrientation = LocalConfiguration.current.orientation
    val bodyMinLines = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) MessageBodyPortraitMinLines else 1

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier.fillMaxSize(),
        textStyle = ProtonTheme.typography.defaultNorm,
        minLines = bodyMinLines,
        colors = TextFieldDefaults.composerTextFieldColors(),
        placeholder = {
            Text(
                text = stringResource(R.string.compose_message_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        }
    )
}

@Composable
private fun TextFieldDefaults.composerTextFieldColors(): TextFieldColors =
    colors(
        focusedTextColor = ProtonTheme.colors.textNorm,
        focusedContainerColor = ProtonTheme.colors.backgroundNorm,
        unfocusedContainerColor = ProtonTheme.colors.backgroundNorm,
        focusedLabelColor = ProtonTheme.colors.textNorm,
        unfocusedLabelColor = ProtonTheme.colors.textHint,
        disabledLabelColor = ProtonTheme.colors.textDisabled,
        errorLabelColor = ProtonTheme.colors.notificationError,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen(onCloseComposerClick = {})
    }
}

private object Composer {

    const val MessageBodyPortraitMinLines = 6
}

private enum class FocusedFieldType {
    TO,
    SUBJECT,
    BODY
}
