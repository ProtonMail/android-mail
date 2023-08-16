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

package ch.protonmail.android.mailnotifications.data.remote

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.coroutines.resume

internal class NotificationTokenRemoteDataSourceImpl @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val enqueuer: Enqueuer
) : NotificationTokenRemoteDataSource {

    override suspend fun fetchToken() = fetchFirebaseToken()

    override suspend fun bindTokenToUser(userId: UserId, token: String) {
        enqueuer.enqueue<RegisterDeviceWorker>(RegisterDeviceWorker.params(userId, token))
    }

    private suspend fun fetchFirebaseToken(): Either<DataError.Remote, String> {
        return suspendCancellableCoroutine { continuation ->
            firebaseMessaging.token.addOnCompleteListener {
                val token = if (it.isSuccessful) it.result.right() else DataError.Remote.Unknown.left()
                continuation.resume(token)
            }
        }
    }
}