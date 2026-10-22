/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ToolbarActionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui.preview.SelectedToolbarActionPreviewProvider
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui.preview.ToolbarActionPreview

@Composable
internal fun SelectedToolbarActionDisplay(
    model: ToolbarActionUiModel,
    reorderButton: @Composable () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            reorderButton()
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Standard))
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.Default),
                painter = painterResource(id = model.action.icon),
                contentDescription = model.action.description.string(),
                tint = ProtonTheme.colors.iconNorm.modifyAlpha(model.enabled)
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Medium))
            Text(
                text = model.action.description.string(),
                color = ProtonTheme.colors.textNorm.modifyAlpha(model.enabled),
                style = ProtonTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemoveClicked, enabled = model.enabled) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_minus_circle),
                    tint = ProtonTheme.colors.notificationError.modifyAlpha(model.enabled),
                    contentDescription = stringResource(R.string.action_delete_description)
                )
            }
        }
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
