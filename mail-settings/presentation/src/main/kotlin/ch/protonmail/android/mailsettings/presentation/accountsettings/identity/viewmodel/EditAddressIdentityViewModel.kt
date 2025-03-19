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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetPrimaryAddressDisplayName
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetPrimaryAddressSignature
import ch.protonmail.android.mailsettings.domain.usecase.identity.UpdatePrimaryAddressIdentity
import ch.protonmail.android.mailsettings.domain.usecase.identity.UpdatePrimaryUserMobileFooter
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityOperation
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.reducer.EditAddressIdentityReducer
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditAddressIdentityViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val getPrimaryAddressDisplayName: GetPrimaryAddressDisplayName,
    private val getPrimaryAddressSignature: GetPrimaryAddressSignature,
    private val getMobileFooter: GetMobileFooter,
    private val updatePrimaryAddressIdentity: UpdatePrimaryAddressIdentity,
    private val updatePrimaryUserMobileFooter: UpdatePrimaryUserMobileFooter,
    private val reducer: EditAddressIdentityReducer,
    private val observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val userUpgradeState: UserUpgradeState
) : ViewModel() {

    private val mutableState =
        MutableStateFlow<EditAddressIdentityState>(EditAddressIdentityState.Loading)
    val state = mutableState.asStateFlow()

    init {
        observePrimaryUserId().flatMapLatest { userId ->
            userId ?: return@flatMapLatest flowOf(emitNewStateFrom(EditAddressIdentityEvent.Error.LoadingError))

            val displayName = getPrimaryAddressDisplayName(userId).getOrElse {
                return@flatMapLatest flowOf(emitNewStateFrom(EditAddressIdentityEvent.Error.LoadingError))
            }
            val signature = getPrimaryAddressSignature(userId).getOrElse {
                return@flatMapLatest flowOf(emitNewStateFrom(EditAddressIdentityEvent.Error.LoadingError))
            }
            val mobileFooter = getMobileFooter(userId).getOrElse {
                return@flatMapLatest flowOf(emitNewStateFrom(EditAddressIdentityEvent.Error.LoadingError))
            }

            observeUpsellingVisibility(UpsellingEntryPoint.Feature.MobileSignature).flatMapLatest {
                    shouldShowUpselling ->
                val footer = mobileFooter.adaptForUpsell(shouldShowUpselling)
                emitNewStateFrom(
                    EditAddressIdentityEvent.Data.ContentLoaded(
                        displayName,
                        signature,
                        footer
                    )
                )

                userUpgradeState.userUpgradeCheckState.map { checkState ->
                    val postUpgradeFooter = getMobileFooter(userId).getOrNull()
                        ?.adaptForUpsell(shouldShowUpselling)
                        ?: footer
                    emitNewStateFrom(
                        EditAddressIdentityEvent.UpgradeStateChanged(postUpgradeFooter, checkState, shouldShowUpselling)
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun MobileFooter.adaptForUpsell(shouldShowUpselling: Boolean) =
        if (shouldShowUpselling) MobileFooter.FreeUserUpsellingMobileFooter(value) else this

    internal fun submit(action: EditAddressIdentityViewAction) {
        viewModelScope.launch {
            when (action) {
                is EditAddressIdentityViewAction.DisplayName.UpdateValue -> {
                    emitNewStateFrom(EditAddressIdentityViewAction.DisplayName.UpdateValue(action.newValue))
                }

                is EditAddressIdentityViewAction.Signature.UpdateValue -> {
                    emitNewStateFrom(EditAddressIdentityViewAction.Signature.UpdateValue(action.newValue))
                }

                is EditAddressIdentityViewAction.Signature.ToggleState -> {
                    emitNewStateFrom(EditAddressIdentityViewAction.Signature.ToggleState(action.enabled))
                }

                is EditAddressIdentityViewAction.MobileFooter.UpdateValue -> {
                    emitNewStateFrom(EditAddressIdentityViewAction.MobileFooter.UpdateValue(action.newValue))
                }

                is EditAddressIdentityViewAction.MobileFooter.ToggleState -> handleToggleState(action)

                is EditAddressIdentityViewAction.Save -> saveSettings()

                is EditAddressIdentityViewAction.HideUpselling -> emitNewStateFrom(
                    EditAddressIdentityEvent.HideUpselling
                )
            }
        }
    }

    private suspend fun handleToggleState(action: EditAddressIdentityViewAction.MobileFooter.ToggleState) {
        val isUpsellingInProgress = userUpgradeState.isUserPendingUpgrade

        if (!action.enabled && isUpsellingInProgress) {
            return emitNewStateFrom(EditAddressIdentityEvent.UpsellingInProgress)
        }

        val shouldShowUpselling = observeUpsellingVisibility(
            UpsellingEntryPoint.Feature.MobileSignature
        ).first()

        if (!action.enabled && shouldShowUpselling) {
            emitNewStateFrom(EditAddressIdentityEvent.ShowUpselling)
        } else {
            emitNewStateFrom(EditAddressIdentityViewAction.MobileFooter.ToggleState(action.enabled))
        }
    }

    @Suppress("ReturnCount")
    private suspend fun saveSettings() {
        val isUpsellingInProgress = userUpgradeState.isUserPendingUpgrade

        if (isUpsellingInProgress) return emitNewStateFrom(EditAddressIdentityEvent.UpsellingInProgress)

        val state = state.value as? EditAddressIdentityState.DataLoaded ?: return emitNewStateFrom(
            EditAddressIdentityEvent.Error.UpdateError
        )

        val displayName = DisplayName(state.displayNameState.displayNameUiModel.textValue.trim())
        val signature = state.signatureState.addressSignatureUiModel.let {
            Signature(it.enabled, SignatureValue(it.textValue.trim()))
        }

        updatePrimaryAddressIdentity(displayName, signature).getOrElse {
            return emitNewStateFrom(EditAddressIdentityEvent.Error.UpdateError)
        }

        val mobileFooter = state.mobileFooterState.mobileFooterUiModel.let {
            MobileFooterPreference(it.textValue.trim(), it.enabled)
        }

        updatePrimaryUserMobileFooter(mobileFooter).getOrElse {
            return emitNewStateFrom(EditAddressIdentityEvent.Error.UpdateError)
        }

        emitNewStateFrom(EditAddressIdentityEvent.Navigation.Close)
    }

    private fun emitNewStateFrom(event: EditAddressIdentityOperation) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
