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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentFoundItemUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import kotlinx.collections.immutable.persistentListOf

/**
 * The idle page shown when the user has search history: recent queries, then previously found items.
 */
@Composable
internal fun RecentSearchesPage(
    state: RecentSearchesState.Data,
    avatarImages: AvatarImagesUiModel,
    actions: RecentSearchesActions,
    downloadingAttachmentId: AttachmentIdUiModel?,
    modifier: Modifier = Modifier
) {
    // When the keyboard is open its inset already covers the nav bar, so take whichever is larger.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier,
        // Nothing floats over this page — the history rows aren't selectable — so a trailing gap is all
        // it needs beyond the keyboard / navigation bar.
        contentPadding = PaddingValues(bottom = maxOf(navInset, imeInset) + ProtonDimens.Spacing.Large)
    ) {
        item(key = "recent-terms") {
            RecentSearchTermsSection(terms = state.terms, actions = actions)
        }
        item(key = "recent-found-items") {
            RecentFoundItemsSection(
                items = state.foundItems,
                avatarImages = avatarImages,
                actions = actions,
                downloadingAttachmentId = downloadingAttachmentId
            )
        }
    }
}

data class RecentSearchesActions(
    val onTermClicked: (String) -> Unit,
    val onTermDismissed: (String) -> Unit,
    val onClearTermsClicked: () -> Unit,
    val onFoundItemClicked: (RecentFoundItemUiModel) -> Unit,
    val onFoundItemStarClicked: (RecentFoundItemUiModel) -> Unit,
    // Sender images are owned by the search view model, since one global store feeds both lists.
    val onAvatarImageLoadRequested: (MailboxItemUiModel) -> Unit,
    val onAvatarImageLoadFailed: (MailboxItemUiModel) -> Unit,
    // Attachment pills open through the search view model too, so a download started on a history row
    // and one started on a result row can't run at the same time.
    val onAttachmentClicked: (AttachmentIdUiModel) -> Unit
) {

    companion object {

        val Empty = RecentSearchesActions(
            onTermClicked = {},
            onTermDismissed = {},
            onClearTermsClicked = {},
            onFoundItemClicked = {},
            onFoundItemStarClicked = {},
            onAvatarImageLoadRequested = {},
            onAvatarImageLoadFailed = {},
            onAttachmentClicked = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RecentSearchesPagePreview() {
    ProtonTheme {
        RecentSearchesPage(
            state = RecentSearchesState.Data(
                terms = persistentListOf("invoice", "flight", "from:victoria"),
                foundItems = persistentListOf(
                    RecentFoundItemUiModel(
                        item = MailboxItemUiModelPreviewData.Message.WeatherForecastAug,
                        searchQuery = "weather"
                    )
                )
            ),
            avatarImages = AvatarImagesUiModel.Empty,
            actions = RecentSearchesActions.Empty,
            downloadingAttachmentId = null
        )
    }
}
