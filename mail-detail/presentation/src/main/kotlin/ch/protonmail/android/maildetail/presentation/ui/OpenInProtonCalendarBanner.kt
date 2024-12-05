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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.elevation
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultSmallStrongNorm

@Composable
fun OpenInProtonCalendarBanner(modifier: Modifier = Modifier, onOpenInProtonCalendarClick: () -> Unit) {
    Button(
        elevation = elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        shape = ProtonTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(backgroundColor = ProtonTheme.colors.backgroundSecondary),
        contentPadding = PaddingValues(ProtonDimens.ExtraSmallSpacing + ProtonDimens.SmallSpacing),
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.separatorNorm),
        modifier = modifier
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
            )
            .fillMaxWidth(),

        onClick = onOpenInProtonCalendarClick
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(MailDimens.ProtonCalendarIconSize),
                painter = painterResource(id = R.drawable.ic_logo_calendar),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(
                    style = ProtonTheme.typography.defaultSmallStrongNorm,
                    color = ProtonTheme.colors.textAccent,
                    maxLines = 1,
                    text = stringResource(id = R.string.open_on_protoncalendar_banner_title)
                )
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing / 2))
                Text(
                    style = ProtonTheme.typography.captionWeak,
                    color = ProtonTheme.colors.textWeak,
                    maxLines = 1,
                    text = stringResource(id = R.string.open_on_protoncalendar_banner_description)
                )
            }
        }
    }
}

@Preview
@Composable
private fun OpenInProtonCalendarBannerPreview() {
    OpenInProtonCalendarBanner(onOpenInProtonCalendarClick = {})
}
