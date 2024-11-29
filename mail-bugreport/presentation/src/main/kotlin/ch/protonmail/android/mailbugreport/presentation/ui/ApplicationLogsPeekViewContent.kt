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

package ch.protonmail.android.mailbugreport.presentation.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewState
import ch.protonmail.android.mailbugreport.presentation.utils.ApplicationLogsUtils.shareLogs
import kotlinx.coroutines.launch
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun ApplicationLogsPeekViewContent(
    state: ApplicationLogsPeekViewState.Loaded,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    val contents = state.uiModel.fileContents

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonTopAppBar(
                modifier = modifier.fillMaxWidth(),
                title = {
                    Text(
                        modifier = Modifier.clickable {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        },
                        text = state.uiModel.fileName,
                        overflow = TextOverflow.Ellipsis,
                        color = ProtonTheme.colors.textNorm
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { coroutineScope.launch { lazyListState.animateScrollToItem(contents.size) } }
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch { context.shareLogs(state.uiModel.rawFile) }
                        }
                    ) {
                        Icon(Icons.Filled.Share, tint = ProtonTheme.colors.iconNorm, contentDescription = null)
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            SelectionContainer {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ProtonDimens.ExtraSmallSpacing)
                        .animateContentSize()
                ) {
                    // Load in chunks as the file contents could be huge and take a while to render.
                    items(state.uiModel.fileContents) { chunk ->
                        Text(text = chunk, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
