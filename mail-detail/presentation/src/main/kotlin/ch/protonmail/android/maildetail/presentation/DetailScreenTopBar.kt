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

package ch.protonmail.android.maildetail.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.overline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String,
    isStarred: Boolean,
    onBackClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    ProtonTheme3 {
        LargeTopAppBar(
            modifier = modifier,
            title = {
                val maxLines = if (scrollBehavior.state.collapsedFraction > 0) 1 else Int.MAX_VALUE
                Column {
                    Text(
                        text = "1 message",
                        fontSize = ProtonTheme.typography.overline.fontSize
                    )
                    Text(
                        maxLines = maxLines,
                        text = title,
                        overflow = TextOverflow.Ellipsis
                    )

                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.presentation_back))
                }
            },
            actions = {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                        painter = getStarredIcon(isStarred),
                        contentDescription = NO_CONTENT_DESCRIPTION,
                        tint = getStarredIconColor(isStarred)
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = ProtonTheme.colors.backgroundNorm,
                scrolledContainerColor = ProtonTheme.colors.backgroundSecondary,
                navigationIconContentColor = ProtonTheme.colors.iconNorm,
                titleContentColor = ProtonTheme.colors.textNorm
            )
        )
    }

}

@Composable
private fun getStarredIconColor(isStarred: Boolean) = if (isStarred) {
    colorResource(id = R.color.sunglow)
} else {
    ProtonTheme.colors.textNorm
}

@Composable
private fun getStarredIcon(isStarred: Boolean) = painterResource(
    id = if (isStarred) {
        R.drawable.ic_proton_star_filled
    } else {
        R.drawable.ic_proton_star
    }
)
