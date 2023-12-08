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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.sample.LabelColorListSample.colorListSample
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    colors: List<Color>,
    selectedColor: Color,
    iconResId: Int? = null,
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

        FlowRow(Modifier.padding(start = ProtonDimens.SmallSpacing)) {
            for (color in colors) {
                ColorItem(
                    color = color,
                    isSelected = color == selectedColor,
                    iconResId = iconResId,
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
    iconResId: Int? = null,
    onColorClicked: (Color) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .padding(
                horizontal = ProtonDimens.ExtraSmallSpacing,
                vertical = ProtonDimens.SmallSpacing
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onColorClicked(color)
            }
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
            )
        }

        if (iconResId == null) {
            Box(
                modifier = Modifier
                    .constrainAs(item) {
                        top.linkTo(parent.top, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        start.linkTo(parent.start, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        end.linkTo(parent.end, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        bottom.linkTo(parent.bottom, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                    }
                    .size(MailDimens.ColorPicker.CircleSize)
                    .clip(CircleShape)
                    .background(color)
            )
        } else {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = color,
                modifier = Modifier
                    .constrainAs(item) {
                        top.linkTo(parent.top, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        start.linkTo(parent.start, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        end.linkTo(parent.end, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                        bottom.linkTo(parent.bottom, margin = MailDimens.ColorPicker.SelectedCircleInternalMargin)
                    }
                    .size(ProtonDimens.DefaultIconSize)
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CircleColorPickerPreview() {
    ColorPicker(
        colors = colorListSample(),
        selectedColor = colorListSample().random(),
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun CircleColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = false,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun SelectedCircleColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = true,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun IconColorPickerPreview() {
    ColorPicker(
        colors = colorListSample(),
        selectedColor = colorListSample().random(),
        iconResId = R.drawable.ic_proton_folder_filled,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun IconColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = false,
        iconResId = R.drawable.ic_proton_folder_filled,
        onColorClicked = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun SelectedIconColorItemPreview() {
    ColorItem(
        color = colorListSample().random(),
        isSelected = true,
        iconResId = R.drawable.ic_proton_folder_filled,
        onColorClicked = {}
    )
}
