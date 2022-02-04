/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data.repository

import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.repository.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.repository.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val remoteDataSource: MessageRemoteDataSource,
    private val localDataSource: MessageLocalDataSource,
) : MessageRepository {

    override suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey,
    ): List<Message> = localDataSource.getMessages(
        userId = userId,
        pageKey = pageKey
    ).let { messages ->
        if (localDataSource.isLocalPageValid(userId, pageKey, messages)) messages
        else runCatching { fetchMessages(userId, pageKey) }.getOrElse { messages }
    }

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId,
    ) = localDataSource.markAsStale(userId, labelId)

    private suspend fun fetchMessages(
        userId: UserId,
        pageKey: PageKey,
    ) = localDataSource.getClippedPageKey(
        userId = userId,
        pageKey = pageKey.copy(size = min(MessageApi.maxPageSize, pageKey.size))
    ).let { adaptedPageKey ->
        remoteDataSource.getMessages(
            userId = userId,
            pageKey = adaptedPageKey
        ).also { messages -> insertMessages(userId, adaptedPageKey, messages) }
    }

    private suspend fun insertMessages(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>,
    ) = localDataSource.upsertMessages(
        userId = userId,
        pageKey = pageKey,
        messages = messages
    )
}
