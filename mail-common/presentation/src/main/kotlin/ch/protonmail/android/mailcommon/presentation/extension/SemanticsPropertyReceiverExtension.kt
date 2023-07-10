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

package ch.protonmail.android.mailcommon.presentation.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import ch.protonmail.android.mailcommon.presentation.extension.CustomSemanticsPropertyKeys.isItemReadKey
import ch.protonmail.android.mailcommon.presentation.extension.CustomSemanticsPropertyKeys.tintColorKey

/**
 * Extension used to specify the tint [Color]? as a custom [SemanticsPropertyKey].
 */
var SemanticsPropertyReceiver.tintColor by tintColorKey

/**
 * Extension used to specify whether the current item is in the unread or read state.
 */
var SemanticsPropertyReceiver.isItemRead by isItemReadKey

private object CustomSemanticsPropertyKeys {

    val tintColorKey = SemanticsPropertyKey<Color?>("TintColorKey")
    val isItemReadKey = SemanticsPropertyKey<Boolean?>("IsItemReadKey")
}
