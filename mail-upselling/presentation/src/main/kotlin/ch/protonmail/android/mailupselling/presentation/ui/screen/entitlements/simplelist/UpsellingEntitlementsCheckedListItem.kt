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

package ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.simplelist

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3

@Composable
internal fun UpsellingEntitlementsCheckedListItem(model: TextUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(vertical = ProtonDimens.ExtraSmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_checkmark),
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .width(ProtonDimens.DefaultIconSize)
                .wrapContentHeight(),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
        Text(
            text = model.string(),
            style = ProtonTheme.typography.body1Regular,
            color = UpsellingLayoutValues.CheckMarkEntitlementColor
        )
    }
}

@Preview
@Composable
private fun CheckedListItemPreview() {
    ProtonTheme3 {
        UpsellingEntitlementsCheckedListItem(
            model = TextUiModel.Text("15 GB storage")
        )
    }
}
