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

package ch.protonmail.android.mailmailbox.data.repository

import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.maillabel.data.local.LabelDataSource
import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountersRepository
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class UnreadCountersRepositoryImpl @Inject constructor(
    private val labelDataSource: LabelDataSource
) : UnreadCountersRepository {

    @MissingRustApi
    override fun observeUnreadCounters(userId: UserId): Flow<List<UnreadCounter>> = combine(
        observeSystemLabelsUnreadCounts(userId),
        observeCustomLabelsUnreadCounts(userId),
        observeCustomFoldersUnreadCounts(userId)
    ) { system, labels, folders ->
        (system + labels + folders).map { labelIdWithCount: LabelIdWithCount ->
            UnreadCounter(labelIdWithCount.labelId, labelIdWithCount.count)
        }

    }

    private fun observeCustomFoldersUnreadCounts(userId: UserId) = labelDataSource.observeMessageFolders(userId)
        .mapLatest { messageFolders ->
            messageFolders.map { folder ->
                LabelIdWithCount(folder.id.toLabelId(), folder.unread.toInt())
            }
        }

    private fun observeCustomLabelsUnreadCounts(userId: UserId) = labelDataSource.observeMessageLabels(userId)
        .mapLatest { messageLabels ->
            messageLabels.map { label ->
                LabelIdWithCount(label.id.toLabelId(), label.unread.toInt())
            }
        }

    private fun observeSystemLabelsUnreadCounts(userId: UserId) = labelDataSource.observeSystemLabels(userId)
        .mapLatest { systemLabels ->
            systemLabels.map { system ->
                LabelIdWithCount(system.id.toLabelId(), system.count.toInt())
            }
        }

    private data class LabelIdWithCount(
        val labelId: LabelId,
        val count: Int
    )

}
