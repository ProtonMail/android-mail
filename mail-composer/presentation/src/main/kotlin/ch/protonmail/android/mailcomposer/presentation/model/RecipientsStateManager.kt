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

import ch.protonmail.android.mailcomposer.presentation.mapper.RecipientUiModelMapper
import ch.protonmail.android.mailmessage.domain.model.Participant
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

    fun setFromParticipants(
        toRecipients: List<Participant>,
        ccRecipients: List<Participant>,
        bccRecipients: List<Participant>
    ) {
        mutableRecipients.update {
            it.copy(
                toRecipients = RecipientUiModelMapper.mapFromParticipants(toRecipients).toImmutableList(),
                ccRecipients = RecipientUiModelMapper.mapFromParticipants(ccRecipients).toImmutableList(),
                bccRecipients = RecipientUiModelMapper.mapFromParticipants(bccRecipients).toImmutableList()
            )
        }
    }

    fun hasValidRecipients() = mutableRecipients.value.let {
        it.toRecipients + it.ccRecipients + it.bccRecipients
    }.let { list ->
        list.isNotEmpty() && list.all { it is RecipientUiModel.Valid && it.address.isNotBlank() }
    }
}
