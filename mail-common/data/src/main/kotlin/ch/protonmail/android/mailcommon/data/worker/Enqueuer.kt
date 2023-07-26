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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import javax.inject.Inject

class Enqueuer @Inject constructor(private val workManager: WorkManager) {

    inline fun <reified T : ListenableWorker> enqueue(params: Map<String, Any>) {
        enqueue(T::class.java, params)
    }

    inline fun <reified T : ListenableWorker> enqueueUniqueWork(
        workerId: String,
        params: Map<String, Any>,
        constraints: Constraints? = buildDefaultConstraints()
    ) {
        enqueueUniqueWork(workerId, T::class.java, params, constraints)
    }

    fun enqueue(worker: Class<out ListenableWorker>, params: Map<String, Any>) {
        workManager.enqueue(createRequest(worker, params, buildDefaultConstraints()))
    }

    fun enqueueUniqueWork(
        workerId: String,
        worker: Class<out ListenableWorker>,
        params: Map<String, Any>,
        constraints: Constraints?
    ) {
        workManager.enqueueUniqueWork(workerId, ExistingWorkPolicy.KEEP, createRequest(worker, params, constraints))
    }

    suspend fun removeUnStartedExistingWork(uniqueWorkId: String) {
        val workInfo = workManager.getWorkInfosForUniqueWork(uniqueWorkId).await()
        val isRunning = workInfo.any { it.state == WorkInfo.State.RUNNING }
        if (isRunning) {
            return
        }
        workManager.cancelUniqueWork(uniqueWorkId)
    }


    private fun createRequest(
        worker: Class<out ListenableWorker>,
        params: Map<String, Any>,
        constraints: Constraints?
    ): OneTimeWorkRequest {

        val data = workDataOf(*params.map { Pair(it.key, it.value) }.toTypedArray())

        return OneTimeWorkRequest.Builder(worker).run {
            setInputData(data)
            if (constraints != null) setConstraints(constraints)
            build()
        }
    }

    fun buildDefaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
