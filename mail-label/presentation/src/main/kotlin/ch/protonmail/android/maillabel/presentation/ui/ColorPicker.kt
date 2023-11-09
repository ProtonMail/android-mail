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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.presentation.LabelColors
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getLabelColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm
import kotlin.math.ceil
import kotlin.math.floor

@SuppressWarnings("UnusedPrivateMember")
@Composable
fun ColorPicker(selectedColor: Color, onColorClicked: (Color) -> Unit) {
    Column {
        Text(
            text = stringResource(id = R.string.color_picker_title),
            modifier = Modifier.padding(
                top = ProtonDimens.MediumSpacing,
                start = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.DefaultSpacing
            ),
            style = ProtonTheme.typography.defaultSmallStrongNorm
        )

        val colors = getLabelColors()
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val circleRadius = MailDimens.ColorPicker.CircleSize.div(2)
        val padding = MailDimens.ColorPicker.CircleHorizontalPadding.times(2)
        val boxSize = circleRadius.plus(padding)
        val columnsCount = floor(screenWidth.div(boxSize)).toInt()
        val rowsCount = ceil(colors.size.toDouble().div(columnsCount.toDouble())).toInt()
        var colorIndex = 0
        for (rowIndex in 0 until rowsCount) {
            Row {
                for (columnIndex in 0 until columnsCount - 1) {
                    val color = colors[colorIndex]
                    if (color == selectedColor) {
                        SelectedColorItem(
                            color = color,
                            onColorClicked = onColorClicked
                        )
                    } else {
                        ColorItem(
                            color = color,
                            onColorClicked = onColorClicked
                        )
                    }
                    if (colorIndex == colors.lastIndex) return
                    colorIndex++
                }
            }
        }
    }
}

@Composable
fun ColorItem(color: Color, onColorClicked: (Color) -> Unit) {
    Box(
        modifier = Modifier
            .padding(
                horizontal = MailDimens.ColorPicker.CircleHorizontalPadding,
                vertical = MailDimens.ColorPicker.CircleVerticalPadding
            )
            .size(MailDimens.ColorPicker.CircleSize)
            .clip(CircleShape)
            .background(color)
            .clickable {
                onColorClicked(color)
            }
    )
}

@Composable
fun SelectedColorItem(color: Color, onColorClicked: (Color) -> Unit) {
    ConstraintLayout {
        val (item, selectedItem) = createRefs()

        Box(
            modifier = Modifier
                .padding(
                    horizontal = MailDimens.ColorPicker.SelectedCircleHorizontalPadding
                )
                .constrainAs(selectedItem) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
                .size(MailDimens.ColorPicker.SelectedCircleSize)
                .clip(CircleShape)
                .border(MailDimens.ColorPicker.SelectedCircleBorderSize, color, CircleShape)
                .background(ProtonTheme.colors.backgroundNorm)
                .clickable {
                    onColorClicked(color)
                }
        )
        Box(
            modifier = Modifier
                .padding(
                    horizontal = MailDimens.ColorPicker.SelectedCircleHorizontalPadding
                )
                .constrainAs(item) {
                    top.linkTo(parent.top, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                    start.linkTo(parent.start, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                    end.linkTo(parent.end, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                    bottom.linkTo(parent.bottom, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                }
                .size(MailDimens.ColorPicker.CircleSize)
                .clip(CircleShape)
                .background(color)
                .clickable {
                    onColorClicked(color)
                }
        )
    }

}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ColorPickerPreview() {
    ColorPicker(
        selectedColor = LabelColors.PlumBase,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ColorItemPreview() {
    ColorItem(
        color = LabelColors.PlumBase,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun SelectedColorItemPreview() {
    SelectedColorItem(
        color = LabelColors.PlumBase,
        onColorClicked = {}
    )
}

