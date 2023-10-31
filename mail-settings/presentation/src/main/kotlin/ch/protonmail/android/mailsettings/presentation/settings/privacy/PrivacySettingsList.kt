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

package ch.protonmail.android.mailsettings.presentation.settings.privacy

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsToggleItem

@Composable
fun PrivacySettingsList(
    modifier: Modifier,
    state: PrivacySettingsState.WithData,
    actions: PrivacySettingsScreen.Actions
) {
    ProtonSettingsList(
        modifier.testTag(PrivacySettingsTestTags.RootItem)
    ) {
        item {
            ProtonSettingsToggleItem(
                name = stringResource(id = R.string.mail_settings_privacy_auto_show_remote_content),
                value = state.settings.autoShowRemoteContent,
                onToggle = actions.onShowRemoteContent
            )
        }
        item { Divider() }
        item {
            ProtonSettingsToggleItem(
                name = stringResource(id = R.string.mail_settings_privacy_auto_show_embedded_images),
                value = state.settings.autoShowEmbeddedImages,
                onToggle = actions.onShowEmbeddedImages
            )
        }
        item { Divider() }
        item {
            ProtonSettingsToggleItem(
                name = stringResource(id = R.string.mail_settings_privacy_request_link_confirmation),
                value = state.settings.requestLinkConfirmation,
                onToggle = actions.onRequestLinkConfirmation
            )
        }
        item { Divider() }
    }
}
