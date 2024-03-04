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

package ch.protonmail.android.mailcontact.data.remote

import ch.protonmail.android.mailcontact.data.remote.resource.LabelContactEmailsBody
import ch.protonmail.android.mailcontact.data.remote.resource.UnlabelContactEmailsBody
import ch.protonmail.android.mailcontact.data.remote.response.LabelContactEmailsResponse
import ch.protonmail.android.mailcontact.data.remote.response.UnlabelContactEmailsResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.PUT

interface ContactGroupApi : BaseRetrofitApi {

    @PUT("contacts/v4/contacts/emails/label")
    suspend fun labelContactEmails(@Body body: LabelContactEmailsBody): LabelContactEmailsResponse

    @PUT("contacts/v4/contacts/emails/unlabel")
    suspend fun unlabelContactEmails(@Body body: UnlabelContactEmailsBody): UnlabelContactEmailsResponse
}
