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

package ch.protonmail.android.mailupselling.presentation.ui.postsubscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CloseButtonBackground
import me.proton.core.compose.theme.ProtonDimens

@Composable
internal fun PostSubscriptionCloseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(ProtonDimens.SmallSpacing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    radius = MailDimens.PostSubscriptionCloseButtonRippleRadius,
                    color = Color.White
                ),
                role = Role.Button,
                onClick = onClick
            )
            .padding(ProtonDimens.SmallSpacing)
            .background(color = CloseButtonBackground, shape = CircleShape)
            .padding(ProtonDimens.SmallSpacing)
    ) {
        Icon(
            modifier = Modifier.size(ProtonDimens.SmallIconSize),
            painter = painterResource(id = R.drawable.ic_proton_cross_big),
            contentDescription = stringResource(id = R.string.post_subscription_close_button_content_description),
            tint = Color.White
        )
    }
}
