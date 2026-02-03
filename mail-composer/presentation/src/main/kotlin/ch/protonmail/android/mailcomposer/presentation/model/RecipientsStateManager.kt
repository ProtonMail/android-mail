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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.presentation.mapper.RecipientUiModelMapper
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class RecipientsStateManager @Inject constructor() {

    private val mutableRecipients = MutableStateFlow(RecipientsState.Empty)
    val recipients = mutableRecipients.asStateFlow()

    fun updateRecipients(values: List<RecipientUiModel>, type: ContactSuggestionsField) {
        val immutableList = values.toImmutableList()
        when (type) {
            ContactSuggestionsField.TO -> mutableRecipients.update { it.copy(toRecipients = immutableList) }
            ContactSuggestionsField.CC -> mutableRecipients.update { it.copy(ccRecipients = immutableList) }
            ContactSuggestionsField.BCC -> mutableRecipients.update { it.copy(bccRecipients = immutableList) }
        }
    }

    fun setFromRawRecipients(
        toRecipients: List<String>,
        ccRecipients: List<String>,
        bccRecipients: List<String>
    ) {
        mutableRecipients.update {
            it.copy(
                toRecipients = RecipientUiModelMapper.mapFromRawValue(toRecipients).toImmutableList(),
                ccRecipients = RecipientUiModelMapper.mapFromRawValue(ccRecipients).toImmutableList(),
                bccRecipients = RecipientUiModelMapper.mapFromRawValue(bccRecipients).toImmutableList()
            )
        }
    }

    fun setFromDraftRecipients(
        toRecipients: List<DraftRecipient>,
        ccRecipients: List<DraftRecipient>,
        bccRecipients: List<DraftRecipient>
    ) {
        mutableRecipients.update {
            it.copy(
                toRecipients = RecipientUiModelMapper.mapFromDraftRecipients(toRecipients).toImmutableList(),
                ccRecipients = RecipientUiModelMapper.mapFromDraftRecipients(ccRecipients).toImmutableList(),
                bccRecipients = RecipientUiModelMapper.mapFromDraftRecipients(bccRecipients).toImmutableList()
            )
        }
    }

    fun hasValidRecipients() = mutableRecipients.value.let {
        it.toRecipients + it.ccRecipients + it.bccRecipients
    }.let { list ->
        list.isNotEmpty() && list.all { it !is RecipientUiModel.Invalid && it.address.isNotBlank() }
    }

    /**
     * Sets all recipients to Validating state with NoLock encryption info.
     * This signals that encryption info is being refreshed (e.g., due to sender change).
     */
    fun resetValidationState() {
        mutableRecipients.update {
            it.copy(
                toRecipients = it.toRecipients.map { recipient -> recipient.toValidating() }.toImmutableList(),
                ccRecipients = it.ccRecipients.map { recipient -> recipient.toValidating() }.toImmutableList(),
                bccRecipients = it.bccRecipients.map { recipient -> recipient.toValidating() }.toImmutableList()
            )
        }
    }

    /**
     * Restores a previously saved recipients state.
     */
    fun restoreState(state: RecipientsState) {
        mutableRecipients.value = state
    }

    private fun RecipientUiModel.toValidating(): RecipientUiModel =
        RecipientUiModel.Validating(address, EncryptionInfoUiModel.NoLock)
}
