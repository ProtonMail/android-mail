/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailupselling.presentation.R

@Composable
internal fun UpsellingInfinity(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier.size(ProtonDimens.IconSize.Default),
        painter = painterResource(id = R.drawable.ic_infinity),
        contentDescription = NO_CONTENT_DESCRIPTION
    )
}
