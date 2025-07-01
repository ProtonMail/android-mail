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

package ch.protonmail.android.maillabel.presentation.sidebar

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.compose.dropUnlessResumedDebounced
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun SidebarUpsellItem(
    show: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = show) {
        Column {
            SidebarItemWithCounter(
                modifier = modifier,
                icon = painterResource(id = R.drawable.ic_upselling_mail_plus),
                text = stringResource(R.string.upselling_mailbox_plus_title),
                onClick = dropUnlessResumedDebounced(onClick)
            )
            HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
        }
    }
}

@SuppressLint("VisibleForTests")
@Preview(
    name = "Upsell item in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Upsell item in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewSidebarUpsellItem() {
    ProtonTheme(
        colors = ProtonTheme.colors.sidebarColors ?: ProtonTheme.colors
    ) {
        SidebarUpsellItem(
            show = true,
            onClick = {}
        )
    }
}
