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

package ch.protonmail.android.mailcomposer.presentation.mapper

import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipientValidity
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailpadlocks.presentation.mapper.EncryptionInfoUiModelMapper

internal object RecipientUiModelMapper {

    fun mapFromRawValue(values: List<String>) = values.map { it.toModel() }
    fun mapFromDraftRecipients(values: List<DraftRecipient>) = values.map { it.toUiModel() }

    private fun String.toModel() = RecipientUiModel.Valid(this)

    private fun DraftRecipient.toUiModel() = when (val recipient = this) {
        is DraftRecipient.GroupRecipient -> RecipientUiModel.Group(
            name = recipient.name,
            members = recipient.recipients.map { it.address },
            color = "" // Currently not exposed by the SDK
        )
        is DraftRecipient.SingleRecipient -> singleRecipientToUiModel(recipient)
    }

    private fun singleRecipientToUiModel(recipient: DraftRecipient.SingleRecipient): RecipientUiModel {
        val encryptionInfo = EncryptionInfoUiModelMapper.fromPrivacyLock(recipient.privacyLock)

        return when (recipient.validity) {
            is DraftRecipientValidity.Invalid -> RecipientUiModel.Invalid(recipient.address, encryptionInfo)
            DraftRecipientValidity.Valid -> RecipientUiModel.Valid(recipient.address, encryptionInfo)
            DraftRecipientValidity.Validating -> RecipientUiModel.Validating(recipient.address, encryptionInfo)
        }
    }
}
