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

package ch.protonmail.android.mailmailbox.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmailbox.data.RustSenderAddressCoroutineScope
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import uniffi.mail_uniffi.AsyncLiveQueryCallback
import uniffi.mail_uniffi.MailUserSessionHasValidSenderAddressResult
import uniffi.mail_uniffi.MailUserSessionWatchAddressesResult
import uniffi.mail_uniffi.WatchHandle
import javax.inject.Inject

class RustSenderAddressDataSourceImpl @Inject constructor(
    @RustSenderAddressCoroutineScope private val coroutineScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : SenderAddressDataSource {

    override fun observeUserHasValidSenderAddress(
        mailUserSession: MailUserSessionWrapper
    ): Flow<Either<DataError, Boolean>> = callbackFlow {
        suspend fun getHasValidSenderAddress() =
            when (val result = mailUserSession.getRustUserSession().hasValidSenderAddress()) {
                is MailUserSessionHasValidSenderAddressResult.Ok -> result.v1.right()
                is MailUserSessionHasValidSenderAddressResult.Error -> result.v1.toDataError().left()
            }

        fun Either<DataError, Boolean>.emit() {
            if (this.isLeft()) {
                Timber.e("rust-sender-addresses: hasValidUserAddress returned an error $this")
            }
            trySend(this)
        }

        val callback = object : AsyncLiveQueryCallback {
            override suspend fun onUpdate() {
                getHasValidSenderAddress().emit()
            }
        }

        getHasValidSenderAddress().emit()

        var watcher: WatchHandle? = null
        when (val result = mailUserSession.getRustUserSession().watchAddresses(callback)) {
            is MailUserSessionWatchAddressesResult.Ok -> {
                watcher = result.v1
            }

            is MailUserSessionWatchAddressesResult.Error -> {
                close()
                Timber.e("rust-sender-addresses: failed to watch addresses! ${result.v1}")
            }
        }

        awaitClose {
            coroutineScope.launch {
                watcher?.disconnect()
                watcher = null
            }
        }
    }.flowOn(ioDispatcher)
}
