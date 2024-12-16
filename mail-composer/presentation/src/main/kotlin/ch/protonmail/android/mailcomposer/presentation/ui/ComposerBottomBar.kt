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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ComposerBottomBar(
    draftId: MessageId,
    senderEmail: SenderEmail,
    isMessagePasswordSet: Boolean,
    isMessageExpirationTimeSet: Boolean,
    onSetMessagePasswordClick: (MessageId, SenderEmail) -> Unit,
    onSetExpirationTimeClick: () -> Unit,
    enableInteractions: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm, thickness = MailDimens.SeparatorHeight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.ExtraLargeSpacing)
                .padding(horizontal = ProtonDimens.ExtraSmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddPasswordButton(draftId, senderEmail, isMessagePasswordSet, enableInteractions, onSetMessagePasswordClick)
            SetExpirationButton(isMessageExpirationTimeSet, enableInteractions, onSetExpirationTimeClick)
        }
    }
}

@Composable
private fun AddPasswordButton(
    draftId: MessageId,
    senderEmail: SenderEmail,
    isMessagePasswordSet: Boolean,
    isEnabled: Boolean,
    onSetMessagePasswordClick: (MessageId, SenderEmail) -> Unit
) {
    BottomBarButton(
        iconRes = R.drawable.ic_proton_lock,
        contentDescriptionRes = R.string.composer_button_add_password,
        shouldShowCheckmark = isMessagePasswordSet,
        onClick = { onSetMessagePasswordClick(draftId, senderEmail) },
        isEnabled = isEnabled
    )
}

@Composable
private fun SetExpirationButton(
    isMessageExpirationTimeSet: Boolean,
    isEnabled: Boolean,
    onSetExpirationTimeClick: () -> Unit
) {
    BottomBarButton(
        iconRes = R.drawable.ic_proton_hourglass,
        contentDescriptionRes = R.string.composer_button_set_expiration,
        shouldShowCheckmark = isMessageExpirationTimeSet,
        onClick = onSetExpirationTimeClick,
        isEnabled = isEnabled
    )
}

@Composable
private fun BottomBarButton(
    @DrawableRes iconRes: Int,
    @StringRes contentDescriptionRes: Int,
    shouldShowCheckmark: Boolean,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    Box {
        EnabledStateIconButton(
            icon = painterResource(id = iconRes),
            isEnabled = isEnabled,
            contentDescription = stringResource(id = contentDescriptionRes),
            onClick = onClick
        )

        if (shouldShowCheckmark) {
            Box(
                modifier = Modifier
                    .size(MailDimens.ExtraLargeSpacing)
                    .padding(bottom = ProtonDimens.SmallSpacing, end = ProtonDimens.ExtraSmallSpacing),
                contentAlignment = Alignment.BottomEnd
            ) {
                BottomBarButtonCheckmark()
            }
        }
    }
}

@Composable
private fun BottomBarButtonCheckmark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ProtonDimens.SmallIconSize)
            .background(ProtonTheme.colors.interactionNorm, CircleShape)
            .border(Dp.Hairline, ProtonTheme.colors.backgroundNorm, CircleShape)
            .padding(ProtonDimens.ExtraSmallSpacing),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_checkmark),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconInverted
        )
    }
}
