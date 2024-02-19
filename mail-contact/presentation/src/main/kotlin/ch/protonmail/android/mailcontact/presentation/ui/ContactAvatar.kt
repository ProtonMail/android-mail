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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.subheadlineUnspecified

@Composable
fun InitialsContactAvatar(modifier: Modifier = Modifier, initials: String) {
    Box(
        modifier = modifier
            .sizeIn(
                minWidth = MailDimens.ContactAvatarSize,
                minHeight = MailDimens.ContactAvatarSize
            )
            .background(
                color = ProtonTheme.colors.backgroundSecondary,
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            )
            .clip(
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.subheadlineUnspecified,
            color = ProtonTheme.colors.textHint,
            text = initials
        )
    }
}

@Composable
fun IconContactAvatar(
    modifier: Modifier = Modifier,
    iconResId: Int,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .sizeIn(
                minWidth = MailDimens.ContactAvatarSize,
                minHeight = MailDimens.ContactAvatarSize
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            )
            .clip(
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            tint = Color.White,
            painter = painterResource(id = iconResId),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
    }
}

@Composable
fun ImageContactAvatar(modifier: Modifier = Modifier, imageBitmap: ImageBitmap) {
    Image(
        modifier = modifier
            .sizeIn(
                minWidth = MailDimens.ContactAvatarSize,
                minHeight = MailDimens.ContactAvatarSize
            )
            .clip(
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            )
            .background(
                color = ProtonTheme.colors.backgroundSecondary,
                shape = RoundedCornerShape(MailDimens.ContactAvatarCornerRadius)
            ),
        bitmap = imageBitmap,
        contentScale = ContentScale.Crop,
        contentDescription = stringResource(id = R.string.contact_avatar_image_content_description)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun InitialsContactAvatarPreview() {
    InitialsContactAvatar(
        initials = "JD"
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun IconContactAvatarPreview() {
    IconContactAvatar(
        iconResId = R.drawable.ic_proton_users,
        backgroundColor = Color.Blue
    )
}
