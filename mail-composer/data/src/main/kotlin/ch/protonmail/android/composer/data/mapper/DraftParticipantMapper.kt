/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.composer.data.mapper

import ch.protonmail.android.composer.data.local.LocalSenderAddresses
import ch.protonmail.android.mailcommon.data.mapper.LocalComposerRecipient
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipientValidity
import ch.protonmail.android.mailcomposer.domain.model.RecipientValidityError
import ch.protonmail.android.mailcomposer.domain.model.SenderAddresses
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailpadlocks.data.mapper.toPrivacyLock
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import uniffi.mail_uniffi.ComposerRecipient
import uniffi.mail_uniffi.ComposerRecipientSingle
import uniffi.mail_uniffi.ComposerRecipientValidState
import uniffi.mail_uniffi.DraftSenderAddressList
import uniffi.mail_uniffi.RecipientInvalidReason
import uniffi.mail_uniffi.SingleRecipientEntry

fun DraftSenderAddressList.toLocalSenderAddresses() = LocalSenderAddresses(this.available, this.active)
fun LocalSenderAddresses.toSenderAddresses() = SenderAddresses(
    this.addresses.map { SenderEmail(it) },
    SenderEmail(this.selected)
)

fun List<LocalComposerRecipient>.toSingleRecipients(): List<DraftRecipient.SingleRecipient> = this
    .filterIsInstance<ComposerRecipient.Single>()
    .map {
        val localRecipient = it.v1
        DraftRecipient.SingleRecipient(
            name = localRecipient.displayName,
            address = localRecipient.address,
            validity = localRecipient.validState.toDraftRecipientValidity(),
            privacyLock = localRecipient.privacyLock?.toPrivacyLock() ?: PrivacyLock.None
        )
    }

fun List<LocalComposerRecipient>.toGroupRecipients(): List<DraftRecipient.GroupRecipient> = this
    .filterIsInstance<ComposerRecipient.Group>()
    .map { group ->
        DraftRecipient.GroupRecipient(
            name = group.v1.displayName,
            recipients = group.v1.recipients.map { it.toSingleDraftRecipient() }
        )
    }

fun List<LocalComposerRecipient>.toComposerRecipients(): List<DraftRecipient> = this.map { localRecipient ->
    when (localRecipient) {
        is ComposerRecipient.Group -> DraftRecipient.GroupRecipient(
            localRecipient.v1.displayName,
            localRecipient.v1.recipients.map { it.toSingleDraftRecipient() }
        )
        is ComposerRecipient.Single -> localRecipient.v1.toSingleDraftRecipient()
    }
}

private fun ComposerRecipientSingle.toSingleDraftRecipient() = DraftRecipient.SingleRecipient(
    name = this.displayName,
    address = this.address,
    validity = this.validState.toDraftRecipientValidity(),
    privacyLock = this.privacyLock?.toPrivacyLock() ?: PrivacyLock.None
)

private fun ComposerRecipientValidState.toDraftRecipientValidity() = when (this) {
    is ComposerRecipientValidState.Invalid -> DraftRecipientValidity.Invalid(this.v1.toRecipientValidityError())
    is ComposerRecipientValidState.Valid -> DraftRecipientValidity.Valid
    is ComposerRecipientValidState.Validating -> DraftRecipientValidity.Validating
}

private fun RecipientInvalidReason.toRecipientValidityError() = when (this) {
    RecipientInvalidReason.FORMAT -> RecipientValidityError.Format
    RecipientInvalidReason.DOES_NOT_EXIST -> RecipientValidityError.NonexistentAddress
    RecipientInvalidReason.UNKNOWN -> RecipientValidityError.Other
}

fun DraftRecipient.SingleRecipient.toSingleRecipientEntry() = SingleRecipientEntry(
    this.name,
    this.address
)
