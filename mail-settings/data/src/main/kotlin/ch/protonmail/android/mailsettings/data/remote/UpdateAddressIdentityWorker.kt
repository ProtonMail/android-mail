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
import androidx.work.workDataOf
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
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
    private val repository: AddressIdentityRepository,
    private val apiProvider: ApiProvider
) : CoroutineWorker(context, workerParameters) {

    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)?.takeIfNotBlank()
        val addressId = inputData.getString(RawAddressIdKey)?.takeIfNotBlank()?.let { AddressId(it) }

        if (userId == null || addressId == null) {
            return Result.failure()
        }

        val displayName = repository
            .getDisplayName(addressId)
            .getOrNull()
            ?: return Result.failure(
                workDataOf(KeyUpdateDataError to "Unable to fetch local display name.")
            )

        val signature = repository
            .getSignatureValue(addressId)
            .getOrNull()
            ?: return Result.failure(
                workDataOf(KeyUpdateDataError to "Unable to fetch local signature value.")
            )

        val updateRequest = UpdateAddressRequest(displayName = displayName.value, signature = signature.text)

        val result = apiProvider.get<AddressApi>(UserId(userId)).invoke {
            updateAddress(addressId.id, updateRequest)
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

        const val RawUserIdKey = "updateAddressIdentityWorkParamUserId"
        const val RawAddressIdKey = "updateAddressIdentityWorkParamAddressId"
        const val KeyUpdateDataError = "updateAddressIdentityDataError"

        fun params(userId: UserId, addressId: AddressId) = mapOf(
            RawUserIdKey to userId.id,
            RawAddressIdKey to addressId.id
        )

        fun id(addressId: AddressId): String = "UpdateAddressIdentityWorker-${addressId.id}"
    }
}
