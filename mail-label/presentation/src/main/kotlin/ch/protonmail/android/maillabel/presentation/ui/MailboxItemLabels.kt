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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel
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

@Composable
fun MailboxItemLabels(modifier: Modifier = Modifier, labels: List<MailboxItemLabelUiModel>) {
    SubcomposeLayout(modifier) { constraints ->

        val plusOneDigitWidth = measurePlusTextWidth(constraints, Plus1CharLimit)
        val plusTwoDigitWidth = measurePlusTextWidth(constraints, Plus2CharsLimit)
        val plusThreeDigitWidth = measurePlusTextWidth(constraints, Plus3CharsLimit)
        val minExpandedLabelWidth = measureMinExpandedLabelWidth(constraints)

        val labelsMeasurables = labels.map { label ->
            label to subcompose(label.name) {
                Label(label = label)
            }
        }

        var labelsWidth = 0
        var notPlacedCount = labels.size

        fun plusPlaceableWidth(): Int {
            val notPlacedCountExcludingCurrent = notPlacedCount - 1
            return when {
                notPlacedCountExcludingCurrent <= 0 -> 0
                notPlacedCount <= Plus1CharLimit -> plusOneDigitWidth
                notPlacedCount <= Plus2CharsLimit -> plusTwoDigitWidth
                else -> plusThreeDigitWidth
            }
        }

        val labelsPlaceables = labelsMeasurables.map { (label, measurables) ->
            measurables.mapNotNull subMap@{ measurable ->
                val availableWidth = constraints.maxWidth - labelsWidth - plusPlaceableWidth()
                val maxWidth = availableWidth.coerceAtLeast(minExpandedLabelWidth)
                val minWidth = when {
                    label.name.length >= MinExpandedLabelLength -> minExpandedLabelWidth
                    else -> 0
                }
                val placeable =
                    measurable.measure(constraints.copy(minWidth = minWidth, maxWidth = maxWidth))
                if (placeable.width > availableWidth) {
                    return@subMap null
                }
                labelsWidth += placeable.width
                notPlacedCount--
                placeable
            }
        }

        layout(width = constraints.maxWidth, height = labelsPlaceables.flatten().firstOrNull()?.height ?: 0) {
            var x = 0
            labelsPlaceables.flatten().forEach { placeable ->
                placeable.place(x = x, y = 0)
                x += placeable.width
            }
            if (notPlacedCount > 0) {
                subcompose(notPlacedCount) { PlusText(count = notPlacedCount) }
                    .map { it.measure(constraints) }.forEach { placeable ->
                        placeable.place(x = x, y = 0)
                        x += placeable.width
                    }
            }
        }
    }
}

private fun SubcomposeMeasureScope.measurePlusTextWidth(constraints: Constraints, charsLimit: Int) =
    subcompose(charsLimit) { PlusText(count = charsLimit) }
        .maxOf { it.measure(constraints).width }

private fun SubcomposeMeasureScope.measureMinExpandedLabelWidth(constraints: Constraints) =
    subcompose(DummyMinExpandedLabelId) { Label(label = DummyMinExpandedLabel) }
        .maxOf { it.measure(constraints).width }

@Composable
private fun Label(label: MailboxItemLabelUiModel) {
    Text(
        modifier = Modifier
            .padding(MailDimens.TinySpacing)
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
        modifier = Modifier.padding(MailDimens.TinySpacing),
        text = "+$count",
        style = ProtonTheme.typography.caption,
        maxLines = 1
    )
}

object MailboxItemLabels {

    internal const val MinExpandedLabelLength = 4
    internal const val Plus1CharLimit = 9
    internal const val Plus2CharsLimit = 99
    internal const val Plus3CharsLimit = 999
    internal const val DummyMinExpandedLabelId = "DummyMinimumExpandedLabelId"
    private const val DummyMinExpandedLabelText = "abc..."
    internal val DummyMinExpandedLabel = MailboxItemLabelUiModel(
        name = DummyMinExpandedLabelText,
        color = Color.Unspecified
    )
}

@Composable
@Preview(showBackground = true, widthDp = 400)
private fun MailboxItemLabelsPreview(
    @PreviewParameter(MailboxItemLabelsPreviewDataProvider::class) labels: List<MailboxItemLabelUiModel>
) {
    ProtonTheme {
        MailboxItemLabels(labels = labels)
    }
}
