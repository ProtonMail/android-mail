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

package ch.protonmail.android.mailcommon.data.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import javax.inject.Inject

class Enqueuer @Inject constructor(private val workManager: WorkManager) {

    inline fun <reified T : ListenableWorker> enqueue(params: Map<String, Any>) {
        enqueue(T::class.java, params)
    }

    inline fun <reified T : ListenableWorker> enqueueUniqueWork(workerId: String, params: Map<String, Any>) {
        enqueueUniqueWork(workerId, T::class.java, params)
    }

    fun enqueue(worker: Class<out ListenableWorker>, params: Map<String, Any>) {
        workManager.enqueue(createRequest(worker, params))
    }

    fun enqueueUniqueWork(workerId: String, worker: Class<out ListenableWorker>, params: Map<String, Any>) {
        workManager.enqueueUniqueWork(workerId, ExistingWorkPolicy.KEEP, createRequest(worker, params))
    }

    private fun createRequest(worker: Class<out ListenableWorker>, params: Map<String, Any>): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = workDataOf(*params.map { Pair(it.key, it.value) }.toTypedArray())

        return OneTimeWorkRequest.Builder(worker)
            .setConstraints(constraints)
            .setInputData(data)
            .build()
    }
}
