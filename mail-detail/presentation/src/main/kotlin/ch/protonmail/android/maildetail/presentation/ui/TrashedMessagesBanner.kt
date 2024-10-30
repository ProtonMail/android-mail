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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.TrashedMessagesBannerUiModel
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm

@Composable
fun TrashedMessagesBanner(
    modifier: Modifier = Modifier,
    uiModel: TrashedMessagesBannerUiModel,
    onActionClick: () -> Unit
) {
    Column(
        modifier = modifier
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.large
            )
            .background(
                color = ProtonTheme.colors.backgroundNorm,
                shape = ProtonTheme.shapes.large
            )

            .padding(
                horizontal = ProtonDimens.DefaultSpacing,
                vertical = ProtonDimens.SmallSpacing
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_trash),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
            Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = uiModel.message),
                style = ProtonTheme.typography.defaultSmallNorm
            )
            Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
            ProtonTextButton(onClick = onActionClick) {
                Text(text = stringResource(id = uiModel.action))
            }
        }
    }
}

@Preview
@Composable
private fun TrashedMessagesBannerPreview() {
    TrashedMessagesBanner(
        modifier = Modifier,
        uiModel = TrashedMessagesBannerUiModel(
            R.string.trashed_messages_banner,
            R.string.show
        ),
        onActionClick = {}
    )
}

