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

package ch.protonmail.android.mailupselling.presentation.ui.drivespotlight

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.compose.theme.headlineSmallNorm

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UseComposableActions")
@Composable
internal fun DriveSpotlightContent(
    copy: TextUiModel,
    onDismiss: () -> Unit,
    onDisplayed: () -> Unit,
    onCTAClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp <= MailDimens.NarrowScreenWidth.value
    LaunchedEffect(Unit) {
        onDisplayed()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth,
                painter = painterResource(id = R.drawable.drive_spotlight_bg),
                contentDescription = NO_CONTENT_DESCRIPTION
            )
            IconButton(
                modifier = Modifier
                    .padding(ProtonDimens.ExtraSmallSpacing)
                    .align(alignment = Alignment.TopStart)
                    .zIndex(1f),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = UpsellingLayoutValues.closeButtonColor,
                    contentDescription = stringResource(R.string.upselling_close_button_content_description),
                    modifier = Modifier
                        .size(UpsellingLayoutValues.closeButtonSize)
                        .padding(ProtonDimens.ExtraSmallSpacing)
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .padding(top = ProtonDimens.MediumSpacing),
            text = stringResource(R.string.drive_spotlight_title),
            style = if (isNarrowScreen) {
                ProtonTheme.typography.headlineSmallNorm
            } else ProtonTheme.typography.headlineNorm,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .padding(top = ProtonDimens.DefaultSpacing),
            text = copy.string(),
            style = ProtonTheme.typography.body2Regular,
            color = ProtonTheme.colors.textNorm,
            textAlign = TextAlign.Center
        )

        ProtonButton(
            onClick = {
                onCTAClicked()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MinHeight)
                .padding(ProtonDimens.DefaultSpacing),
            shape = ProtonTheme.shapes.medium,
            border = null,
            elevation = null,
            colors = ButtonDefaults.protonButtonColors(),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Text(
                text = stringResource(R.string.drive_spotlight_cta),
                color = Color.White
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun DriveSpotlightContentPreview() {
    ProtonTheme3 {
        DriveSpotlightContent(
            copy = TextUiModel.Text(
                "Securely share photos with family and friends with albums in Proton Drive. " +
                    "Included free with your plan."
            ),
            onDismiss = {},
            onDisplayed = {},
            onCTAClicked = {}
        )
    }
}
