/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcomposer.presentation.ui.chips.item

import android.os.Parcelable
import androidx.compose.runtime.Stable
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import kotlinx.parcelize.Parcelize

@Stable
internal sealed class ChipItem(
    open val value: String,
    open val encryptionInfo: EncryptionInfoUiModel
) : Parcelable {

    @Parcelize
    data class Validating(
        override val value: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : ChipItem(value, encryptionInfo)

    @Parcelize
    data class Valid(
        override val value: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : ChipItem(value, encryptionInfo)

    @Parcelize
    data class Invalid(
        override val value: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : ChipItem(value, encryptionInfo)

    @Parcelize
    data class Counter(
        override val value: String,
        override val encryptionInfo: EncryptionInfoUiModel = EncryptionInfoUiModel.NoLock
    ) : ChipItem(value, encryptionInfo)

    @Parcelize
    data class Group(
        override val value: String,
        val color: String,
        val memberCount: Int,
        val members: List<String>
    ) : ChipItem(value, EncryptionInfoUiModel.NoLock)
}
