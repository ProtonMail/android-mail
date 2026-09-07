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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.design.compose.component.appbar.ProtonTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodySmallNorm
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewState
import ch.protonmail.android.mailbugreport.presentation.utils.ApplicationLogsUtils.shareLogs
import kotlinx.coroutines.launch

@Composable
internal fun ApplicationLogsPeekViewContent(
    state: ApplicationLogsPeekViewState.Loaded,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val contents = state.uiModel.fileContents
    val lineStyle = ProtonTheme.typography.bodySmallNorm.copy(fontFamily = FontFamily.Monospace)

    val contentWidth = remember(lineStyle, state.uiModel.longestLineChars) {
        val charWidth = textMeasurer.measure(text = "0", style = lineStyle).size.width
        with(density) { (charWidth * state.uiModel.longestLineChars).toDp() }
    }

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
                        style = ProtonTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis,
                        color = ProtonTheme.colors.textNorm
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
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
                            painter = painterResource(id = R.drawable.ic_proton_arrow_down),
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch { context.shareLogs(state.uiModel.fileUri) }
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
                        .padding(ProtonDimens.Spacing.Small)
                ) {
                    // Load in chunks as the file contents could be huge and take a while to render.
                    items(state.uiModel.fileContents) { chunk ->
                        Text(
                            // One log entry stays on one row. The row scrolls sideways instead of
                            // wrapping, so the timestamp and tag columns line up down the file.
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .width(contentWidth),
                            text = chunk,
                            style = lineStyle,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
