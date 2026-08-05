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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentFoundItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.ComposeMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * "Previously found": the search results the user opened before, rendered with the same row as the
 * mailbox and the live results list so they carry avatars, read state and the star affordance.
 */
@Composable
internal fun RecentFoundItemsSection(
    items: ImmutableList<RecentFoundItemUiModel>,
    avatarImages: AvatarImagesUiModel,
    actions: RecentSearchesActions,
    downloadingAttachmentId: AttachmentIdUiModel?,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    // MailboxItem's callbacks take the mailbox model, so map back to the recents row that owns it.
    val byId = remember(items) { items.associateBy { it.item.id } }
    val itemActions = remember(byId, actions) {
        ComposeMailboxItem.Actions.Empty.copy(
            onItemClicked = { item -> byId[item.id]?.let(actions.onFoundItemClicked) },
            onStarClicked = { item -> byId[item.id]?.let(actions.onFoundItemStarClicked) },
            onAvatarImageLoadRequested = actions.onAvatarImageLoadRequested,
            onAvatarImageLoadFailed = actions.onAvatarImageLoadFailed,
            onAttachmentClicked = actions.onAttachmentClicked
        )
    }

    Column(modifier = modifier) {
        SectionHeader(text = stringResource(R.string.content_search_previously_found_title))
        items.forEach { found ->
            MailboxItem(
                item = found.item,
                actions = itemActions,
                avatarImageUiModel = avatarImages.stateFor(found.item),
                accessibilitySwipeActions = persistentListOf(),
                isSelectable = false,
                downloadingAttachmentId = downloadingAttachmentId
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RecentFoundItemsSectionPreview() {
    ProtonTheme {
        RecentFoundItemsSection(
            items = persistentListOf(
                RecentFoundItemUiModel(
                    item = MailboxItemUiModelPreviewData.Message.WeatherForecastAug,
                    searchQuery = "weather"
                ),
                RecentFoundItemUiModel(
                    item = MailboxItemUiModelPreviewData.Message.WeatherForecastSep,
                    searchQuery = "weather"
                )
            ),
            avatarImages = AvatarImagesUiModel.Empty,
            actions = RecentSearchesActions.Empty,
            downloadingAttachmentId = null
        )
    }
}
