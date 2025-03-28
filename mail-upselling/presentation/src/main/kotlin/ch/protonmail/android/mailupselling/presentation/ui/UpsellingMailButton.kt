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

package ch.protonmail.android.mailupselling.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun UpsellingMailButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    viewModel: UpsellingButtonViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState()

    val isPromo = state.value.visibility == UpsellingVisibility.PROMO
    val onButtonClick = {
        onClick()
        viewModel.trackButtonInteraction(isPromo)
    }

    AnimatedVisibility(
        visible = state.value.isShown,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        if (isPromo) {
            UpsellingPromotionalMailButton(modifier = modifier, onButtonClick = onButtonClick)
        } else {
            UpsellingMailButton(modifier = modifier, onButtonClick = onButtonClick)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingMailButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)
    Surface(
        modifier = modifier.semantics { contentDescription = accessibilityDescription },
        color = Color.Transparent,
        onClick = onButtonClick,
        border = BorderStroke(MailDimens.DefaultBorder, ProtonTheme.colors.separatorNorm),
        shape = ProtonTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(ProtonDimens.SmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logo_mail_mono),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingPromotionalMailButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)
    Box {
        val iconColor = Color(0xFF8042FF)
        val bgColor = Color(0x4d8a6eff)
        Surface(
            modifier = modifier.semantics { contentDescription = accessibilityDescription },
            color = bgColor,
            onClick = onButtonClick,
            border = null,
            shape = ProtonTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .padding(ProtonDimens.SmallSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_mail_mono),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = iconColor
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = iconColor
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 4.dp)) {
            Box(
                modifier = Modifier.size(14.dp)
                    .background(color = ProtonTheme.colors.backgroundNorm, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.Center)
                    .background(color = ProtonTheme.colors.iconAccent, shape = CircleShape)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingMailButtonPreview() {
    ProtonTheme {
        UpsellingMailButton(onButtonClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingMailButtonPreview_Promo() {
    ProtonTheme {
        UpsellingPromotionalMailButton(onButtonClick = {})
    }
}
