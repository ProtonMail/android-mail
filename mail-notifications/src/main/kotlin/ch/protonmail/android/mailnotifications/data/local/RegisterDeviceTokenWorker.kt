/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnotifications.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailnotifications.data.repository.DeviceRegistrationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
internal class RegisterDeviceTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val deviceRegistrationRepository: DeviceRegistrationRepository
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KeyDeviceToken)
        if (token.isNullOrEmpty()) {
            return Result.failure(workDataOf(KeyDeviceTokenError to "Input data is missing"))
        }

        Timber.tag(LogTag).d("Worker - Registering device token...")

        return deviceRegistrationRepository.registerDeviceToken(token).fold(
            ifLeft = { error ->
                Timber.tag(LogTag).d("Worker - Registration failed ($error), scheduling retry")
                Result.retry()
            },
            ifRight = {
                Timber.tag(LogTag).d("Worker - Registration succeeded")
                Result.success()
            }
        )
    }

    companion object {

        const val UniqueWorkerId = "RegisterDeviceTokenWorker"
        const val KeyDeviceToken = "deviceToken"
        const val KeyDeviceTokenError = "DeviceTokenDataError"
        private const val LogTag = "Register device token"

        fun params(token: String) = mapOf(KeyDeviceToken to token)
    }
}
