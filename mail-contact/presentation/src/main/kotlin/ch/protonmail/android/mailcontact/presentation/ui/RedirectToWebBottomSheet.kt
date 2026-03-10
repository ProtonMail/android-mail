/*
 * Copyright (c) 2025 Proton Technologies AG
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

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumInverted
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.design.compose.theme.titleMediumNorm
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcontact.presentation.R

@Composable
fun RedirectToWebBottomSheetContent(
    @StringRes description: Int,
    @StringRes buttonText: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(ProtonDimens.Spacing.ExtraLarge)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.illustration_contacts),
            contentDescription = NO_CONTENT_DESCRIPTION
        )

        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Standard))

        Text(
            text = stringResource(id = R.string.contact_bottom_sheet_redirect_to_web_title),
            style = ProtonTheme.typography.titleMediumNorm,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Compact))

        Text(
            text = stringResource(id = description),
            style = ProtonTheme.typography.bodyMediumWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Jumbo))

        ProtonButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConfirm,
            elevation = null,
            shape = ProtonTheme.shapes.massive,
            border = null,
            colors = ButtonDefaults.protonButtonColors()
        ) {
            Text(
                text = stringResource(id = buttonText),
                style = ProtonTheme.typography.bodyMediumInverted
            )
        }

        ProtonButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss,
            elevation = null,
            shape = ProtonTheme.shapes.massive,
            border = null,
            colors = ButtonDefaults.protonButtonColors().copy(
                containerColor = ProtonTheme.colors.backgroundInvertedNorm,
                contentColor = ProtonTheme.colors.textAccent
            )
        ) {
            Text(
                text = stringResource(id = R.string.contact_bottom_sheet_redirect_to_web_dismiss_button),
                style = ProtonTheme.typography.bodyMedium.copy(color = ProtonTheme.colors.textAccent)
            )
        }
    }
}

@Preview
@Composable
fun RedirectToWebBottomSheetContentPreview() {
    RedirectToWebBottomSheetContent(
        description = R.string.add_contact_bottom_sheet_redirect_to_web_descrption,
        buttonText = R.string.contact_bottom_sheet_redirect_to_web_button,
        onConfirm = {},
        onDismiss = {}
    )
}
