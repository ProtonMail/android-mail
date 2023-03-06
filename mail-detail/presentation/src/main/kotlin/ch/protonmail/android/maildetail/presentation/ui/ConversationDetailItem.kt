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

package ch.protonmail.android.maildetail.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import kotlinx.coroutines.flow.collectLatest
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun ConversationDetailItem(
    uiModel: ConversationDetailMessageUiModel,
    listState: LazyListState,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = ProtonTheme.colors.backgroundNorm
        )
    ) {
        when (uiModel) {
            is Collapsed -> {
                ConversationDetailCollapsedMessageHeader(
                    uiModel = uiModel,
                    modifier = Modifier.clickable {
                        actions.onExpand(uiModel.messageId)
                    }
                )
            }

            is Expanded -> {
                ConversationDetailExpandedItem(
                    uiModel = uiModel,
                    messageId = uiModel.messageId,
                    listState = listState,
                    actions = actions
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailExpandedItem(
    messageId: MessageId,
    uiModel: Expanded,
    listState: LazyListState,
    actions: ConversationDetailItem.Actions,
    modifier: Modifier = Modifier,
) {
    var bodyBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    LaunchedEffect(key1 = bodyBounds) {
        snapshotFlow { listState }
            .collectLatest {
                if (!it.isScrollInProgress) {
                    val viewportHeight = it.layoutInfo.viewportEndOffset + it.layoutInfo.viewportStartOffset
                    val isFullyVisible = bodyBounds.bottom <= viewportHeight
                    if (!isFullyVisible) {
                        actions.onRequestScrollTo(messageId)
                    }
                }
            }
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .clickable { actions.onCollapse(messageId) }
                .fillMaxWidth()
                .height(16.dp)
        )
        MessageDetailHeader(uiModel = uiModel.messageDetailHeaderUiModel)
        MessageBody(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                bodyBounds = coordinates.boundsInParent()
            },
            messageBodyUiModel = uiModel.messageBodyUiModel,
            onMessageBodyLinkClicked = { actions.onMessageBodyLinkClicked(it.toString()) }
        )
    }
}

object ConversationDetailItem {
    data class Actions(
        val onCollapse: (MessageId) -> Unit,
        val onExpand: (MessageId) -> Unit,
        val onMessageBodyLinkClicked: (url: String) -> Unit,
        val onOpenMessageBodyLink: (url: String) -> Unit,
        val onRequestScrollTo: (MessageId) -> Unit
    )
}
