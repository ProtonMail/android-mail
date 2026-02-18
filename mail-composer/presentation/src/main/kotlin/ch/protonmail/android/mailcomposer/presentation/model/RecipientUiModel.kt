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

import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import kotlinx.collections.immutable.toImmutableList

sealed class RecipientUiModel(
    open val address: String,
    open val encryptionInfo: EncryptionInfoUiModel
) {

    data class Valid(
        override val address: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : RecipientUiModel(address, encryptionInfo)

    data class Invalid(
        override val address: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : RecipientUiModel(address, encryptionInfo)

    data class Validating(
        override val address: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : RecipientUiModel(address, encryptionInfo)

    data class Group(
        val name: String,
        val members: List<String>,
        val color: String
    ) : RecipientUiModel(address = name, encryptionInfo = EncryptionInfoUiModel.NoLock)
}

internal fun List<RecipientUiModel>.toImmutableChipList() = this.map { it.toChipItem() }.toImmutableList()

private fun RecipientUiModel.toChipItem(): ChipItem = when (this) {
    is RecipientUiModel.Invalid -> ChipItem.Invalid(address, encryptionInfo)
    is RecipientUiModel.Valid -> ChipItem.Valid(address, encryptionInfo)
    is RecipientUiModel.Validating -> ChipItem.Validating(address, encryptionInfo)
    is RecipientUiModel.Group -> ChipItem.Group(
        value = name,
        color = color,
        memberCount = members.size,
        members = members
    )
}
