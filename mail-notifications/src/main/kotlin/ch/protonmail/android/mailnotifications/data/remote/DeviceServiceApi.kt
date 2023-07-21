/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
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
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.mailnotifications.data.remote

import ch.protonmail.android.mailnotifications.data.remote.resource.RegisterDeviceRequest
import ch.protonmail.android.mailnotifications.data.remote.resource.RegisterDeviceResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeviceServiceApi : BaseRetrofitApi {

    @POST("devices")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun registerDevice(@Body deviceRequest: RegisterDeviceRequest): RegisterDeviceResponse

    companion object {

        const val ACCEPT_HEADER_V1 = "Accept: application/vnd.protonmail.v1+json"
        const val CONTENT_TYPE = "Content-Type: application/json;charset=utf-8"
    }
}
