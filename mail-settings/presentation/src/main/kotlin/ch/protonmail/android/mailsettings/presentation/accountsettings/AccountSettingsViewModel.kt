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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.annotations.AutodeleteFeatureEnabled
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveUserSettings
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.NotLoggedIn
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AccountSettingsViewAction
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.domain.feature.IsFido2Enabled
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.mailsettings.domain.entity.ViewMode.ConversationGrouping
import me.proton.core.usersettings.domain.entity.UserSettings
import me.proton.core.usersettings.domain.usecase.ObserveRegisteredSecurityKeys
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    accountManager: AccountManager,
    private val isFido2Enabled: IsFido2Enabled,
    private val observeUser: ObserveUser,
    private val observeUserSettings: ObserveUserSettings,
    private val observeMailSettings: ObserveMailSettings,
    private val observeRegisteredSecurityKeys: ObserveRegisteredSecurityKeys,
    private val observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val userUpgradeState: UserUpgradeState,
    private val isPaidMailUser: IsPaidMailUser,
    @AutodeleteFeatureEnabled private val isAutodeleteFeatureEnabled: Boolean
) : ViewModel() {

    private val autoDeleteUpsellingVisibility = MutableStateFlow<BottomSheetVisibilityEffect>(
        BottomSheetVisibilityEffect.Hide
    )

    private val subscriptionNeededErrorVisibility = MutableStateFlow<Effect<TextUiModel>>(Effect.empty())

    private val autoDeleteUpsellingInProgressVisibility = MutableStateFlow<Effect<TextUiModel>>(Effect.empty())

    private val autoDeleteState: StateFlow<AutoDeleteSettingsState> =
        accountManager.getPrimaryUserId().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(AutoDeleteSettingsState())
            }

            combine(
                observeMailSettings(userId),
                observeUpsellingVisibility(UpsellingEntryPoint.Feature.AutoDelete),
                autoDeleteUpsellingVisibility,
                autoDeleteUpsellingInProgressVisibility,
                subscriptionNeededErrorVisibility
            ) { mailSettings, upsellingVisibility, bottomSheetVisibility, upsellingInProgress, subscriptionNeeded ->
                AutoDeleteSettingsState(
                    autoDeleteInDays = mailSettings?.autoDeleteSpamAndTrashDays,
                    isSettingVisible = isAutodeleteFeatureEnabled,
                    isUpsellingVisible = upsellingVisibility,
                    upsellingVisibility = Effect.of(bottomSheetVisibility),
                    upsellingInProgress = upsellingInProgress,
                    doesSettingNeedSubscription = !isPaidMailUser(userId).getOrElse { false },
                    subscriptionNeededError = subscriptionNeeded
                )
            }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis),
            AutoDeleteSettingsState()
        )

    val state: StateFlow<AccountSettingsState> = accountManager.getPrimaryUserId().flatMapLatest { userId ->
        if (userId == null) {
            return@flatMapLatest flowOf(NotLoggedIn)
        }

        combine(
            observeUser(userId),
            observeUserSettings(userId),
            observeMailSettings(userId),
            observeRegisteredSecurityKeys(userId),
            autoDeleteState
        ) { user, userSettings, mailSettings, securityKeys, autoDeleteSettingsState ->
            val (usedSpace, maxSpace) = if (user?.usedBaseSpace != null && user.maxBaseSpace != null) {
                Pair(user.usedBaseSpace, user.maxBaseSpace)
            } else {
                Pair(user?.usedSpace, user?.maxSpace)
            }
            Data(
                getRecoveryEmail(userSettings),
                maxSpace,
                usedSpace,
                user?.email,
                mailSettings?.viewMode?.enum?.let { it == ConversationGrouping },
                registeredSecurityKeys = securityKeys,
                securityKeysVisible = isFido2Enabled(userId),
                autoDeleteSettingsState = autoDeleteSettingsState
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis),
        Loading
    )

    fun submit(action: AccountSettingsViewAction) {
        viewModelScope.launch {
            when (action) {
                is AccountSettingsViewAction.SettingsItemClicked -> {
                    handleSettingsItemClicked()
                }

                AccountSettingsViewAction.DismissUpselling -> handleDismissUpselling()
            }
        }
    }

    private fun getRecoveryEmail(userSettings: UserSettings?) = userSettings?.email?.value?.takeIfNotBlank()

    private fun handleSettingsItemClicked() {
        val isUpsellingInProgress = userUpgradeState.isUserPendingUpgrade

        if (isUpsellingInProgress) {
            autoDeleteUpsellingInProgressVisibility.value = Effect.of(
                TextUiModel(R.string.upselling_snackbar_upgrade_in_progress)
            )

            return
        }

        if (autoDeleteState.value.isUpsellingVisible) {
            autoDeleteUpsellingVisibility.value = BottomSheetVisibilityEffect.Show
        } else if (autoDeleteState.value.doesSettingNeedSubscription) {
            subscriptionNeededErrorVisibility.value = Effect.of(
                TextUiModel(R.string.mail_settings_auto_delete_subscription_needed)
            )
        }
    }

    private fun handleDismissUpselling() {
        autoDeleteUpsellingVisibility.value = BottomSheetVisibilityEffect.Hide
    }
}
