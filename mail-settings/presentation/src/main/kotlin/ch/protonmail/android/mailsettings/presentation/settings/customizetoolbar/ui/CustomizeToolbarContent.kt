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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.CustomizeToolbarState
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.model.CustomizeToolbarOperation
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.previewdata.CustomizeToolbarPreview
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.previewdata.CustomizeToolbarPreviewProvider
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
internal fun CustomizeToolbarContent(
    state: CustomizeToolbarState,
    modifier: Modifier = Modifier,
    onAction: (CustomizeToolbarOperation) -> Unit,
    onBackClick: () -> Unit
) {
    when (state) {
        is CustomizeToolbarState.Data -> {
            val pagerState = rememberPagerState(
                initialPage = state.selectedTabIdx,
                pageCount = { state.pages.size }
            )

            LaunchedEffect(state.selectedTabIdx) {
                pagerState.scrollToPage(state.selectedTabIdx)
            }
            LaunchedEffect(state) {
                snapshotFlow {
                    pagerState.settledPage
                }.collect {
                    onAction(CustomizeToolbarOperation.TabSelected(it))
                }
            }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    Column {
                        ProtonTopAppBar(
                            modifier = Modifier.fillMaxWidth(),
                            title = {
                                androidx.compose.material.Text(
                                    stringResource(id = R.string.mail_settings_customize_toolbar)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    androidx.compose.material.Icon(
                                        Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = stringResource(id = R.string.presentation_back)
                                    )
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    onAction(CustomizeToolbarOperation.SaveClicked)
                                    onBackClick()
                                }) {
                                    Text(
                                        stringResource(R.string.customize_toolbar_done_action),
                                        color = ProtonTheme.colors.textAccent,
                                        style = ProtonTheme.typography.defaultNorm
                                    )
                                }
                            }
                        )
                        ToolbarTypeTabs(
                            pagerState.currentPage,
                            tabs = state.tabs,
                            modifier = Modifier,
                            onSelected = {
                                onAction(CustomizeToolbarOperation.TabSelected(it))
                            }
                        )
                    }

                }, content = { paddingValues ->
                    HorizontalPager(
                        state = pagerState
                    ) { index ->
                        val page = state.pages[index]
                        Column(modifier = Modifier.fillMaxSize()) {
                            ToolbarActions(
                                items = page.selectedActions,
                                disclaimer = page.disclaimer,
                                onAction = onAction,
                                remainingItems = page.remainingActions,
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            )
        }
        CustomizeToolbarState.Loading -> Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                ProtonSettingsTopBar(
                    title = stringResource(id = R.string.mail_settings_customize_toolbar),
                    onBackClick = onBackClick
                )

            }, content = { paddingValues ->
                ProtonCenteredProgress(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                )
            }
        )
        CustomizeToolbarState.NotLoggedIn -> Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                ProtonSettingsTopBar(
                    title = stringResource(id = R.string.mail_settings_customize_toolbar),
                    onBackClick = onBackClick
                )
            }, content = { paddingValues ->
                ProtonErrorMessage(
                    errorMessage = stringResource(id = R.string.x_error_not_logged_in),
                    modifier = Modifier.padding(paddingValues)
                        .padding(ProtonDimens.DefaultSpacing)
                )
            }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CustomizeToolbarContentPreview(
    @PreviewParameter(CustomizeToolbarPreviewProvider::class) preview: CustomizeToolbarPreview
) {
    ProtonTheme {
        CustomizeToolbarContent(
            state = preview.uiModel,
            modifier = Modifier,
            onAction = {},
            onBackClick = {}
        )
    }
}
