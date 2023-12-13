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

package ch.protonmail.android.mailconversation.data.remote

import ch.protonmail.android.mailconversation.data.remote.resource.UnreadConversationCountResource
import ch.protonmail.android.mailconversation.data.remote.response.UnreadConversationsCountsResponse
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import javax.inject.Inject

class UnreadConversationsCountRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider
) : UnreadConversationsCountRemoteDataSource {

    override suspend fun getConversationCounters(userId: UserId): List<UnreadConversationCountResource> {
        val result = apiProvider.get<UnreadConversationsCountersApi>(userId).invoke {
            getConversationCounters()
        }
        return countResourcesOrEmptyList(result)
    }

    private fun countResourcesOrEmptyList(
        result: ApiResult<UnreadConversationsCountsResponse>
    ): List<UnreadConversationCountResource> = when (result) {
        is ApiResult.Success -> result.value.counts
        is ApiResult.Error -> emptyList()
    }

}
