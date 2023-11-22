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

package ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingState.NotLoggedIn
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.util.kotlin.exhaustive
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

const val TEST_TAG_CONV_MODE_SETTINGS_SCREEN = "AccountConvModeTestTag"

@Composable
fun ConversationModeSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ConversationModeSettingViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = Loading
        ).value
    ) {
        is Data -> {
            ConversationModeSettingScreen(
                modifier = modifier,
                onBackClick = onBackClick,
                onConversationModeToggled = viewModel::onConversationToggled,
                state = state
            )
        }
        Loading -> ProtonCenteredProgress()
        NotLoggedIn -> ProtonErrorMessage(errorMessage = stringResource(id = commonString.x_error_not_logged_in))
    }.exhaustive
}

@Composable
fun ConversationModeSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onConversationModeToggled: (Boolean) -> Unit,
    state: Data
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_CONV_MODE_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = string.mail_settings_conversation_mode),
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            SettingsToggleItem(
                modifier = Modifier.padding(paddingValues),
                name = stringResource(id = string.mail_settings_conversation_mode),
                hint = stringResource(id = string.mail_settings_conversation_mode_hint),
                value = state.isEnabled ?: false,
                onToggle = onConversationModeToggled
            )
        }
    )
}

@Preview(name = "Conversation mode settings screen")
@Composable
fun ConversationModeSettingsScreenPreview() {
    ConversationModeSettingScreen(
        onBackClick = {}
    )
}
