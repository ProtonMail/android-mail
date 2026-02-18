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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodySmallNorm
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.ui.chips.item.ChipItem
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.ActionGroup
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.ActionGroupItem
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoUiModelSample
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import kotlinx.collections.immutable.persistentListOf

@Composable
@Suppress("UseComposableActions")
internal fun RecipientChipActionsBottomSheetContent(
    chipItem: ChipItem,
    onCopy: (ChipItem) -> Unit,
    onRemove: (ChipItem) -> Unit,
    onEncryptionInfoClicked: (EncryptionInfoUiModel.WithLock) -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundInvertedNorm)
            .padding(horizontal = ProtonDimens.Spacing.Large)
            .padding(top = ProtonDimens.Spacing.Standard)
            .padding(bottom = ProtonDimens.Spacing.Large)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .scrollable(
                    rememberScrollableState(consumeScrollDelta = { 0f }),
                    orientation = Orientation.Vertical
                )
        ) {
            Text(
                modifier = Modifier
                    .background(ProtonTheme.colors.backgroundInvertedNorm)
                    .align(Alignment.CenterHorizontally),
                text = chipItem.value,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ProtonTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

            val actions = if (chipItem is ChipItem.Group) {
                persistentListOf(Actions.Remove)
            } else {
                persistentListOf(Actions.Copy, Actions.Remove)
            }

            ActionGroup(
                modifier = Modifier,
                items = actions,
                onItemClicked = { source ->
                    when (source) {
                        Actions.Copy -> onCopy(chipItem)
                        Actions.Remove -> onRemove(chipItem)
                    }
                }
            ) { action, onClick ->
                ActionGroupItem(
                    modifier = Modifier,
                    icon = action.icon,
                    description = stringResource(action.description),
                    contentDescription = stringResource(action.description),
                    onClick = onClick
                )
            }
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            val encryptionInfo = chipItem.encryptionInfo
            if (encryptionInfo is EncryptionInfoUiModel.WithLock) {
                ActionGroup(
                    modifier = Modifier,
                    items = persistentListOf(encryptionInfo),
                    onItemClicked = { source ->
                        onEncryptionInfoClicked(source)
                    }
                ) { item, onClick ->
                    ActionGroupItem(
                        modifier = Modifier,
                        icon = R.drawable.ic_proton_info_circle,
                        description = stringResource(item.title),
                        contentDescription = stringResource(item.title),
                        onClick = onClick,
                        secondaryContent = {
                            Text(
                                text = stringResource(R.string.composer_encryption_info_learn_more),
                                style = ProtonTheme.typography.bodySmallNorm,
                                color = ProtonTheme.colors.interactionBrandDefaultNorm
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            }

            BottomNavigationBarSpacer()
        }
    }
}

private enum class Actions(@DrawableRes val icon: Int, @StringRes val description: Int) {
    Copy(R.drawable.ic_proton_squares, R.string.composer_copy_recipient_action_item),
    Remove(R.drawable.ic_proton_trash, R.string.composer_remove_recipient_action_item)
}

@Preview(showBackground = true)
@Composable
private fun PreviewInlineActions() {
    ProtonTheme {
        RecipientChipActionsBottomSheetContent(
            chipItem = ChipItem.Valid("test@proton.me", EncryptionInfoUiModelSample.StoredWithZeroAccessEncryption),
            onCopy = {},
            onRemove = {},
            onEncryptionInfoClicked = {}
        )
    }
}
