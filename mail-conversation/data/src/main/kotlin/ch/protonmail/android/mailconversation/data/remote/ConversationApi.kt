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

import ch.protonmail.android.mailconversation.data.remote.response.GetConversationResponse
import ch.protonmail.android.mailconversation.data.remote.response.GetConversationsResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ConversationApi : BaseRetrofitApi {

    @GET("mail/v4/conversations")
    suspend fun getConversations(
        @Query("Page") page: Int = 0,
        @Query("PageSize") pageSize: Int = 50,
        @Query("Limit") limit: Int = pageSize,
        @Query("LabelID") labelIds: List<String> = emptyList(),
        @Query("Sort") sort: String = "Time",
        /* 0:ASC, 1:DESC */
        @Query("Desc") desc: Int = 1,
        @Query("Begin") beginTime: Long? = null,
        @Query("BeginID") beginId: String? = null,
        @Query("End") endTime: Long? = null,
        @Query("EndID") endId: String? = null,
        /* Keyword search of To, CC, BCC, From, Subject */
        @Query("Keyword") keyword: String? = null,
        @Query("Unread") unread: Int? = null,
    ): GetConversationsResponse

    @GET("mail/v4/conversations/{conversationId}")
    suspend fun getConversation(
        @Path("conversationId") conversationId: String,
    ): GetConversationResponse

    companion object {
        const val maxPageSize = 150
    }
}
