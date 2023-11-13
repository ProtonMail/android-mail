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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.sample.LabelColorListSample.colorListSample
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm

@Composable
fun ColorPicker(
    colors: List<Color>,
    selectedColor: Color,
    onColorClicked: (Color) -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.color_picker_title),
            modifier = Modifier.padding(
                top = ProtonDimens.MediumSpacing,
                start = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing
            ),
            style = ProtonTheme.typography.defaultSmallStrongNorm
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = MailDimens.ColorPicker.CircleBoxSize)
        ) {
            items(colors) { color ->
                ColorItem(
                    color = color,
                    isSelected = color == selectedColor,
                    onColorClicked = onColorClicked
                )
            }
        }
    }
}

@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onColorClicked: (Color) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier.padding(
            horizontal = ProtonDimens.ExtraSmallSpacing,
            vertical = ProtonDimens.SmallSpacing
        )
    ) {
        val (item, selectedItem) = createRefs()

        if (isSelected) {
            Box(
                modifier = Modifier
                    .constrainAs(selectedItem) {
                        centerTo(parent)
                    }
                    .size(MailDimens.ColorPicker.SelectedCircleSize)
                    .clip(CircleShape)
                    .border(MailDimens.ColorPicker.SelectedCircleBorderSize, color, CircleShape)
                    .background(ProtonTheme.colors.backgroundNorm)
                    .clickable {
                        onColorClicked(color)
                    }
            )
        }
        Box(
            modifier = Modifier
                .constrainAs(item) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
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
        colors = colorListSample(),
        selectedColor = colorListSample().random(),
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = false,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun SelectedColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = true,
        onColorClicked = {}
    )
}
