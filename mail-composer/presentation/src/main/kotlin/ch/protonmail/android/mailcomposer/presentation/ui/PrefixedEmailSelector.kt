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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.uicomponents.text.defaultTextFieldColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
internal fun PrefixedEmailSelector(
    @StringRes prefixStringResource: Int,
    modifier: Modifier = Modifier,
    selectedEmail: String,
    onChangeSender: () -> Unit
) {
    Row(modifier) {
        TextField(
            value = selectedEmail,
            onValueChange = { },
            modifier = Modifier
                .testTag(PrefixedEmailSelectorTestTags.TextField)
                .align(Alignment.CenterVertically)
                .weight(1f),
            readOnly = true,
            textStyle = ProtonTheme.typography.defaultNorm,
            prefix = {
                Row {
                    Text(
                        modifier = Modifier.testTag(ComposerTestTags.FieldPrefix),
                        text = stringResource(prefixStringResource),
                        color = ProtonTheme.colors.textWeak,
                        style = ProtonTheme.typography.defaultNorm
                    )
                    Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.defaultTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email
            )
        )

        ChangeSenderButton(Modifier.align(Alignment.CenterVertically), onChangeSender)
    }
}

@Composable
private fun ChangeSenderButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        modifier = modifier
            .testTag(ComposerTestTags.ChangeSenderButton),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_three_dots_vertical),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

object PrefixedEmailSelectorTestTags {

    const val TextField = "TextField"
}
