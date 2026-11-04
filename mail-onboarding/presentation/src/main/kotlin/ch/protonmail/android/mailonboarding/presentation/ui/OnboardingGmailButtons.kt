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

package ch.protonmail.android.mailonboarding.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailonboarding.presentation.R

/** The Gmail page's actions, in place of the single button the other pages have. */
@Composable
internal fun OnboardingGmailButtons(onExplore: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier.padding(ProtonDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Compact)
    ) {
        ProtonSolidButton(
            modifier = Modifier
                .height(ButtonHeight)
                .fillMaxWidth(),
            onClick = onExplore,
            shape = ProtonTheme.shapes.huge
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_gmail_help_me_move),
                style = buttonTextStyle(),
                color = ProtonTheme.colors.textInverted
            )
        }
        ProtonButton(
            modifier = Modifier
                .height(ButtonHeight)
                .fillMaxWidth(),
            onClick = onLater,
            shape = ProtonTheme.shapes.huge,
            border = null,
            elevation = null,
            colors = ButtonDefaults.protonButtonColors(
                backgroundColor = if (ProtonTheme.colors.isDark) {
                    ProtonTheme.colors.shade20
                } else {
                    LightSecondaryBackground
                },
                contentColor = ProtonTheme.colors.textNorm
            )
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_gmail_no_thanks),
                style = buttonTextStyle()
            )
        }
    }
}

@Composable
private fun buttonTextStyle() = ProtonTheme.typography.titleSmall.copy(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.SemiBold
)

private val ButtonHeight = 44.dp

// Deep Cove at 10% from the design; it has no design system token.
private val LightSecondaryBackground = Color(0x1A181039)
