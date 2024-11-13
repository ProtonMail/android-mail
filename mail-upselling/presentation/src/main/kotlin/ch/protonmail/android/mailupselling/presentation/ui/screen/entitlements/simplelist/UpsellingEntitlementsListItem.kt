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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.PlanEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import coil.compose.AsyncImage
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm

@Composable
internal fun UpsellingEntitlementsListItem(
    modifier: Modifier = Modifier,
    entitlementUiModel: PlanEntitlementListUiModel
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(vertical = ProtonDimens.ExtraSmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val imageModel = when (entitlementUiModel) {
            is PlanEntitlementListUiModel.Default -> entitlementUiModel.remoteResource
            is PlanEntitlementListUiModel.Overridden -> entitlementUiModel.localResource
        }

        AsyncImage(
            modifier = Modifier.size(UpsellingLayoutValues.EntitlementsList.imageSize),
            placeholder = painterResource(R.drawable.ic_logo_mail_mono),
            model = imageModel,
            contentDescription = NO_CONTENT_DESCRIPTION,
            colorFilter = ColorFilter.tint(UpsellingLayoutValues.EntitlementsList.iconColor),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.size(ProtonDimens.SmallSpacing))
        Text(
            text = entitlementUiModel.text.string(),
            style = ProtonTheme.typography.defaultSmallNorm,
            color = UpsellingLayoutValues.EntitlementsList.textColor
        )
    }
}
