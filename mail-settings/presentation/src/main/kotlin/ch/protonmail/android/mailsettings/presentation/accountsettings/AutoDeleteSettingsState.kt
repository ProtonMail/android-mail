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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect

data class AutoDeleteSettingsState(
    val autoDeleteInDays: Int? = null,
    val isSettingVisible: Boolean = false,
    val doesSettingNeedSubscription: Boolean = false,
    val isUpsellingVisible: Boolean = false,
    val upsellingVisibility: Effect<BottomSheetVisibilityEffect> = Effect.empty(),
    val upsellingInProgress: Effect<TextUiModel> = Effect.empty(),
    val subscriptionNeededError: Effect<TextUiModel> = Effect.empty()
)
