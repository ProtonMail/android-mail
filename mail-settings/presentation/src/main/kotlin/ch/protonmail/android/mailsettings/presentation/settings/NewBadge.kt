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

package ch.protonmail.android.mailsettings.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.labelMediumNorm
import ch.protonmail.android.mailsettings.presentation.R

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .testTag(NewBadgeTestTags.Item)
            .background(color = ProtonTheme.colors.brandMinus30, shape = CircleShape)
            .padding(horizontal = ProtonDimens.Spacing.Compact, vertical = ProtonDimens.Spacing.Tiny),
        text = stringResource(id = R.string.mail_settings_new_badge),
        maxLines = 1,
        style = ProtonTheme.typography.labelMediumNorm.copy(color = ProtonTheme.colors.brandPlus10)
    )
}

object NewBadgeTestTags {

    const val Item = "SettingsNewBadge"
}
