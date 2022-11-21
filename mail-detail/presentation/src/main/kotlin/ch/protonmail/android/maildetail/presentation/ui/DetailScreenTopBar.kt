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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.maildetail.presentation.R.color
import ch.protonmail.android.maildetail.presentation.R.drawable
import ch.protonmail.android.maildetail.presentation.R.plurals
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreview
import ch.protonmail.android.maildetail.presentation.previewdata.DetailsScreenTopBarPreviewProvider
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.overline

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String,
    isStarred: Boolean?,
    messageCount: Int?,
    actions: DetailScreenTopBar.Actions,
    scrollBehavior: TopAppBarScrollBehavior
) {
    ProtonTheme3 {
        LargeTopAppBar(
            modifier = modifier,
            title = {
                Column {
                    messageCount?.let { count ->
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = pluralStringResource(plurals.message_count_label_text, count, count),
                            fontSize = ProtonTheme.typography.overline.fontSize,
                            textAlign = TextAlign.Center
                        )
                    }
                    val isFullyExpanded = scrollBehavior.state.collapsedFraction == 0F
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (isFullyExpanded) 2 else 1,
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                }
            },
            navigationIcon = {
                IconButton(onClick = actions.onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = string.presentation_back))
                }
            },
            actions = {
                if (isStarred != null) {
                    val onStarIconClick = {
                        when (isStarred) {
                            true -> actions.onUnStarClick()
                            false -> actions.onStarClick()
                        }
                    }
                    IconButton(onClick = onStarIconClick) {
                        Icon(
                            modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                            painter = getStarredIcon(isStarred),
                            contentDescription = NO_CONTENT_DESCRIPTION,
                            tint = getStarredIconColor(isStarred)
                        )
                    }
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
    colorResource(id = color.sunglow)
} else {
    ProtonTheme.colors.textNorm
}

@Composable
private fun getStarredIcon(isStarred: Boolean) = painterResource(
    id = if (isStarred) {
        drawable.ic_proton_star_filled
    } else {
        drawable.ic_proton_star
    }
)

object DetailScreenTopBar {

    /**
     * Using an empty String for a Text inside LargeTopAppBar causes a crash.
     */
    const val NoTitle = " "

    data class Actions(
        val onBackClick: () -> Unit,
        val onStarClick: () -> Unit,
        val onUnStarClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onStarClick = {},
                onUnStarClick = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
@OptIn(ExperimentalMaterial3Api::class)
private fun DetailScreenTopBarPreview(
    @PreviewParameter(DetailsScreenTopBarPreviewProvider::class) preview: DetailsScreenTopBarPreview
) {
    ProtonTheme3 {
        val initialHeightOffset = if (preview.isExpanded) 0f else -Float.MAX_VALUE
        val state = rememberTopAppBarState(initialHeightOffset = initialHeightOffset)
        DetailScreenTopBar(
            title = preview.title,
            isStarred = preview.isStarred,
            messageCount = preview.messageCount,
            actions = DetailScreenTopBar.Actions.Empty,
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = state)
        )
    }
}
