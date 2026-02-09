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

package ch.protonmail.android.mailsettings.presentation.settings.appicon.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.AppIconUiModel
import coil.compose.AsyncImage

@Composable
internal fun SelectableAppIcon(
    preset: AppIconUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = ProtonDimens.BorderSize.Semilarge,
                            color = ProtonTheme.colors.brandNorm,
                            shape = RoundedCornerShape(ProtonDimens.CornerRadius.Jumbo)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(ProtonDimens.Spacing.Compact),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = preset.iconPreviewResId,
                contentDescription = NO_CONTENT_DESCRIPTION,
                modifier = Modifier
                    .size(ProtonDimens.IconSize.ExtraExtraLarge)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(
            modifier = Modifier.height(ProtonDimens.Spacing.Standard)
        )
        Text(
            text = preset.data.id.name,
            style = ProtonTheme.typography.bodyLargeNorm
        )
    }
}
