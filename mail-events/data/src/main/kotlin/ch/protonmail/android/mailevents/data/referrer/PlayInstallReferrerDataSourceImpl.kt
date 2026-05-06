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

package ch.protonmail.android.mailevents.data.referrer

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooGenericExceptionCaught")
class PlayInstallReferrerDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : InstallReferrerDataSource {

    override suspend fun getInstallReferrer(): Either<DataError, InstallReferrer> = withContext(ioDispatcher) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        try {
            val responseCode = withTimeoutOrNull(CONNECTION_TIMEOUT_MS.milliseconds) {
                awaitConnection(referrerClient)
            } ?: run {
                Timber.d("Install referrer connection timed out")
                return@withContext DataError.Local.Unknown.left()
            }

            when (responseCode) {
                InstallReferrerClient.InstallReferrerResponse.OK -> {
                    val response = referrerClient.installReferrer
                    InstallReferrer(
                        referrerUrl = response.installReferrer,
                        referrerClickTimestampMs = response.referrerClickTimestampSeconds * 1000,
                        installBeginTimestampMs = response.installBeginTimestampSeconds * 1000,
                        isGooglePlayInstant = response.googlePlayInstantParam
                    ).right()
                }

                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                    Timber.d("Install referrer not supported on this device")
                    DataError.Local.Unknown.left()
                }

                InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                    Timber.d("Install referrer service unavailable")
                    DataError.Local.Unknown.left()
                }

                else -> {
                    Timber.d("Unknown install referrer response code: $responseCode")
                    DataError.Local.Unknown.left()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get install referrer details")
            DataError.Local.Unknown.left()
        } finally {
            referrerClient.endConnection()
        }
    }

    private suspend fun awaitConnection(referrerClient: InstallReferrerClient): Int =
        suspendCancellableCoroutine { continuation ->
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (continuation.isActive) continuation.resume(responseCode)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Timber.d("Install referrer service disconnected")
                }
            })
        }

    private companion object {

        const val CONNECTION_TIMEOUT_MS = 10_000L
    }
}
