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

package ch.protonmail.android.maillabel.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.maillabel.presentation.previewdata.MailboxItemLabelsPreviewDataProvider
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.DummyMinExpandedLabel
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.DummyMinExpandedLabelId
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.MinExpandedLabelLength
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.Plus1CharLimit
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.Plus2CharsLimit
import ch.protonmail.android.maillabel.presentation.ui.MailboxItemLabels.Plus3CharsLimit
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.overline
import kotlin.math.ceil

@Composable
fun LabelsList(
    modifier: Modifier = Modifier,
    labels: List<LabelUiModel>,
    isExpanded: Boolean = false
) {
    SubcomposeLayout(modifier = modifier.wrapContentSize()) { constraints ->

        val labelsMeasurables = labels.map { label ->
            label to subcompose(label.name) {
                Label(label = label)
            }
        }

        val buildPlaceablesResult = buildPlaceables(
            labels = labels,
            isExpanded = isExpanded,
            labelsMeasurables = labelsMeasurables,
            constraints = constraints
        )
        val labelsPlaceables = buildPlaceablesResult.placeables
        val rowsCount = buildPlaceablesResult.rowsCount
        val rowHeight = buildPlaceablesResult.rowHeight
        val notPlacedCount = buildPlaceablesResult.notPlacedCount

        layout(
            width = constraints.maxWidth,
            height = rowsCount * rowHeight
        ) {
            var x = 0
            var y = 0
            var availableRowWidth = constraints.maxWidth
            labelsPlaceables.flatten().forEach { placeable ->

                val rowCanFitPlaceable = when (isExpanded) {
                    true -> availableRowWidth - placeable.width >= 0
                    false -> true
                }

                if (rowCanFitPlaceable.not()) {
                    availableRowWidth = constraints.maxWidth
                    y += rowHeight
                    x = 0
                }
                placeable.place(x = x, y = y)
                x += placeable.width
                availableRowWidth -= placeable.width
            }

            if (notPlacedCount > 0) {
                subcompose(notPlacedCount) { PlusText(count = notPlacedCount) }
                    .map { it.measure(constraints) }
                    .forEach { placeable ->
                        placeable.place(x = x, y = 0)
                        x += placeable.width
                    }
            }
        }
    }
}

@Suppress("ComplexMethod")
private fun SubcomposeMeasureScope.buildPlaceables(
    labels: List<LabelUiModel>,
    isExpanded: Boolean,
    labelsMeasurables: List<Pair<LabelUiModel, List<Measurable>>>,
    constraints: Constraints
): BuildPlaceablesResult {
    val plusOneDigitWidth = measurePlusTextWidth(constraints, Plus1CharLimit)
    val plusTwoDigitWidth = measurePlusTextWidth(constraints, Plus2CharsLimit)
    val plusThreeDigitWidth = measurePlusTextWidth(constraints, Plus3CharsLimit)
    val minExpandedLabelWidth = measureMinExpandedLabelWidth(constraints)

    var labelsWidth = 0
    var notPlacedCount = labels.size

    fun plusPlaceableWidth(): Int {
        val notPlacedCountExcludingCurrent = notPlacedCount - 1
        return when {
            isExpanded -> 0
            notPlacedCountExcludingCurrent <= 0 -> 0
            notPlacedCount <= Plus1CharLimit -> plusOneDigitWidth
            notPlacedCount <= Plus2CharsLimit -> plusTwoDigitWidth
            else -> plusThreeDigitWidth
        }
    }

    val labelsPlaceables = labelsMeasurables.map { (label, measurables) ->
        measurables.mapNotNull subMap@{ measurable ->
            val availableWidth = when (isExpanded) {
                true -> constraints.maxWidth
                false -> constraints.maxWidth - labelsWidth - plusPlaceableWidth()
            }
            val maxWidth = if (availableWidth <= 0) constraints.maxWidth else
                availableWidth.coerceAtLeast(minExpandedLabelWidth)
            val minWidth = when {
                label.name.length >= MinExpandedLabelLength -> minExpandedLabelWidth
                else -> 0
            }
            val placeable =
                measurable.measure(constraints.copy(minWidth = minWidth, maxWidth = maxWidth))
            if (isExpanded.not() && placeable.width > availableWidth) {
                return@subMap null
            }
            labelsWidth += placeable.width
            notPlacedCount--
            placeable
        }
    }

    val rowsCount = ceil(labelsWidth.toDouble() / constraints.maxWidth.coerceAtLeast(1)).toInt()
    val rowHeight = labelsPlaceables.flatten().firstOrNull()?.height ?: 0

    return BuildPlaceablesResult(
        placeables = labelsPlaceables,
        rowsCount = rowsCount,
        rowHeight = rowHeight,
        notPlacedCount = notPlacedCount
    )
}

private fun SubcomposeMeasureScope.measurePlusTextWidth(constraints: Constraints, charsLimit: Int) =
    subcompose(charsLimit) { PlusText(count = charsLimit) }
        .maxOf { it.measure(constraints).width }

private fun SubcomposeMeasureScope.measureMinExpandedLabelWidth(constraints: Constraints) =
    subcompose(DummyMinExpandedLabelId) { Label(label = DummyMinExpandedLabel) }
        .maxOf { it.measure(constraints).width }

@Composable
private fun Label(label: LabelUiModel) {
    Text(
        modifier = Modifier
            .padding(
                end = ProtonDimens.ExtraSmallSpacing,
                top = MailDimens.TinySpacing,
                bottom = MailDimens.TinySpacing
            )
            .background(label.color, shape = RoundedCornerShape(percent = 100))
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = MailDimens.TinySpacing),
        text = label.name,
        style = ProtonTheme.typography.overline.copy(color = ProtonTheme.colors.floatyText),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PlusText(count: Int) {
    Text(
        modifier = Modifier,
        text = "+$count",
        style = ProtonTheme.typography.caption,
        maxLines = 1
    )
}

private data class BuildPlaceablesResult(
    val placeables: List<List<Placeable>>,
    val rowsCount: Int,
    val rowHeight: Int,
    val notPlacedCount: Int
)

object MailboxItemLabels {

    internal const val MinExpandedLabelLength = 4
    internal const val Plus1CharLimit = 9
    internal const val Plus2CharsLimit = 99
    internal const val Plus3CharsLimit = 999
    internal const val DummyMinExpandedLabelId = "DummyMinimumExpandedLabelId"
    private const val DummyMinExpandedLabelText = "abc..."
    internal val DummyMinExpandedLabel = LabelUiModel(
        name = DummyMinExpandedLabelText,
        color = Color.Unspecified
    )
}

@Composable
@Preview(showBackground = true, widthDp = 400)
private fun MailboxItemLabelsPreview(
    @PreviewParameter(MailboxItemLabelsPreviewDataProvider::class) labels: List<LabelUiModel>
) {
    ProtonTheme {
        LabelsList(labels = labels)
    }
}
