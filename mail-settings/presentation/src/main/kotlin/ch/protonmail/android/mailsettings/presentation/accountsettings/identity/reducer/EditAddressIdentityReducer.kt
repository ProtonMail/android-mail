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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.mapper.EditAddressIdentityMapper
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityEvent
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityOperation
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.EditAddressIdentityViewAction
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import javax.inject.Inject

class EditAddressIdentityReducer @Inject constructor(
    private val editAddressIdentityMapper: EditAddressIdentityMapper
) {

    fun newStateFrom(currentState: EditAddressIdentityState, operation: EditAddressIdentityOperation) =
        when (operation) {
            is EditAddressIdentityViewAction -> operation.newStateForAction(currentState)
            is EditAddressIdentityEvent -> currentState.toNewStateFromEvent(operation)
        }

    private fun EditAddressIdentityState.toNewStateFromEvent(
        event: EditAddressIdentityEvent
    ): EditAddressIdentityState {

        return when (this) {
            is EditAddressIdentityState.Loading -> when (event) {
                is EditAddressIdentityEvent.Data.ContentLoaded -> event.toDataState()
                is EditAddressIdentityEvent.Error -> EditAddressIdentityState.LoadingError
                else -> this
            }

            is EditAddressIdentityState.DataLoaded -> when (event) {
                is EditAddressIdentityEvent.Error.UpdateError -> this.copy(
                    updateError = Effect.of(Unit)
                )

                EditAddressIdentityEvent.Navigation.Close -> this.copy(
                    close = Effect.of(Unit)
                )

                EditAddressIdentityEvent.HideUpselling -> this.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
                )

                EditAddressIdentityEvent.ShowUpselling -> this.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show)
                )

                EditAddressIdentityEvent.UpsellingInProgress -> this.copy(
                    upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress))
                )

                is EditAddressIdentityEvent.UpgradeStateChanged -> this.copy(
                    mobileFooterState = this.mobileFooterState.copy(
                        mobileFooterUiModel = editAddressIdentityMapper.toMobileFooterUiModel(
                            event.mobileFooter,
                            isUpgradePending = event.userUpgradeCheckState
                            is UserUpgradeState.UserUpgradeCheckState.Pending
                        )
                    )
                )

                else -> this
            }

            else -> this
        }
    }

    private fun EditAddressIdentityViewAction.newStateForAction(
        currentState: EditAddressIdentityState
    ): EditAddressIdentityState {
        return when (currentState) {
            is EditAddressIdentityState.DataLoaded -> when (this) {
                is EditAddressIdentityViewAction.DisplayName.UpdateValue -> updateDisplayName(currentState, newValue)
                is EditAddressIdentityViewAction.Signature.ToggleState -> updateSignatureToggle(currentState, enabled)
                is EditAddressIdentityViewAction.Signature.UpdateValue -> updateSignatureValue(currentState, newValue)
                is EditAddressIdentityViewAction.MobileFooter.ToggleState -> updateMobileFooterToggle(
                    currentState,
                    enabled
                )

                is EditAddressIdentityViewAction.MobileFooter.UpdateValue -> updateMobileFooterValue(
                    currentState,
                    newValue
                )

                EditAddressIdentityViewAction.HideUpselling -> hideUpselling(currentState)

                else -> currentState
            }

            else -> currentState
        }
    }

    private fun updateDisplayName(
        currentState: EditAddressIdentityState.DataLoaded,
        newValue: String
    ): EditAddressIdentityState.DataLoaded {
        val updatedUiModel = currentState.displayNameState.displayNameUiModel.copy(textValue = newValue)
        return currentState.copy(
            displayNameState = EditAddressIdentityState.DisplayNameState(updatedUiModel)
        )
    }

    private fun updateSignatureToggle(
        currentState: EditAddressIdentityState.DataLoaded,
        newValue: Boolean
    ): EditAddressIdentityState.DataLoaded {
        val signatureUiModel = currentState.signatureState.addressSignatureUiModel.copy(enabled = newValue)
        return currentState.copy(
            signatureState = EditAddressIdentityState.SignatureState(signatureUiModel)
        )
    }

    private fun updateSignatureValue(
        currentState: EditAddressIdentityState.DataLoaded,
        newValue: String
    ): EditAddressIdentityState.DataLoaded {
        val signatureUiModel = currentState.signatureState.addressSignatureUiModel.copy(textValue = newValue)
        return currentState.copy(
            signatureState = EditAddressIdentityState.SignatureState(signatureUiModel)
        )
    }

    private fun updateMobileFooterValue(
        currentState: EditAddressIdentityState.DataLoaded,
        newValue: String
    ): EditAddressIdentityState.DataLoaded {
        val mobileFooterUiModel = currentState.mobileFooterState.mobileFooterUiModel.copy(textValue = newValue)
        return currentState.copy(
            mobileFooterState = EditAddressIdentityState.MobileFooterState(mobileFooterUiModel)
        )
    }

    private fun updateMobileFooterToggle(
        currentState: EditAddressIdentityState.DataLoaded,
        newValue: Boolean
    ): EditAddressIdentityState.DataLoaded {
        val mobileFooterUiModel = currentState.mobileFooterState.mobileFooterUiModel.copy(enabled = newValue)
        return currentState.copy(
            mobileFooterState = EditAddressIdentityState.MobileFooterState(mobileFooterUiModel)
        )
    }

    private fun hideUpselling(currentState: EditAddressIdentityState.DataLoaded): EditAddressIdentityState.DataLoaded {
        return currentState.copy(
            upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
        )
    }

    private fun EditAddressIdentityEvent.Data.ContentLoaded.toDataState(): EditAddressIdentityState.DataLoaded {
        val displayNameUiModel = editAddressIdentityMapper.toDisplayNameUiModel(displayName)
        val signatureUiModel = editAddressIdentityMapper.toSignatureUiModel(signature)
        val mobileFooterUiModel = editAddressIdentityMapper.toMobileFooterUiModel(mobileFooter)

        return EditAddressIdentityState.DataLoaded(
            EditAddressIdentityState.DisplayNameState(displayNameUiModel),
            EditAddressIdentityState.SignatureState(signatureUiModel),
            EditAddressIdentityState.MobileFooterState(mobileFooterUiModel),
            Effect.empty(),
            Effect.empty(),
            Effect.empty(),
            Effect.empty()
        )
    }
}
