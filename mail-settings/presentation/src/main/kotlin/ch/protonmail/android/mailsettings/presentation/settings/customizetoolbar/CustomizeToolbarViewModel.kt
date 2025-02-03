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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.SettingsToolbarType
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ObserveToolbarActionsSettings
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ReorderSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.ResetSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.SaveSettingsActions
import ch.protonmail.android.mailsettings.domain.usecase.toolbaractions.SelectSettingsActions
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.mapper.ToolbarActionMapper
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.model.CustomizeToolbarOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class CustomizeToolbarViewModel @Inject constructor(
    private val resetToDefault: ResetSettingsActions,
    private val select: SelectSettingsActions,
    private val reorder: ReorderSettingsActions,
    private val save: SaveSettingsActions,
    observe: ObserveToolbarActionsSettings,
    private val mapper: ToolbarActionMapper
) : ViewModel() {

    private val tabSelection = MutableStateFlow(SettingsToolbarType.Message)
    private val tab get() = tabSelection.value

    private val preferences = observe()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), replay = 1)

    val state: StateFlow<CustomizeToolbarState> =
        preferences.combine(tabSelection) { prefsResponse, tab ->
            when (prefsResponse) {
                is Either.Left -> CustomizeToolbarState.NotLoggedIn
                is Either.Right -> {
                    val prefs = prefsResponse.value
                    CustomizeToolbarState.Data(
                        selectedTabIdx = tab.ordinal,
                        pages = listOf(
                            mapper.mapToUI(
                                isMailbox = false,
                                prefs.isConversationMode, prefs.messageOrConvToolbar.current
                            ),
                            mapper.mapToUI(
                                isMailbox = true,
                                prefs.isConversationMode, prefs.listToolbar.current
                            )
                        ),
                        tabs = listOf(
                            TextUiModel.TextRes(
                                if (prefs.isConversationMode) {
                                    R.string.customize_toolbar_conversation
                                } else {
                                    R.string.customize_toolbar_message
                                }
                            ),
                            TextUiModel.TextRes(R.string.customize_toolbar_mailbox)
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = CustomizeToolbarState.Loading
        )

    internal fun submit(action: CustomizeToolbarOperation) {
        when (action) {
            is CustomizeToolbarOperation.ActionRemoved -> select(action.actionId, selected = false, tab)
            is CustomizeToolbarOperation.ActionSelected -> select(action.actionId, selected = true, tab)
            CustomizeToolbarOperation.ResetToDefaultConfirmed -> resetToDefault(tab)
            is CustomizeToolbarOperation.TabSelected -> selectTab(action.tabIdx)
            is CustomizeToolbarOperation.ActionMoved -> reorder(
                fromIndex = action.fromIndex,
                toIndex = action.toIndex, tab
            )
            CustomizeToolbarOperation.SaveClicked -> savePreferences()
        }
    }

    private fun savePreferences() = viewModelScope.launch {
        save(preferences.firstOrNull()?.getOrNull() ?: return@launch)
    }

    private fun selectTab(tabIdx: Int) {
        tabSelection.update { SettingsToolbarType.entries[tabIdx] }
    }
}
