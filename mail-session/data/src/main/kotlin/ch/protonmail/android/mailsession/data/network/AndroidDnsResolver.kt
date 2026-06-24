/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.network

import java.net.Inet6Address
import java.net.InetAddress
import android.net.DnsResolver
import android.os.CancellationSignal
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import uniffi.mail_uniffi.IpAddr
import uniffi.mail_uniffi.Resolver
import uniffi.mail_uniffi.ResolverException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AndroidDnsResolver @Inject constructor(private val networkManager: NetworkManager) : Resolver {

    private val resolver: DnsResolver = DnsResolver.getInstance()

    private val dispatcherIoAsExecutor by lazy {
        Dispatchers.IO.asExecutor()
    }


    @Throws(ResolverException::class)
    override suspend fun resolve(host: String): List<IpAddr>? {
        return suspendCancellableCoroutine { continuation ->
            Timber.tag("DnsResolution").d("required for host: $host")

            val network = runCatching { networkManager.activeNetwork }.getOrNull()
            when {
                network == null -> {
                    val exception = ResolverException.Network("Network is unavailable! Throwing")
                    Timber.tag("DnsResolution").d("DNS resolution error: $exception")
                    continuation.resumeWithException(exception)
                }

                else -> {
                    Timber.tag("DnsResolution").d("Resolving via ${network.networkHandle}")

                    val cancelSignal = CancellationSignal()
                    continuation.invokeOnCancellation { cancelSignal.cancel() }

                    val callback = object : DnsResolver.Callback<List<InetAddress>> {

                        override fun onAnswer(answer: List<InetAddress>, rcode: Int) {
                            Timber.tag("DnsResolution").d("DNS host for '$host' resolved to $answer")

                            val addresses = answer.mapNotNull {
                                @Suppress("UNNECESSARY_SAFE_CALL") // InetAddress is a platform type
                                it?.toRustIpAddress(originalHost = host)
                            }

                            continuation.resume(addresses)
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            val exception =
                                ResolverException.Other("DNS resolution for '$host' errored: $error")
                            Timber.tag("DnsResolution").d("DNS resolution error: $exception ")
                            continuation.resumeWithException(exception)
                        }
                    }

                    runCatching {
                        resolver.query(
                            network,
                            host,
                            DnsResolver.FLAG_EMPTY,
                            dispatcherIoAsExecutor,
                            cancelSignal,
                            callback
                        )
                    }.getOrElse {
                        val exception = ResolverException.Other("DNS resolution for '$host' error from resolver: $it")
                        Timber.tag("DnsResolution").d("DNS resolution error: $exception")
                        continuation.resumeWithException(exception)
                    }
                }
            }
        }
    }

    private fun InetAddress?.toRustIpAddress(originalHost: String): IpAddr? {
        val address = this?.hostAddress

        if (address == null) {
            Timber.tag("DnsResolution").d("Null address on DNS resolution for '$originalHost'")
            return null
        }

        return when (this) {
            is Inet6Address -> IpAddr.V6(address)
            else -> IpAddr.V4(address)
        }
    }
}
