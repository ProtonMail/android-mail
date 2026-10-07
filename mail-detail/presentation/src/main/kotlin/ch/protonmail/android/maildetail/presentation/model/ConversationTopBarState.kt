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

package ch.protonmail.android.maildetail.presentation.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import ch.protonmail.android.mailcommon.presentation.Effect

@Stable
class ConversationTopBarState {

    val messages = mutableStateOf<Int?>(0)
    val title = mutableStateOf("")
    val highlightText = mutableStateOf("")
    val isStarred = mutableStateOf<Boolean?>(false)
    val subjectAlpha = mutableFloatStateOf(0f)

    val topBarStarClickEffect = mutableStateOf(Effect.empty<Boolean>())

    fun onStarClick() {
        topBarStarClickEffect.value = Effect.of(true)
    }

    fun onStarUnClick() {
        topBarStarClickEffect.value = Effect.of(false)
    }

    fun updateSubjectAlpha(alpha: Float) {
        subjectAlpha.floatValue = alpha.coerceIn(0f, 1f)
    }
}
