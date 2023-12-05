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

package ch.protonmail.android.mailmailbox.data.remote

import ch.protonmail.android.mailmailbox.data.remote.response.UnreadCountResource
import ch.protonmail.android.mailmailbox.data.remote.response.UnreadCountsResponse
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import javax.inject.Inject

class UnreadCountRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider
) : UnreadCountRemoteDataSource {

    override suspend fun getMessageCounters(userId: UserId): List<UnreadCountResource> {
        val result = apiProvider.get<UnreadCountersApi>(userId).invoke {
            getMessageCounters()
        }
        return countResourcesOrEmptyList(result)
    }

    override suspend fun getConversationCounters(userId: UserId): List<UnreadCountResource> {
        val result = apiProvider.get<UnreadCountersApi>(userId).invoke {
            getConversationCounters()
        }
        return countResourcesOrEmptyList(result)
    }

    private fun countResourcesOrEmptyList(result: ApiResult<UnreadCountsResponse>): List<UnreadCountResource> =
        when (result) {
            is ApiResult.Success -> result.value.counts
            is ApiResult.Error -> emptyList()
        }

}
