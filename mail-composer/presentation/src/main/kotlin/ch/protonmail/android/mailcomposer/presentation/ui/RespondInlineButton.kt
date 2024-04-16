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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens

@Composable
internal fun RespondInlineButton(onRespondInlineClick: () -> Unit, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        ProtonTextButton(
            onClick = onRespondInlineClick,
            modifier = Modifier.align(CenterEnd)
        ) {
            Text(text = stringResource(id = R.string.respondInline))
        }
    }
}
