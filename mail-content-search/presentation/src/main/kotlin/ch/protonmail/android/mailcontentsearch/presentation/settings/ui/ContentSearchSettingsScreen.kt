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

package ch.protonmail.android.mailcontentsearch.presentation.settings.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonSettingsDetailsAppBar
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsViewAction
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsViewModel

@Composable
fun ContentSearchSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onLearnMoreClick: () -> Unit = {}
) {
    val viewModel: ContentSearchSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = ContentSearchSettingsScreen.Actions(
        onContentSearchToggle = { enabled ->
            viewModel.submit(ContentSearchSettingsViewAction.ToggleContentSearch(enabled))
        },
        onAllowMobileDataToggle = { enabled ->
            viewModel.submit(ContentSearchSettingsViewAction.ToggleAllowMobileData(enabled))
        },
        onClearLocalSearchData = { viewModel.submit(ContentSearchSettingsViewAction.ClearLocalData) },
        onLearnMoreClick = onLearnMoreClick
    )

    ContentSearchSettingsScreen(
        modifier = modifier,
        state = state,
        actions = actions,
        onBackClick = onBackClick
    )
}

@Composable
@Suppress("UseComposableActions")
private fun ContentSearchSettingsScreen(
    modifier: Modifier = Modifier,
    state: ContentSearchSettingsState,
    actions: ContentSearchSettingsScreen.Actions,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsDetailsAppBar(
                title = "",
                onBackClick = onBackClick
            )
        },
        containerColor = ProtonTheme.colors.backgroundInvertedNorm,
        content = { paddingValues ->
            when (state) {
                is ContentSearchSettingsState.Loading -> ProtonCenteredProgress()
                is ContentSearchSettingsState.LoadingError -> Unit
                is ContentSearchSettingsState.Data -> ContentSearchSettingsContent(
                    modifier = Modifier.padding(paddingValues),
                    state = state,
                    actions = actions
                )
            }
        }
    )
}

object ContentSearchSettingsScreen {

    data class Actions(
        val onContentSearchToggle: (Boolean) -> Unit,
        val onAllowMobileDataToggle: (Boolean) -> Unit,
        val onClearLocalSearchData: () -> Unit,
        val onLearnMoreClick: () -> Unit
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ContentSearchSettingsScreenPreview() {
    ProtonTheme {
        ContentSearchSettingsScreen(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = false,
                isAllowMobileDataEnabled = true,
                syncPercentage = null
            ),
            actions = ContentSearchSettingsScreen.Actions(
                onContentSearchToggle = {},
                onAllowMobileDataToggle = {},
                onClearLocalSearchData = {},
                onLearnMoreClick = {}
            ),
            onBackClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Enabled")
@Composable
private fun ContentSearchSettingsScreenOnPreview() {
    ProtonTheme {
        ContentSearchSettingsScreen(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = true,
                isAllowMobileDataEnabled = true,
                syncPercentage = 3.45
            ),
            actions = ContentSearchSettingsScreen.Actions(
                onContentSearchToggle = {},
                onAllowMobileDataToggle = {},
                onClearLocalSearchData = {},
                onLearnMoreClick = {}
            ),
            onBackClick = {}
        )
    }
}
