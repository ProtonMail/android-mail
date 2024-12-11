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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CloseButtonBackground
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionColors.CloseButtonColor
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionDimens.CloseButtonSize

@Composable
internal fun PostSubscriptionCloseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CloseButtonSize)
                .background(
                    color = CloseButtonBackground,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                tint = CloseButtonColor,
                contentDescription = stringResource(R.string.post_subscription_close_button_content_description)
            )
        }
    }
}
