/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.protonmail.android.vendored.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastCoerceIn

/*
 * Ported from androidx.compose.material3 1.5.0-alpha28, file
 * `androidx/compose/material3/WavyProgressIndicator.kt`.
 *
 * Material 3 only ships this in 1.5.0-alpha, which needs compileSdk 37 and AGP 9.1.
 *
 * To be replaced once M3 1.5.0 is stable and we have the same compileSdk + AGP versions.
 */

/**
 * A determinate linear wavy progress indicator.
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color the progress indicator color
 * @param trackColor the indicator's track color, visible when the progress has not reached the area
 * of the overall indicator yet
 * @param stroke a [Stroke] that will be used to draw this indicator
 * @param trackStroke a [Stroke] that will be used to draw the indicator's track
 * @param gapSize the gap between the track and the progress parts of the indicator
 * @param stopSize the size of the stop indicator at the end of the track
 * @param amplitude a lambda that provides an amplitude for the wave path as a function of the
 * indicator's progress. 0.0 represents no amplitude, and 1.0 represents an amplitude that will take
 * the full height of the progress indicator. Values outside of this range are coerced into the
 * range.
 * @param wavelength the length of a wave. Will be applied in case the path has an [amplitude] that
 * is greater than zero and represents a wave.
 * @param waveSpeed the speed in which the wave will move when the [amplitude] is greater than zero.
 * The value here represents a DP per seconds, and by default it's matched to the [wavelength] to
 * render an animation that moves the wave by one wave length per second.
 */
@Composable
@Suppress("LongParameterList")
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    stroke: Stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
    trackStroke: Stroke = WavyProgressIndicatorDefaults.linearTrackStroke,
    gapSize: Dp = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
    stopSize: Dp = WavyProgressIndicatorDefaults.LinearTrackStopIndicatorSize,
    amplitude: (progress: Float) -> Float = WavyProgressIndicatorDefaults.indicatorAmplitude,
    wavelength: Dp = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
    waveSpeed: Dp = wavelength // Match to 1 wavelength per second
) {
    Spacer(
        modifier = modifier
            .then(IncreaseVerticalSemanticsBounds)
            .semantics(mergeDescendants = true) {
                // Ensure progress lambda is accessed safely for semantics
                val currentProgress = progress().takeUnless { it.isNaN() } ?: 0f
                progressBarRangeInfo = ProgressBarRangeInfo(currentProgress.fastCoerceIn(0f, 1f), 0f..1f)
            }
            .size(
                width = WavyProgressIndicatorDefaults.LinearContainerWidth,
                height = WavyProgressIndicatorDefaults.LinearContainerHeight
            )
            .clipToBounds()
            .linearWavyProgressIndicator(
                progress = progress,
                color = color,
                trackColor = trackColor,
                stroke = stroke,
                trackStroke = trackStroke,
                gapSize = gapSize,
                stopSize = stopSize,
                amplitude = amplitude,
                wavelength = wavelength,
                waveSpeed = waveSpeed
            )
    )
}

object WavyProgressIndicatorDefaults {

    /**
     * A default [AnimationSpec] that should be used when animating between progress in a
     * determinate progress indicator.
     */
    val ProgressAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = MotionTokens.DurationLong2.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier
    )

    /** A default active indicator [Color]. */
    val indicatorColor: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    /** A default track [Color]. */
    val trackColor: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer

    /** A default linear progress indicator active indicator [Stroke]. */
    val linearIndicatorStroke: Stroke
        @Composable get() = Stroke(
            width = with(LocalDensity.current) { LinearProgressIndicatorTokens.ActiveThickness.toPx() },
            cap = StrokeCap.Round
        )

    /** A default linear progress indicator track [Stroke]. */
    val linearTrackStroke: Stroke
        @Composable get() = Stroke(
            width = with(LocalDensity.current) { LinearProgressIndicatorTokens.TrackThickness.toPx() },
            cap = StrokeCap.Round
        )

    /** A default wavelength of a determinate linear progress indicator when it's in a wavy form. */
    val LinearDeterminateWavelength: Dp = LinearProgressIndicatorTokens.ActiveWaveWavelength

    /** A default linear progress indicator container height. */
    val LinearContainerHeight: Dp = LinearProgressIndicatorTokens.WaveHeight

    /** A default linear progress indicator container width. */
    val LinearContainerWidth: Dp = 240.dp

    /** A default linear stop indicator size. */
    val LinearTrackStopIndicatorSize: Dp = LinearProgressIndicatorTokens.StopSize

    /**
     * A default gap size that appears in between the active indicator and the track at the linear
     * progress indicator.
     */
    val LinearIndicatorTrackGapSize: Dp = LinearProgressIndicatorTokens.TrackActiveSpace

    /** A function that returns a determinate indicator's amplitude for a given progress. */
    val indicatorAmplitude: (progress: Float) -> Float = { progress ->
        // Sets the amplitude to the max on 10%, and back to zero on 95% of the progress.
        if (progress <= 0.1f || progress >= 0.95f) {
            0f
        } else {
            1f
        }
    }
}

// Animation spec for increasing the amplitude drawing when its changing.
internal val IncreasingAmplitudeAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = MotionTokens.DurationLong2.toInt(),
    easing = MotionTokens.EasingStandardCubicBezier
)

// Animation spec for decreasing the amplitude drawing when its changing.
internal val DecreasingAmplitudeAnimationSpec: AnimationSpec<Float> = tween(
    durationMillis = MotionTokens.DurationLong2.toInt(),
    easing = MotionTokens.EasingEmphasizedAccelerateCubicBezier
)

private val VerticalSemanticsBoundsPadding: Dp = 10.dp

/**
 * Increases the semantics bounds vertically by [VerticalSemanticsBoundsPadding] in order to meet
 * the TalkBack box minimum size while preserving the visual appearance.
 */
private val IncreaseVerticalSemanticsBounds: Modifier = Modifier
    .layout { measurable, constraints ->
        val paddingPx = VerticalSemanticsBoundsPadding.roundToPx()
        // We need to add vertical padding to the semantics bounds in order to meet
        // screenreader green box minimum size, but we also want to
        // preserve a visual appearance and layout size below that minimum
        // in order to maintain backwards compatibility. This custom
        // layout effectively implements "negative padding".
        val newConstraint = constraints.offset(0, paddingPx * 2)
        val placeable = measurable.measure(newConstraint)

        // But when actually placing the placeable, create the layout without additional
        // space. Place the placeable where it would've been without any extra padding.
        val height = placeable.height - paddingPx * 2
        val width = placeable.width
        layout(width, height) { placeable.place(0, -paddingPx) }
    }
    .semantics(mergeDescendants = true) {}
    .padding(vertical = VerticalSemanticsBoundsPadding)

/** The subset of `androidx.compose.material3.tokens.LinearProgressIndicatorTokens` used here. */
private object LinearProgressIndicatorTokens {

    val ActiveThickness = 4.0.dp
    val ActiveWaveWavelength = 40.0.dp
    val StopSize = 4.0.dp
    val TrackActiveSpace = 4.0.dp
    val TrackThickness = 4.0.dp
    val WaveHeight = 10.0.dp
}

/** The subset of `androidx.compose.material3.tokens.MotionTokens` used here. */
private object MotionTokens {

    const val DurationLong2 = 500.0
    val EasingEmphasizedAccelerateCubicBezier = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EasingLinearCubicBezier = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
    val EasingStandardCubicBezier = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}
