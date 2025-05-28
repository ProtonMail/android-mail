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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailmailbox.presentation.R
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3

@Composable
fun IncludeSpamTrashItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ProtonDimens.SmallSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.mailbox_include_all_desc),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(ProtonDimens.SmallSpacing)
        )
        ProtonButton(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = ButtonDefaults.MinHeight),
            shape = ProtonTheme.shapes.medium,
            border = null,
            elevation = null,
            colors = ButtonDefaults.protonButtonColors(),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Text(
                text = stringResource(R.string.mailbox_include_all_button)
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun UpsellingScreenContentPreview_PromoA() {
    ProtonTheme3 {
        IncludeSpamTrashItem(onClick = {}, modifier = Modifier)
    }
}
