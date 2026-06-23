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
import uniffi.mail_uniffi.ResolverOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidDnsResolver @Inject constructor(private val networkManager: NetworkManager) {

    private val resolver: DnsResolver = DnsResolver.getInstance()

    private val dispatcherIoAsExecutor by lazy {
        Dispatchers.IO.asExecutor()
    }

    /**
     * Resolves [host] and reports the outcome back to Rust. Failures are returned as
     * [ResolverOutcome] variants (never thrown): the Rust side maps a [ResolverOutcome.NetworkError]
     * / [ResolverOutcome.OtherError] to a resolver error and falls back to muon's own resolver.
     */
    suspend fun resolve(host: String): ResolverOutcome {
        return suspendCancellableCoroutine { continuation ->
            Timber.tag("DnsResolution").d("required for host: $host")

            val network = runCatching { networkManager.activeNetwork }.getOrNull()
            when {
                network == null -> {
                    Timber.tag("DnsResolution").d("DNS resolution error: network is unavailable")
                    continuation.resume(ResolverOutcome.NetworkError("Network is unavailable!"))
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

                            continuation.resume(ResolverOutcome.Resolved(addresses))
                        }

                        override fun onError(error: DnsResolver.DnsException) {
                            Timber.tag("DnsResolution").d("DNS resolution for '$host' errored: $error")
                            continuation.resume(
                                ResolverOutcome.OtherError("DNS resolution for '$host' errored: $error")
                            )
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
                        Timber.tag("DnsResolution").d("DNS resolution for '$host' error from resolver: $it")
                        continuation.resume(
                            ResolverOutcome.OtherError("DNS resolution for '$host' error from resolver: $it")
                        )
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
