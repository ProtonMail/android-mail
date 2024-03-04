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

package ch.protonmail.android.mailcontact.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.ColorHexWithName
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.usecase.GetColorHexWithNameList
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultStrongUnspecified
import me.proton.core.compose.theme.headlineNorm

@Composable
fun ColorPickerDialog(
    modifier: Modifier = Modifier,
    title: String,
    selectedValue: Color,
    values: List<ColorHexWithName>,
    onDismissRequest: () -> Unit,
    onValueSelected: (Color) -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(MailDimens.DialogCardRadius),
            modifier = modifier
                .padding(ProtonDimens.DefaultSpacing)
                .fillMaxWidth()
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .background(ProtonTheme.colors.backgroundNorm)
            ) {
                val (header, cancelButton, typeList) = createRefs()

                Text(
                    text = title,
                    style = ProtonTheme.typography.headlineNorm,
                    modifier = Modifier
                        .constrainAs(header) {
                            top.linkTo(parent.top, margin = ProtonDimens.MediumSpacing)
                            start.linkTo(parent.start, margin = ProtonDimens.MediumSpacing)
                        }
                )
                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier
                        .constrainAs(cancelButton) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                ) {
                    Text(
                        text = stringResource(R.string.picker_dialog_cancel),
                        style = ProtonTheme.typography.defaultStrongUnspecified,
                        color = ProtonTheme.colors.brandNorm
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .constrainAs(typeList) {
                            top.linkTo(header.bottom, margin = ProtonDimens.DefaultSpacing)
                            bottom.linkTo(cancelButton.top, margin = ProtonDimens.SmallSpacing)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            height = Dimension.preferredWrapContent
                        }
                ) {
                    items(values) { value ->
                        val color = value.colorHex.getColorFromHexString()
                        val isSelected = color == selectedValue
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton
                                ) { onValueSelected(color) }
                                .padding(
                                    horizontal = ProtonDimens.DefaultSpacing,
                                    vertical = MailDimens.PickerDialogItemVerticalPadding
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColoredRadioButton(
                                color = color,
                                isSelected = isSelected
                            )
                            Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
                            Text(
                                modifier = Modifier.weight(1f, fill = true),
                                text = value.name.string(),
                                style = ProtonTheme.typography.defaultNorm
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ColoredRadioButton(
    modifier: Modifier = Modifier,
    color: Color,
    isSelected: Boolean
) {
    ConstraintLayout(
        modifier = modifier
    ) {
        val (item, selectedItem) = createRefs()

        if (isSelected) {
            Box(
                modifier = Modifier
                    .constrainAs(selectedItem) {
                        centerTo(parent)
                    }
                    .size(MailDimens.ColoredRadioButton.SelectedCircleSize)
                    .clip(CircleShape)
                    .border(MailDimens.ColoredRadioButton.SelectedCircleBorderSize, color, CircleShape)
                    .background(ProtonTheme.colors.backgroundNorm)
            )
        }

        Box(
            modifier = Modifier
                .constrainAs(item) {
                    top.linkTo(parent.top, margin = MailDimens.ColoredRadioButton.SelectedCircleInternalMargin)
                    start.linkTo(parent.start, margin = MailDimens.ColoredRadioButton.SelectedCircleInternalMargin)
                    end.linkTo(parent.end, margin = MailDimens.ColoredRadioButton.SelectedCircleInternalMargin)
                    bottom.linkTo(parent.bottom, margin = MailDimens.ColoredRadioButton.SelectedCircleInternalMargin)
                }
                .size(MailDimens.ColoredRadioButton.CircleSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ColorPickerDialogPreview() {
    ColorPickerDialog(
        title = "Color picker dialog",
        selectedValue = Color.Red,
        values = listOf(
            ColorHexWithName(TextUiModel(R.string.color_purple), GetColorHexWithNameList.Colors.PurpleBase),
            ColorHexWithName(TextUiModel(R.string.color_enzian), GetColorHexWithNameList.Colors.EnzianBase),
            ColorHexWithName(TextUiModel(R.string.color_pink), GetColorHexWithNameList.Colors.PinkBase),
            ColorHexWithName(TextUiModel(R.string.color_plum), GetColorHexWithNameList.Colors.PlumBase),
            ColorHexWithName(TextUiModel(R.string.color_strawberry), GetColorHexWithNameList.Colors.StrawberryBase)
        ),
        onDismissRequest = { },
        onValueSelected = { }
    )
}
