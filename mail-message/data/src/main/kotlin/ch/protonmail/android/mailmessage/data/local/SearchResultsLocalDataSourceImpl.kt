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

package ch.protonmail.android.mailmessage.data.local

import ch.protonmail.android.mailmessage.data.local.entity.SearchResultEntity
import ch.protonmail.android.mailmessage.domain.model.Message
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class SearchResultsLocalDataSourceImpl @Inject constructor(private val db: SearchResultsDatabase) :
    SearchResultsLocalDataSource {

    private val searchResultDao = db.searchResultsDao()

    override suspend fun upsertResults(
        userId: UserId,
        keyword: String,
        messages: List<Message>
    ) = db.inTransaction {
        searchResultDao.insertOrUpdate(
            *messages.map {
                SearchResultEntity(userId, keyword, it.messageId)
            }.toTypedArray()
        )
    }

    override suspend fun deleteResults(userId: UserId, keyword: String) = searchResultDao.deleteAll(userId, keyword)
}
