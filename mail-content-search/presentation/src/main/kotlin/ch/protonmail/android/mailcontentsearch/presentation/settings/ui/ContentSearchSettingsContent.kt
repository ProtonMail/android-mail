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
import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState
import ch.protonmail.android.mailcontentsearch.presentation.settings.model.IndexSizeUiModel

@Composable
internal fun ContentSearchSettingsContent(
    state: ContentSearchSettingsState.Data,
    actions: ContentSearchSettingsScreen.Actions,
    modifier: Modifier = Modifier
) {
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }

    // The indexing section stays composed while it collapses, and by then the progress observer has
    // already blanked the percentage for the disabled account. Drawing the last state it was shown
    // with keeps it from flashing 0% on the way out.
    var lastEnabledState by remember { mutableStateOf(state) }
    if (state.isContentSearchEnabled) lastEnabledState = state

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ProtonDimens.Spacing.Large)
            .verticalScroll(rememberScrollState())
    ) {
        ContentSearchCard(
            isEnabled = state.isContentSearchEnabled,
            actions = ContentSearchCardActions(
                onToggle = actions.onContentSearchToggle,
                onLearnMoreClick = actions.onLearnMoreClick
            )
        )

        AnimatedVisibility(visible = state.isContentSearchEnabled) {
            Column {
                SectionHeader(title = R.string.mail_settings_content_search_indexing_section)

                IndexingCard(
                    state = lastEnabledState,
                    actions = IndexingCardActions(
                        onPause = actions.onPauseIndexing,
                        onResume = actions.onResumeIndexing,
                        onRetry = actions.onRetryIndexing
                    )
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

                MobileDataCard(
                    showLeadingIcon = false,
                    isEnabled = lastEnabledState.isAllowMobileDataEnabled,
                    onToggle = actions.onAllowMobileDataToggle
                )
            }
        }

        SectionHeader(title = R.string.mail_settings_content_search_on_this_device_section)

        OnThisDeviceCard(
            indexSize = state.indexSize,
            onClearDataClick = { showClearDataDialog = true }
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

        Text(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Medium),
            text = stringResource(id = R.string.mail_settings_content_search_clear_data_description),
            style = ProtonTheme.typography.bodyMediumWeak,
            color = ProtonTheme.colors.textWeak
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo))
    }

    if (showClearDataDialog) {
        ClearSearchDataConfirmationDialog(
            onCancelClicked = { showClearDataDialog = false },
            onConfirmClicked = {
                showClearDataDialog = false
                actions.onClearLocalSearchData()
            }
        )
    }
}

@Composable
private fun SectionHeader(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .semantics { heading() }
            .padding(
                top = ProtonDimens.Spacing.ExtraLarge,
                bottom = ProtonDimens.Spacing.Large
            ),
        text = stringResource(id = title),
        style = ProtonTheme.typography.titleMedium
    )
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
                syncPercentage = 30.0,
                isIndexingActive = true,
                indexSize = IndexSizeUiModel.Loaded(bytes = 1_288_490_188L)
            ),
            actions = PreviewActions
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Up to date")
@Composable
private fun ContentSearchContentUpToDatePreview() {
    ProtonInvertedTheme {
        ContentSearchSettingsContent(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = true,
                isAllowMobileDataEnabled = true,
                syncPercentage = null,
                isIndexingCompleted = true,
                indexSize = IndexSizeUiModel.Loaded(bytes = 1_288_490_188L)
            ),
            actions = PreviewActions
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Failed")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Failed dark")
@Composable
private fun ContentSearchContentFailedPreview() {
    ProtonInvertedTheme {
        ContentSearchSettingsContent(
            state = ContentSearchSettingsState.Data(
                isContentSearchEnabled = true,
                isAllowMobileDataEnabled = true,
                syncPercentage = 30.0,
                isIndexingFailed = true
            ),
            actions = PreviewActions
        )
    }
}

private val PreviewActions = ContentSearchSettingsScreen.Actions(
    onContentSearchToggle = {},
    onAllowMobileDataToggle = {},
    onClearLocalSearchData = {},
    onRetryIndexing = {},
    onPauseIndexing = {},
    onResumeIndexing = {},
    onLearnMoreClick = {}
)
