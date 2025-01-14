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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ToolbarActionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.previewdata.SelectedToolbarActionPreviewProvider
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.previewdata.ToolbarActionPreview
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun SelectedToolbarActionDisplay(
    model: ToolbarActionUiModel,
    reorderButton: @Composable () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.SmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRemoveClicked, enabled = model.enabled) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_minus_circle_filled),
                    tint = ProtonTheme.colors.notificationError.modifyAlpha(model.enabled),
                    contentDescription = stringResource(R.string.action_delete_description)
                )
            }
            Icon(
                modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                painter = painterResource(id = model.icon),
                contentDescription = model.description.string(),
                tint = ProtonTheme.colors.iconNorm.modifyAlpha(model.enabled)
            )
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            Text(
                text = model.description.string(),
                color = ProtonTheme.colors.textNorm.modifyAlpha(model.enabled),
                style = ProtonTheme.typography.body1Regular
            )
            Spacer(modifier = Modifier.weight(1f))
            reorderButton()
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            color = ProtonTheme.colors.separatorNorm
        )
    }
}

private fun Color.modifyAlpha(enabled: Boolean) = copy(alpha = if (enabled) 1.0f else 0.5f)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CustomizeToolbarContentPreview(
    @PreviewParameter(SelectedToolbarActionPreviewProvider::class) preview: ToolbarActionPreview
) {
    val model = preview.uiModel
    SelectedToolbarActionDisplay(
        model,
        reorderButton = {
            ActionDragHandle()
        },
        onRemoveClicked = {}
    )
}
