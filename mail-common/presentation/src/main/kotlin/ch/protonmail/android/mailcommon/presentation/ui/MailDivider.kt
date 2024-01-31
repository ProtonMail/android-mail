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

package ch.protonmail.android.mailcommon.presentation.ui

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun MailDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.testTag(MailDividerTestTags.HeaderDivider),
        thickness = MailDimens.SeparatorHeight,
        color = ProtonTheme.colors.separatorNorm
    )
}

object MailDividerTestTags {

    const val HeaderDivider = "HeaderDivider"
}
