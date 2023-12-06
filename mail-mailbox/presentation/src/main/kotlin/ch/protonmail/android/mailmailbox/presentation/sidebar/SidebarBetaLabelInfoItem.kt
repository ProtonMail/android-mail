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

package ch.protonmail.android.mailmailbox.presentation.sidebar

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.sidebar.SidebarBetaLabelInfoItem.ProtonMailBetaKbUrl
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionStrongUnspecified
import me.proton.core.compose.theme.captionWeak

@Composable
fun SidebarBetaLabelInfoItem(onClick: (Uri) -> Unit) {
    ProtonSidebarItem(
        modifier = Modifier
            .testTag(SidebarBetaLabelInfoItemTestTags.RootItem)
            .padding(ProtonDimens.SmallSpacing)
            .padding(ProtonDimens.ExtraSmallSpacing)
            .height(MailDimens.SidebarBetaLabelHeight)
            .wrapContentSize()
            .fillMaxSize()
            .background(ProtonTheme.colors.interactionWeakNorm, shape = ProtonTheme.shapes.medium),
        onClick = { onClick(ProtonMailBetaKbUrl) }
    ) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.sidebar_beta_version_title),
                    style = ProtonTheme.typography.captionStrongUnspecified
                )
                Text(
                    text = stringResource(id = R.string.sidebar_beta_version_description),
                    style = ProtonTheme.typography.captionWeak
                )
            }
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = R.drawable.ic_proton_arrow_out_square),
                tint = ProtonTheme.colors.iconWeak,
                contentDescription = stringResource(id = R.string.sidebar_beta_version_icon_content_description)
            )
        }
    }
}

private object SidebarBetaLabelInfoItem {

    val ProtonMailBetaKbUrl: Uri = Uri.parse("https://proton.me/support/mail-android-beta")
}

object SidebarBetaLabelInfoItemTestTags {

    const val RootItem = "SidebarBetaLabelInfoRootItem"
}
