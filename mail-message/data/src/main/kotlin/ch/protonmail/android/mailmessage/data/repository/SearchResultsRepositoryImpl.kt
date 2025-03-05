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

package ch.protonmail.android.mailmessage.data.repository

import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.local.SearchResultsLocalDataSource
import ch.protonmail.android.mailmessage.domain.repository.SearchResultsRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class SearchResultsRepositoryImpl @Inject constructor(
    private val localDataSource: SearchResultsLocalDataSource,
    private val messageLocalDataSource: MessageLocalDataSource
) : SearchResultsRepository {

    override suspend fun deleteAll(userId: UserId) {
        localDataSource.deleteAllResults(userId)
        messageLocalDataSource.deleteSearchIntervals(userId)
    }
}
