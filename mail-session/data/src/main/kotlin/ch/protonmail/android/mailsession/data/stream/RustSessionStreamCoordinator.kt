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

package ch.protonmail.android.mailsession.data.stream

import ch.protonmail.android.mailcommon.data.mapper.LocalDeviceInfoProvider
import ch.protonmail.android.mailsession.data.network.AndroidDnsResolver
import ch.protonmail.android.mailsession.domain.coroutines.RustStreamScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.android.core.humanverification.domain.ChallengeNotifierCallback
import uniffi.mail_uniffi.MailSessionBundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts the long-running loops that service the request streams handed back by
 * `createMailSession`.
 *
 * The streams must be driven for the whole lifetime of the session, so the drivers run on a
 * session-scoped [CoroutineScope]. The Rust side cancels the streams (their `nextAsync` returns an
 * error) when the underlying session context is dropped, which ends each loop cleanly.
 */
@Singleton
class RustSessionStreamCoordinator @Inject constructor(
    @RustStreamScope private val scope: CoroutineScope,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val challengeNotifier: ChallengeNotifierCallback,
    private val dnsResolver: AndroidDnsResolver
) {

    fun start(bundle: MailSessionBundle) {
        scope.launch {
            DeviceInfoRequestStreamDriver(bundle.deviceInfoStream, deviceInfoProvider).loop(this)
        }
        scope.launch {
            ChallengeRequestStreamDriver(bundle.challengeStream, challengeNotifier).loop(this)
        }
        bundle.resolverStream?.let { resolverStream ->
            scope.launch {
                ResolverRequestStreamDriver(resolverStream, dnsResolver).loop(this)
            }
        }
    }
}
