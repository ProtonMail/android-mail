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

package ch.protonmail.android.mailbyoe.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.design.compose.theme.titleMediumNorm

/**
 * The design system's prompt (Prompt V5.1): a [title], a [message], and a primary and a secondary action stacked at
 * full width. Dismissing it, by back or a tap outside, runs [onDismissRequest], which is the secondary action unless
 * given.
 *
 * It always uses the normal theme, so it stays readable over the inverted screens of the Gmail connection.
 */
@Composable
internal fun ByoePromptDialog(
    title: String,
    message: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    secondaryAction: String,
    onSecondaryAction: () -> Unit,
    onDismissRequest: () -> Unit = onSecondaryAction
) {
    ProtonTheme {
        Dialog(onDismissRequest = onDismissRequest) {
            val shape = RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge)
            Column(
                modifier = Modifier
                    .width(PromptWidth)
                    .shadow(ProtonDimens.ShadowElevation.Lifted, shape)
                    .background(ProtonTheme.colors.backgroundNorm, shape)
                    .padding(ProtonDimens.Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large)
            ) {
                Text(
                    text = title,
                    style = ProtonTheme.typography.titleMediumNorm.copy(fontWeight = FontWeight.Bold)
                )
                Text(text = message, style = ProtonTheme.typography.bodyMediumNorm)
                Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
                    ProtonSolidButton(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(ProtonDimens.CornerRadius.Large),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PromptButtonHeight)
                    ) {
                        Text(text = primaryAction, style = ProtonTheme.typography.bodyMedium)
                    }
                    ProtonButton(
                        onClick = onSecondaryAction,
                        elevation = null,
                        shape = RoundedCornerShape(ProtonDimens.CornerRadius.Large),
                        border = BorderStroke(ProtonDimens.BorderSize.Default, ProtonTheme.colors.borderNorm),
                        colors = ButtonDefaults.protonButtonColors(
                            backgroundColor = ProtonTheme.colors.backgroundNorm,
                            contentColor = ProtonTheme.colors.textNorm
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PromptButtonHeight)
                    ) {
                        Text(text = secondaryAction, style = ProtonTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val PromptWidth = 288.dp
private val PromptButtonHeight = 36.dp

@Preview
@Composable
private fun ByoePromptDialogPreview() {
    ByoePromptDialog(
        title = "Connect your Gmail",
        message = "Your Gmail account couldn't be connected. Please try again.",
        primaryAction = "Try again",
        onPrimaryAction = {},
        secondaryAction = "Cancel",
        onSecondaryAction = {}
    )
}
