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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.design.compose.component.ProtonSettingsHeader
import ch.protonmail.android.design.compose.component.ProtonSettingsItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbugreport.presentation.R
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews

@Composable
internal fun ApplicationLogsScreenList(
    modifier: Modifier = Modifier,
    appVersion: String,
    isStandalone: Boolean,
    actions: ApplicationLogsScreenList.Actions
) {

    LazyColumn(modifier = modifier) {
        if (isStandalone) viewLogsSection(actions)

        item {
            ProtonSettingsHeader(
                title = stringResource(R.string.application_events_header_export),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = ProtonDimens.Spacing.Small)
            )
        }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_share),
                onClick = actions.onShare
            )
        }
        item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
        item {
            ProtonSettingsItem(
                name = stringResource(R.string.application_events_save_to_disk),
                onClick = actions.onExport
            )
        }

        if (isStandalone) featureFlagsSection(actions)

        item { AppVersion(appVersion) }
    }
}

private fun LazyListScope.viewLogsSection(actions: ApplicationLogsScreenList.Actions) {
    item {
        ProtonSettingsHeader(title = R.string.application_events_header_view)
    }
    item {
        ProtonSettingsItem(
            name = stringResource(R.string.application_events_view_logcat),
            hint = stringResource(R.string.application_events_view_logcat_hint),
            onClick = actions.onShowLogcat
        )
    }
    item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
    item {
        ProtonSettingsItem(
            name = stringResource(R.string.application_events_view_rust_events),
            hint = stringResource(R.string.application_events_view_rust_events_hint),
            onClick = actions.onShowRustEvents
        )
    }
    item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
    item {
        ProtonSettingsItem(
            name = stringResource(R.string.application_events_view_events),
            hint = stringResource(R.string.application_events_view_events_hint),
            onClick = actions.onShowAppEvents
        )
    }
}

private fun LazyListScope.featureFlagsSection(actions: ApplicationLogsScreenList.Actions) {
    item { HorizontalDivider(color = ProtonTheme.colors.separatorNorm) }
    item {
        ProtonSettingsHeader(
            title = stringResource(R.string.application_events_feature_flags_title),
            modifier = Modifier.padding(bottom = ProtonDimens.Spacing.Small)
        )
    }
    item {
        ProtonSettingsItem(
            name = stringResource(R.string.application_events_feature_flags_subtitle),
            onClick = actions.onFeatureFlagNavigation
        )
    }
}

@Composable
private fun AppVersion(appVersion: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.Standard)
            .padding(top = ProtonDimens.Spacing.Standard),
        text = appVersion,
        textAlign = TextAlign.Center,
        color = ProtonTheme.colors.textWeak,
        style = ProtonTheme.typography.bodyMedium,
        maxLines = 1
    )
}

object ApplicationLogsScreenList {
    data class Actions(
        val onExport: () -> Unit,
        val onShare: () -> Unit,
        val onShowAppEvents: () -> Unit,
        val onShowRustEvents: () -> Unit,
        val onShowLogcat: () -> Unit,
        val onFeatureFlagNavigation: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExport = {},
                onShare = {},
                onShowAppEvents = {},
                onShowRustEvents = {},
                onShowLogcat = {},
                onFeatureFlagNavigation = {}
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun ApplicationLogsScreenPreview() {
    ProtonTheme {
        ApplicationLogsScreenList(
            appVersion = "Proton Mail (0) - 0.7.0",
            actions = ApplicationLogsScreenList.Actions.Empty,
            isStandalone = false
        )
    }
}
