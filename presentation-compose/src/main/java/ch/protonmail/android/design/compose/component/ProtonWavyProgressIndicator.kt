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

package ch.protonmail.android.design.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.vendored.material3.LinearWavyProgressIndicator
import ch.protonmail.android.vendored.material3.WavyProgressIndicatorDefaults

/**
 * The Material 3 determinate linear wavy progress indicator, in Proton colours.
 *
 * The indicator itself lives in `:presentation-vendored` because it is Material source copied in
 * verbatim. This wrapper exists so callers get the theme colours without reaching into that module.
 *
 * @param progress the fraction done, from 0 to 1. Read in a lambda so progress updates redraw
 * without recomposing the caller.
 * @param waveSpeed how fast the wave travels. Zero freezes it in place, still wavy but not moving.
 */
@Composable
fun ProtonWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProtonTheme.colors.textAccent,
    trackColor: Color = ProtonTheme.colors.interactionBrandWeakNorm,
    waveSpeed: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength
) {
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        waveSpeed = waveSpeed
    )
}

private val PreviewProgressValues = listOf(0.05f, 0.3f, 0.7f, 0.97f)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProtonWavyProgressIndicatorPreview() {
    ProtonTheme {
        Column(
            modifier = Modifier.padding(ProtonDimens.Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large)
        ) {
            PreviewProgressValues.forEach { progress ->
                ProtonWavyProgressIndicator(progress = { progress })
            }
        }
    }
}
