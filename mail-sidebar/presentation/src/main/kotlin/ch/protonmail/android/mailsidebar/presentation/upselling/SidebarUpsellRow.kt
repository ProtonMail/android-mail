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

package ch.protonmail.android.mailsidebar.presentation.upselling

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailsidebar.presentation.R
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarItem
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.getSidebarIcon
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel
import ch.protonmail.android.mailupselling.presentation.R as upsellingR

@Composable
fun SidebarUpsellRow(modifier: Modifier = Modifier, onClick: (type: UpsellingVisibility) -> Unit) {

    val viewModel = hiltViewModel<UpsellingButtonViewModel, UpsellingButtonViewModel.Factory> { factory ->
        factory.create(UpsellingEntryPoint.Feature.Sidebar)
    }

    val state = viewModel.state.collectAsStateWithLifecycle()
    val type = state.value.visibility

    // Set min to 1.dp to allow lazyColumns to render it.
    Box(modifier = modifier.heightIn(min = 1.dp)) {
        AnimatedVisibility(
            visible = state.value.isShown,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            when (val visibility = state.value.visibility) {
                is UpsellingVisibility.Hidden -> Unit
                is UpsellingVisibility.Promotional.BlackFriday ->
                    SidebarUpsellRowBlackFriday(visibility, onButtonClick = { onClick(type) })

                is UpsellingVisibility.Promotional.SpringPromo ->
                    SidebarUpsellRowSpringPromo(onButtonClick = { onClick(type) })

                is UpsellingVisibility.Promotional.IntroductoryPrice,
                is UpsellingVisibility.Normal -> SidebarUpsellRow(onButtonClick = { onClick(type) })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SidebarUpsellRow(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    ProtonSidebarItem(
        modifier = modifier,
        icon = painterResource(R.drawable.ic_diamond),
        iconTint = Color.Unspecified,
        text = stringResource(R.string.drawer_upgrade_plus),
        onClick = onButtonClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SidebarUpsellRowBlackFriday(
    visibility: UpsellingVisibility.Promotional.BlackFriday,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconModifier = Modifier
        .widthIn(min = ProtonDimens.IconSize.Default)
        .clip(CircleShape.copy(all = CornerSize(ProtonDimens.Spacing.Compact)))
        .background(UpsellingLayoutValues.BlackFriday.mainColor)
        .padding(vertical = ProtonDimens.Spacing.Small)

    ProtonSidebarItem(
        modifier = modifier,
        icon = visibility.getSidebarIcon(),
        iconTint = Color.Unspecified,
        iconModifier = iconModifier,
        text = stringResource(R.string.drawer_upgrade_plus_black_friday),
        textColor = UpsellingLayoutValues.BlackFriday.mainColor,
        onClick = onButtonClick
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SidebarUpsellRowSpringPromo(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconModifier = Modifier
        .width(ProtonDimens.IconSize.Default)
        .padding(vertical = ProtonDimens.Spacing.Small)

    ProtonSidebarItem(
        modifier = modifier,
        icon = painterResource(upsellingR.drawable.ic_upselling_spring),
        iconTint = Color.Unspecified,
        iconModifier = iconModifier,
        text = stringResource(R.string.drawer_upgrade_plus_spring_sale),
        textColor = UpsellingLayoutValues.SpringPromo.mainColor,
        onClick = onButtonClick
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun SidebarUpsellRowPreview() {
    ProtonTheme {
        SidebarUpsellRow(onButtonClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun SidebarUpsellRowBlackFridayPreview() {
    ProtonTheme {
        SidebarUpsellRowBlackFriday(UpsellingVisibility.Promotional.BlackFriday.Wave1, onButtonClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun SidebarUpsellRowSpringPromoPreview() {
    ProtonTheme {
        SidebarUpsellRowSpringPromo(onButtonClick = {})
    }
}
