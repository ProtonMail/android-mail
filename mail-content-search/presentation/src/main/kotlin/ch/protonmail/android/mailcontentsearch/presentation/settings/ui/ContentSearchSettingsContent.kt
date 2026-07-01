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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState

@Composable
internal fun ContentSearchSettingsContent(
    modifier: Modifier = Modifier,
    state: ContentSearchSettingsState.Data,
    actions: ContentSearchSettingsScreen.Actions
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ProtonDimens.Spacing.Large)
            .verticalScroll(rememberScrollState())
    ) {
        ContentSearchCard(
            isEnabled = state.isContentSearchEnabled,
            syncPercentage = state.syncPercentage,
            isIndexingActive = state.isIndexingActive,
            onToggle = actions.onContentSearchToggle,
            onLearnMoreClick = actions.onLearnMoreClick
        )

        AnimatedVisibility(visible = state.isContentSearchEnabled) {
            Column {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
                MobileDataCard(
                    isEnabled = state.isAllowMobileDataEnabled,
                    onToggle = actions.onAllowMobileDataToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

        ClearSearchDataCard(onClick = actions.onClearLocalSearchData)

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Medium),
            text = stringResource(id = R.string.mail_settings_content_search_clear_data_description),
            style = ProtonTheme.typography.bodyMediumWeak,
            color = ProtonTheme.colors.textWeak
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo))
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Disabled")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Disabled dark")
@Composable
private fun ContentSearchSettingsContentOffPreview() {
    ProtonInvertedTheme {
        ContentSearchSettingsContent(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = false,
                isAllowMobileDataEnabled = true,
                syncPercentage = null
            ),
            actions = PreviewActions
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Enabled")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Enabled dark")
@Composable
private fun ContentSearchSettingsContentOnPreview() {
    ProtonInvertedTheme {
        ContentSearchSettingsContent(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = true,
                isAllowMobileDataEnabled = true,
                syncPercentage = 3.45
            ),
            actions = PreviewActions
        )
    }
}

private val PreviewActions = ContentSearchSettingsScreen.Actions(
    onContentSearchToggle = {},
    onAllowMobileDataToggle = {},
    onClearLocalSearchData = {},
    onLearnMoreClick = {}
)
