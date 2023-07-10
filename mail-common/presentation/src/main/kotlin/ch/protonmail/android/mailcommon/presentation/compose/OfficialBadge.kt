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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overlineStrongNorm

@Composable
fun OfficialBadge(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .testTag(OfficialBadgeTestTags.Item)
            .padding(start = ProtonDimens.ExtraSmallSpacing)
            .background(color = ProtonTheme.colors.backgroundSecondary, shape = ProtonTheme.shapes.medium)
            .padding(horizontal = ProtonDimens.ExtraSmallSpacing, vertical = MailDimens.TinySpacing),
        text = stringResource(id = R.string.auth_badge_official),
        maxLines = 1,
        style = ProtonTheme.typography.overlineStrongNorm.copy(color = ProtonTheme.colors.textAccent)
    )
}

object OfficialBadgeTestTags {

    const val Item = "OfficialBadge"
}
