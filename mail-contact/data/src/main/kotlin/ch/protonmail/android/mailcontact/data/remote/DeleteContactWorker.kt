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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.repository.ContactRemoteDataSource
import me.proton.core.domain.entity.UserId

@HiltWorker
class DeleteContactWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val remoteDataSource: ContactRemoteDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(RawUserIdKey)
        val contactId = inputData.getString(RawContactIdKey)

        if (userId == null || contactId == null) {
            return Result.failure()
        }

        return kotlin.runCatching {
            remoteDataSource.deleteContacts(UserId(userId), listOf(ContactId(contactId)))
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() }
        )
    }

    companion object {

        internal const val RawUserIdKey = "userId"
        internal const val RawContactIdKey = "contactId"

        fun params(userId: String, contactId: String) = mapOf(
            RawUserIdKey to userId,
            RawContactIdKey to contactId
        )

    }

}
