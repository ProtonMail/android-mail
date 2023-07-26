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

package ch.protonmail.android.composer.data.remote

import ch.protonmail.android.composer.data.remote.resource.CreateDraftBody
import ch.protonmail.android.composer.data.remote.resource.UpdateDraftBody
import ch.protonmail.android.mailmessage.data.remote.resource.MessageWithBodyResource
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DraftApi : BaseRetrofitApi {

    @POST("mail/v4/messages")
    suspend fun createDraft(@Body body: CreateDraftBody): MessageWithBodyResource

    @PUT("mail/v4/messages/{messageId}")
    suspend fun updateDraft(@Path("messageId") messageId: String, @Body body: UpdateDraftBody): MessageWithBodyResource
}
