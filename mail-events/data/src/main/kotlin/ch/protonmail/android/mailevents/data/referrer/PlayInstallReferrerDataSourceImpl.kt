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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailevents.domain.model.InstallReferrer
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

@Suppress("TooGenericExceptionCaught")
class PlayInstallReferrerDataSourceImpl @Inject constructor(
    private val context: Context
) : InstallReferrerDataSource {

    override suspend fun getInstallReferrer(): Either<DataError, InstallReferrer> =
        suspendCancellableCoroutine { continuation ->
            val referrerClient = InstallReferrerClient.newBuilder(context).build()

            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                val response = referrerClient.installReferrer
                                val referrer = InstallReferrer(
                                    referrerUrl = response.installReferrer,
                                    referrerClickTimestampMs = response.referrerClickTimestampSeconds * 1000,
                                    installBeginTimestampMs = response.installBeginTimestampSeconds * 1000,
                                    isGooglePlayInstant = response.googlePlayInstantParam
                                )
                                continuation.resume(referrer.right())
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to get install referrer details")
                                continuation.resume(DataError.Local.Unknown.left())
                            } finally {
                                referrerClient.endConnection()
                            }
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            Timber.w("Install referrer not supported on this device")
                            continuation.resume(DataError.Local.Unknown.left())
                            referrerClient.endConnection()
                        }

                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            Timber.w("Install referrer service unavailable")
                            continuation.resume(DataError.Local.Unknown.left())
                            referrerClient.endConnection()
                        }

                        else -> {
                            Timber.w("Unknown install referrer response code: $responseCode")
                            continuation.resume(DataError.Local.Unknown.left())
                            referrerClient.endConnection()
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Timber.d("Install referrer service disconnected")
                }
            })

            continuation.invokeOnCancellation {
                referrerClient.endConnection()
            }
        }
}
