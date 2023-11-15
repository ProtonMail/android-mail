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

package ch.protonmail.android.mailsettings.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import me.proton.core.user.data.api.AddressApi
import me.proton.core.user.data.api.request.UpdateAddressRequest
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.takeIfNotBlank

@HiltWorker
internal class UpdateAddressIdentityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
        val addressId = inputData.getString(RawAddressIdKey)?.takeIfNotBlank()
        val displayName = inputData.getString(RawDisplayNameKey) ?: ""
        val signature = inputData.getString(RawSignatureKey) ?: ""

        if (userId == null || addressId == null) {
            return Result.failure()
        }

        val updateRequest = UpdateAddressRequest(displayName = displayName, signature = signature)

        val result = apiProvider.get<AddressApi>(UserId(userId)).invoke {
            updateAddress(addressId, updateRequest)
        }

        return when (result) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> {
                if (result.isRetryable()) Result.retry()
                else Result.failure()
            }
        }
    }

    companion object {

        private const val RawUserIdKey = "updateAddressIdentityWorkParamUserId"
        private const val RawAddressIdKey = "updateAddressIdentityWorkParamAddressId"
        private const val RawDisplayNameKey = "updateAddressIdentityWorkParamDisplayNameValue"
        private const val RawSignatureKey = "updateAddressIdentityWorkParamSignatureValue"

        fun params(
            userId: UserId,
            addressId: AddressId,
            displayName: String,
            signature: String
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawAddressIdKey to addressId.id,
            RawDisplayNameKey to displayName,
            RawSignatureKey to signature
        )

        fun id(addressId: AddressId): String = "UpdateAddressIdentityWorker-${addressId.id}"
    }
}
