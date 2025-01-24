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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
internal fun ToolbarDisclaimer(text: TextUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.medium
            )
            .background(color = ProtonTheme.colors.backgroundSecondary, shape = ProtonTheme.shapes.medium)
            .padding(ProtonDimens.ListItemTextStartPadding)
    ) {
        Row {
            Icon(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.ic_info_circle),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = text.string(),
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ToolbarDisclaimerPreview_Short() {
    ProtonTheme {
        ToolbarDisclaimer(
            text = TextUiModel.Text("Short"),
            modifier = Modifier
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ToolbarDisclaimerPreview_Tall() {
    ProtonTheme {
        ToolbarDisclaimer(
            text = TextUiModel.Text("Longer disclaimer value to make it go into several lines and wrap"),
            modifier = Modifier
        )
    }
}
