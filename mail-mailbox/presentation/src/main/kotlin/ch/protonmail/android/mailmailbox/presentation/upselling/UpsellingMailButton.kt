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

package ch.protonmail.android.mailmailbox.presentation.upselling

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.presentation.R
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun UpsellingMailButton(modifier: Modifier = Modifier, viewModel: UpsellingButtonViewModel = hiltViewModel()) {
    val state = rememberAsState(flow = viewModel.state, initial = UpsellingButtonViewModel.initialState)
    AnimatedVisibility(
        visible = state.value.isShown,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        Surface(
            modifier = modifier,
            color = Color.Transparent,
            onClick = {},
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
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingMailButtonPreview() {
    ProtonTheme {
        UpsellingMailButton(viewModel = UpsellingButtonViewModel())
    }
}
