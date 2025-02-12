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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import ch.protonmail.android.uicomponents.thenIf
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun EnabledStateIconButton(
    icon: Painter,
    isEnabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledColor = ProtonTheme.colors.iconNorm
    val disabledColor = ProtonTheme.colors.iconDisabled

    val animatedColor = remember { Animatable(if (isEnabled) enabledColor else disabledColor) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isEnabled) {
        val targetColor = if (isEnabled) enabledColor else disabledColor

        // Animate to the target color without abrupt interruptions
        if (animatedColor.targetValue != targetColor) {
            scope.launch {
                animatedColor.animateTo(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 500)
                )
            }
        }
    }

    IconButton(
        modifier = modifier
            .thenIf(!isEnabled) { semantics { disabled() } },
        onClick = onClick,
        enabled = isEnabled
    ) {
        Icon(
            painter = icon,
            tint = animatedColor.value,
            contentDescription = contentDescription
        )
    }
}
