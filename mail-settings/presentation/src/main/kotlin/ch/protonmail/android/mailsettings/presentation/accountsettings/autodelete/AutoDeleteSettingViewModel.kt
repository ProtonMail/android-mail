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

package ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.presentation.model.DialogState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState.NotLoggedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

@HiltViewModel
class AutoDeleteSettingViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val mailSettingsRepository: MailSettingsRepository,
    observeMailSettings: ObserveMailSettings
) : ViewModel() {

    private val _state = MutableStateFlow<AutoDeleteSettingState>(Loading)
    val state: StateFlow<AutoDeleteSettingState> = _state

    private val actionMutex = Mutex()

    init {
        accountManager.getPrimaryUserId().flatMapLatest { userId ->
            if (userId == null) flowOf(null)
            else observeMailSettings(userId)
        }.onEach { mailSettings ->

            actionMutex.withLock {
                val currentState = _state.value

                _state.value = if (mailSettings == null) {
                    NotLoggedIn
                } else {
                    val isEnabled = mailSettings.autoDeleteSpamAndTrashDays?.let { it > 0 } ?: false

                    if (currentState is Data) {
                        currentState.copy(
                            isEnabled = isEnabled
                        )
                    } else Data(isEnabled = isEnabled)
                }
            }

        }.launchIn(viewModelScope)
    }

    fun submit(action: AutoDeleteViewAction) {
        viewModelScope.launch {
            emitNewStateFor(action)
        }

    }

    private suspend fun emitNewStateFor(operation: AutoDeleteToggleOperation) {
        actionMutex.withLock {
            val currentState = _state.value

            if (currentState is Data) {
                _state.value = when (operation) {
                    AutoDeleteViewAction.ActivationConfirmed -> {
                        updateSetting(true)

                        currentState.withDialogsHidden()
                    }

                    AutoDeleteViewAction.ActivationRequested -> currentState.copy(
                        enablingDialogState = DialogState.Shown(
                            title = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_title),
                            message = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_text),
                            dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
                            confirmButtonText = TextUiModel(
                                R.string.mail_settings_auto_delete_dialog_enabling_button_confirm
                            )
                        ),
                        disablingDialogState = DialogState.Hidden
                    )

                    AutoDeleteViewAction.DeactivationConfirmed -> {

                        updateSetting(false)

                        currentState.withDialogsHidden()
                    }

                    AutoDeleteViewAction.DeactivationRequested -> currentState.copy(
                        disablingDialogState = DialogState.Shown(
                            title = TextUiModel(R.string.mail_settings_auto_delete_dialog_disabling_title),
                            message = TextUiModel(R.string.mail_settings_auto_delete_dialog_disabling_text),
                            dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
                            confirmButtonText = TextUiModel(
                                R.string.mail_settings_auto_delete_dialog_disabling_button_confirm
                            )
                        ),
                        enablingDialogState = DialogState.Hidden
                    )

                    AutoDeleteViewAction.DialogDismissed ->
                        currentState.withDialogsHidden()
                }
            }
        }

    }

    private fun Data.withDialogsHidden() = this.copy(
        disablingDialogState = DialogState.Hidden,
        enablingDialogState = DialogState.Hidden
    )

    @Suppress("MagicNumber")
    private suspend fun updateSetting(isEnabled: Boolean) {
        accountManager
            .getPrimaryUserId()
            .filterNotNull()
            .mapLatest { userId ->
                mailSettingsRepository.updateAutoDeleteSpamAndTrashDays(
                    userId,
                    if (isEnabled) {
                        30
                    } else {
                        0
                    }
                )
            }
            .launchIn(viewModelScope)
    }

}