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

package ch.protonmail.android.mailcommon.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm

@Composable
fun AutoDeleteBanner(
    modifier: Modifier = Modifier,
    uiModel: AutoDeleteBannerUiModel,
    actions: AutoDeleteBannerActions
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
            )
            .clickable(enabled = uiModel is AutoDeleteBannerUiModel.Upgrade, onClick = actions.onActionClick)
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.medium
            )
            .background(
                color = ProtonTheme.colors.backgroundNorm,
                shape = ProtonTheme.shapes.medium
            )
            .padding(ProtonDimens.DefaultSpacing)
    ) {

        val mainText = when (uiModel) {
            is AutoDeleteBannerUiModel.Info -> R.string.auto_delete_banner_text_enabled_info
            is AutoDeleteBannerUiModel.Upgrade -> R.string.auto_delete_banner_text_upgrade
            is AutoDeleteBannerUiModel.Activate.Spam -> R.string.auto_delete_banner_text_activate_spam
            is AutoDeleteBannerUiModel.Activate.Trash -> R.string.auto_delete_banner_text_activate_trash
        }

        Row {
            when (uiModel) {
                is AutoDeleteBannerUiModel.Upgrade, is AutoDeleteBannerUiModel.Activate -> Icon(
                    painter = painterResource(id = R.drawable.ic_m_plus_rainbow),
                    contentDescription = null,
                    tint = Color.Unspecified
                )

                else -> Icon(
                    painter = painterResource(id = R.drawable.ic_proton_trash_clock),
                    contentDescription = null,
                    tint = ProtonTheme.colors.iconWeak
                )
            }
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            Column {
                Text(
                    text = stringResource(mainText),
                    style = ProtonTheme.typography.defaultSmallNorm
                )
                if (uiModel is AutoDeleteBannerUiModel.Upgrade) {
                    Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
                    Text(
                        text = stringResource(R.string.auto_delete_banner_action_upgrade_learn_more),
                        color = ProtonTheme.colors.textAccent
                    )
                }
            }
        }

        if (uiModel is AutoDeleteBannerUiModel.Activate) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ProtonDimens.SmallSpacing),
                horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
            ) {
                ProtonTextButton(
                    modifier = Modifier.weight(1f),
                    onClick = actions.onConfirmClick,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ProtonTheme.colors.brandNorm
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.auto_delete_banner_button_activate_confirm),
                        color = ProtonTheme.colors.textInverted
                    )
                }
                ProtonTextButton(
                    modifier = Modifier.weight(1f),
                    onClick = actions.onDismissClick,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ProtonTheme.colors.backgroundSecondary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.auto_delete_banner_button_activate_dismiss),
                        color = ProtonTheme.colors.textNorm
                    )
                }
            }
        }
    }
}

sealed interface AutoDeleteBannerUiModel {

    data object Upgrade : AutoDeleteBannerUiModel
    data object Info : AutoDeleteBannerUiModel

    sealed interface Activate : AutoDeleteBannerUiModel {
        data object Trash : Activate
        data object Spam : Activate
    }
}

data class AutoDeleteBannerActions(
    val onActionClick: () -> Unit,
    val onConfirmClick: () -> Unit,
    val onDismissClick: () -> Unit
) {

    companion object {

        val Empty = AutoDeleteBannerActions(
            {},
            {},
            {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutoDeleteBannerPreviewUpgrade() {
    ProtonTheme {
        AutoDeleteBanner(
            modifier = Modifier,
            uiModel = AutoDeleteBannerUiModel.Upgrade,
            actions = AutoDeleteBannerActions.Empty
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutoDeleteBannerPreviewActivateSpam() {
    ProtonTheme {
        AutoDeleteBanner(
            modifier = Modifier,
            uiModel = AutoDeleteBannerUiModel.Activate.Spam,
            actions = AutoDeleteBannerActions.Empty
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutoDeleteBannerPreviewActivateTrash() {
    ProtonTheme {
        AutoDeleteBanner(
            modifier = Modifier,
            uiModel = AutoDeleteBannerUiModel.Activate.Trash,
            actions = AutoDeleteBannerActions.Empty
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutoDeleteBannerPreviewInfo() {
    ProtonTheme {
        AutoDeleteBanner(
            modifier = Modifier,
            uiModel = AutoDeleteBannerUiModel.Info,
            actions = AutoDeleteBannerActions.Empty
        )
    }
}
