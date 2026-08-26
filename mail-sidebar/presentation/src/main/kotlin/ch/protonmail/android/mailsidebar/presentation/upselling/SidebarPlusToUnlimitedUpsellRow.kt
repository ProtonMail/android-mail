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

package ch.protonmail.android.mailsidebar.presentation.upselling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.viewmodel.hiltViewModelOrNull
import ch.protonmail.android.mailsidebar.presentation.R
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarItem
import ch.protonmail.android.mailupselling.presentation.viewmodel.PlusToUnlimitedUpsellViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SidebarPlusToUnlimitedUpsellRow(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val viewModel = hiltViewModelOrNull<PlusToUnlimitedUpsellViewModel>()
    val theme = viewModel?.theme?.collectAsStateWithLifecycle()?.value

    // Set min to 1.dp to allow lazyColumns to render it.
    Box(modifier = modifier.heightIn(min = 1.dp)) {
        AnimatedVisibility(visible = theme != null, enter = scaleIn(), exit = scaleOut()) {
            theme?.let {
                ProtonSidebarItem(
                    icon = painterResource(R.drawable.ic_infinity),
                    iconTint = Color.Unspecified,
                    text = stringResource(it.rowTitleRes),
                    onClick = onClick
                )
            }
        }
    }
}
