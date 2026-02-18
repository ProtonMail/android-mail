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
import ch.protonmail.android.mailcomposer.domain.model.RecipientValidityError
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock

fun RecipientUiModel.toDraftRecipient(): DraftRecipient = when (this) {
    is RecipientUiModel.Group -> DraftRecipient.GroupRecipient(
        name = this.name,
        recipients = this.members.map { email ->
            DraftRecipient.SingleRecipient(
                name = "",
                address = email,
                validity = DraftRecipientValidity.Validating,
                privacyLock = PrivacyLock.None
            )
        }
    )
    is RecipientUiModel.Invalid -> DraftRecipient.SingleRecipient(
        name = "",
        address = this.address,
        validity = DraftRecipientValidity.Invalid(RecipientValidityError.Format),
        privacyLock = PrivacyLock.None
    )
    is RecipientUiModel.Valid -> DraftRecipient.SingleRecipient(
        name = "",
        address = this.address,
        validity = DraftRecipientValidity.Valid,
        privacyLock = PrivacyLock.None
    )
    is RecipientUiModel.Validating -> DraftRecipient.SingleRecipient(
        name = "",
        address = this.address,
        validity = DraftRecipientValidity.Validating,
        privacyLock = PrivacyLock.None
    )
}
